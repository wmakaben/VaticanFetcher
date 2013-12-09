/*
 * RVDF class
 */

package net.sourceforge.docfetcher.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.docfetcher.Main;
import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.enums.SettingsConf;
import net.sourceforge.docfetcher.enums.SystemConf;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.gui.indexing.IndexingPanel;
import net.sourceforge.docfetcher.gui.pref.PrefDialog;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;
import net.sourceforge.docfetcher.model.Cancelable;
import net.sourceforge.docfetcher.model.Daemon;
import net.sourceforge.docfetcher.model.FolderWatcher;
import net.sourceforge.docfetcher.model.IndexLoadingProblems;
import net.sourceforge.docfetcher.model.IndexLoadingProblems.CorruptedIndex;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.IndexingQueue;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.search.ResultDocument;
import net.sourceforge.docfetcher.util.AppUtil;
import net.sourceforge.docfetcher.util.CharsetDetectorHelper;
import net.sourceforge.docfetcher.util.ConfLoader;
import net.sourceforge.docfetcher.util.ConfLoader.Loadable;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.collect.AlphanumComparator;
import net.sourceforge.docfetcher.util.gui.CocoaUIEnhancer;
import net.sourceforge.docfetcher.util.gui.LazyImageCache;
import net.sourceforge.docfetcher.util.gui.dialog.InfoDialog;
import net.sourceforge.docfetcher.util.gui.dialog.ListConfirmDialog;
import net.sourceforge.docfetcher.util.gui.dialog.MultipleChoiceDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.Resources;

public class Application {
	
	private static SearchBar searchBar;
	private static StatusBar statusBar;
	private static PanelForm panelForm;
	private static ResultPanel resultPanel;
	private static TabPanel tabPanel;
	private static PreviewPanel previewPanel;
	private static FilterPanel filterPanel;
	private static IndexingPanel indexingPanel;
	private static IndexPanel indexPanel;
	
	private static volatile Shell shell;
	
	private static File programConfFile;
	private static File settingsConfFile;
	
	private static volatile IndexRegistry indexRegistry;
	private static volatile FolderWatcher folderWatcher;
	private static SystemTrayHider systemTrayHider;
	private static boolean systemTrayShutdown = false;
	@Nullable private static HotkeyHandler hotkeyHandler;
	
	private Application() {
		throw new UnsupportedOperationException();
	}
	
	public static void main(String args[]){
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		// Load system configuration files and set up system values
		String systemConfName = "system-conf.txt";
		String systemConfPath = "dev/system-conf.txt";
		boolean success = ConfLoader.loadFromStreamOrFile(Main.class, SystemConf.class, systemConfName, systemConfPath);
		if (!success) {
			Util.printErr("Couldn't find resource: " + systemConfName);
			System.exit(1);
		}
		
		AppUtil.Const.PROGRAM_NAME.set(SystemConf.Str.ProgramName.get());
		AppUtil.Const.PROGRAM_VERSION.set(SystemConf.Str.ProgramVersion.get());
		AppUtil.Const.PROGRAM_BUILD_DATE.set(SystemConf.Str.BuildDate.get());
		AppUtil.Const.USER_DIR_PATH.set(Util.USER_DIR_PATH);
		AppUtil.Const.IS_PORTABLE.set(SystemConf.Bool.IsPortable.get());
		AppUtil.Const.IS_DEVELOPMENT_VERSION.set(SystemConf.Bool.IsDevelopmentVersion.get());

		Msg.loadFromDisk();
		Msg.setCheckEnabled(false);
		AppUtil.Messages.system_error.set(Msg.system_error.get());
		AppUtil.Messages.confirm_operation.set(Msg.confirm_operation.get());
		AppUtil.Messages.invalid_operation.set(Msg.invalid_operation.get());
		AppUtil.Messages.program_died_stacktrace_written.set(Msg.report_bug.get());
		AppUtil.Messages.program_running_launch_another.set(Msg.program_running_launch_another.get());
		AppUtil.Messages.ok.set(Msg.ok.get());
		AppUtil.Messages.cancel.set(Msg.cancel.get());
		Msg.setCheckEnabled(true);
		AppUtil.Messages.checkInitialized();
		
		// Path overrides
		File confPathOverride = null;
		File swtLibDir = null;
		try {
			Properties pathProps = CharsetDetectorHelper.load(new File("misc", "paths.txt"));
			confPathOverride = toFile(pathProps, "settings");
			IndexRegistry.indexPathOverride = toFile(pathProps, "indexes");
			swtLibDir = toFile(pathProps, "swt");
		}
		catch (IOException e1) { /* Ignore */ }
		
		// Set the path from which to load the native SWT libraries. 
		// Bug #399
		String swtLibSuffix;
		if (Util.IS_WINDOWS)
			swtLibSuffix = "windows-";
		else if (Util.IS_LINUX)
			swtLibSuffix = "linux-";
		else if (Util.IS_MAC_OS_X)
			swtLibSuffix = "macosx-";
		else
			swtLibSuffix = "unknown-";
		if (Util.IS_64_BIT_JVM)
			swtLibSuffix += "64";
		else
			swtLibSuffix += "32";
		if (swtLibDir == null)
			swtLibDir = new File(AppUtil.getAppDataDir(), (AppUtil.isPortable() ? "lib/swt/" : "swt/") + swtLibSuffix);
		else
			swtLibDir = new File(swtLibDir, swtLibSuffix);
		swtLibDir.mkdirs(); // SWT won't recognize the path if it doesn't exist
		System.setProperty("swt.library.path", Util.getAbsPath(swtLibDir));

		// Load program configurations and settings/preferences
		programConfFile = loadProgramConf(confPathOverride);
		settingsConfFile = loadSettingsConf(confPathOverride);
		
		// Update indexes in headless mode
		if (args.length >= 1 && args[0].equals("--update-indexes")) {
			loadIndexRegistryHeadless(getIndexParentDir(IndexRegistry.indexPathOverride));
			return;
		}
		
		// Check single instance
		if (!AppUtil.checkSingleInstance())
			return;
				
		checkMultipleDocFetcherJars();		
		
		// Determine shell title
		String shellTitle;
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			shellTitle = SystemConf.Str.ProgramName.get();
		else
			shellTitle = ProgramConf.Str.AppName.get();
		
		// Load index registry; create display and shell
		Display.setAppName(shellTitle); // must be called *before* the display is created
		Display display = new Display();
		AppUtil.setDisplay(display);
		shell = new Shell(display);
		loadIndexRegistry(shell, getIndexParentDir(IndexRegistry.indexPathOverride));
		
		// Load images
		LazyImageCache lazyImageCache = new LazyImageCache(display, AppUtil.getImageDir());
		Img.initialize(lazyImageCache);
		lazyImageCache.reportMissingFiles(shell, Img.class, Msg.missing_image_files.get());
		shell.setImage(Img.RV_ICON.get());
		
		// Set default uncaught exception handler
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, final Throwable e) {
				handleCrash(e);
			}
		});	
		
		SettingsConf.ShellBounds.MainWindow.bind(shell);
		SettingsConf.Bool.MainShellMaximized.bindMaximized(shell);
		shell.setText(shellTitle);
		shell.setLayout(new FormLayout());
		
		// init methods for all panels/components
		initCocoaMenu(display);
		initSystemTrayHider();
		createPanelForm(shell);
		createSearchBar(shell);
		createStatusBar(shell);
		initHotkey();
		initGlobalKeys(display);
		
		// Set up the SearchQueue
		new SearchQueue(searchBar, indexPanel, resultPanel, statusBar);
		
		// Creates the GUI layout
		FormData fd = new FormData();
		fd.left = new FormAttachment(0,0);
		fd.right = new FormAttachment(100, 0);
		searchBar.getControl().setLayoutData(fd);
		searchBar.setFocus();
		
		fd = new FormData();
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		fd.bottom = new FormAttachment(100, 0);
		statusBar.setLayoutData(fd);	
		
		fd = new FormData();
		fd.top = new FormAttachment(searchBar.getControl(), 5);
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		fd.bottom = new FormAttachment(statusBar, -5);
		panelForm.setLayoutData(fd);
		
		// Try to show the manual in the embedded browser
		if (SettingsConf.Bool.ShowManualOnStartup.get() && SettingsConf.Bool.ShowPreviewPanel.get()) {
			File file = ManualLocator.getManualFile();
			if (file == null) {
				String msg = Msg.file_not_found.get() + "\n" + ManualLocator.manualFilename;
				AppUtil.showError(msg, true, true);
			}
		}
		showManual();
		
		shell.addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				handleShellClosed(e);
			}
		});
		
		shell.open();
	    while (!shell.isDisposed()) {
	    	try {
				if (!display.readAndDispatch())
					display.sleep();
			}
			catch (Throwable t) {
				handleCrash(t);
			}
	    }
	    display.dispose();
	    saveSettingsConfFile();
	}
	
	// Creates the search bar and the methods/events
	private static void createSearchBar(Composite parent){
		searchBar = new SearchBar(parent, programConfFile);
		// Saves the preferences when the OK button is clicked on the preferences dialog
		searchBar.evtOKClicked.add(new Event.Listener<Void> () {
		    public void update(Void eventData) {
		    	saveSettingsConfFile();
		    }
		});
		// Hides the search bar when it is minimized to the system tray
		searchBar.evtHideInSystemTray.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				systemTrayHider.hide();
			}
		});
		// Switches the orientation of the 2 middle panels to either horizontal or vertical
		searchBar.evtChangeView.add(new Event.Listener<Void>() {
			public void update(Void eventData){
				panelForm.switchOrientation();
			}
		});
		// Opens the manual in the preview panel when the help button is pressed
		searchBar.evtOpenManual.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				showManual();
			}
		});
	}
	
	// Creates the status bar and the method/events
	private static void createStatusBar(Composite parent){
		statusBar = new StatusBar(parent);
		// When the indexing status is clicked, it shows the progress on the index panel tab
		statusBar.evtIndexLabelClicked.add(new Event.Listener<Void>(){
			public void update(Void eventData) {
				if(statusBar.isIndexLabelVisible())
					tabPanel.selectIndex();
				// If the queue is empty, the indexing message disappears
				if(statusBar.isDoneIndexing())
					statusBar.setIndexLabelVisible(false);
			}
		});
	}
	
	// Creates the result panel and tab panel for the panel form
	private static void createPanelForm(Composite parent){
		panelForm = new PanelForm(parent){
			protected ResultPanel createResultPanel(Composite parent) {
				resultPanel = new ResultPanel(parent);
				// Shows a preview of the file selected in the results panel
				resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
					public void update(List<ResultDocument> eventData) {
						if (!eventData.isEmpty()){
							previewPanel.setPreview(eventData.get(0));
							tabPanel.selectPreview();
						}
					}
				});
				// Hides the result panel when minimized to system tray
				resultPanel.evtHideInSystemTray.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						systemTrayHider.hide();
					}
				});
				return resultPanel;
			}
			// Creates the tab panel
			protected TabPanel createTabPanel(Composite parent) {
				tabPanel = createTabs(parent);
				return tabPanel;
			}
		};
	}
	
	// Creates the preview, filter, and index panel for the tabs
	private static TabPanel createTabs(Composite parent){
		tabPanel = new TabPanel(parent){
			// Creates the preview panel
			protected PreviewPanel createPreviewPanel(Composite parent) {
				previewPanel = new PreviewPanel(parent);
				previewPanel.evtHideInSystemTray.add(new Event.Listener<Void>() {
					public void update(Void eventData) {
						systemTrayHider.hide();
					}
				});
				return previewPanel;
			}
			// Creates the filter panel
			protected FilterPanel createFilterPanel(Composite parent) {
				filterPanel = new FilterPanel(parent){
					protected IndexPanel createIndexPanel(Composite parent) {
						indexPanel = new IndexPanel(parent, indexRegistry);
						IndexPanel.evtSwitchToIndexingPanel.add(new Event.Listener<Void>() {
							public void update(Void eventData){
								tabPanel.selectIndex();
							}
						});
						return indexPanel;
					}
				};
				return filterPanel;
			}
			// Creates the index status panel
			protected IndexingPanel createIndexPanel(Composite parent) {
				indexingPanel = new IndexingPanel(parent, indexRegistry);
				return indexingPanel;
			}
		};
		return tabPanel;
	}
	
	@Nullable
	private static File toFile(@NotNull Properties props, @NotNull String key) {
		String value = props.getProperty(key);
		if (value == null)
			return null;
		Pattern homePattern = Pattern.compile("\\$\\{user\\.home}(?:[\\\\/](.*))?");
		Matcher m = homePattern.matcher(value);
		if (m.matches()) {
			if (m.group(1) == null || m.group(1).trim().isEmpty())
				return new File(Util.USER_HOME_PATH);
			return new File(Util.USER_HOME_PATH, m.group(1));
		}
		return Util.getCanonicalFile(value);
	}
	
	private static File loadProgramConf(@Nullable File pathOverride) {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();

		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("dist/program-conf.txt");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			confFile = new File(pathOverride, "program-conf.txt");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/program-conf.txt");
		}

		try {
			List<Loadable> notLoaded = ConfLoader.load(confFile, ProgramConf.class, false);
			if (!notLoaded.isEmpty()) {
				List<String> entryNames = new ArrayList<String>(
					notLoaded.size());
				for (Loadable entry : notLoaded)
					entryNames.add("  " + entry.name());
				String msg = Msg.entries_missing.format(confFile.getName());
				msg += "\n" + Joiner.on("\n").join(entryNames);
				msg += "\n\n" + Msg.entries_missing_regenerate.get();
				int style = SWT.YES | SWT.NO | SWT.ICON_WARNING;
				if (AppUtil.showErrorOnStart(msg, style) == SWT.YES) {
					regenerateConfFile(confFile);
				}
			}
		}
		catch (FileNotFoundException e) {
			regenerateConfFile(confFile);
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
		return confFile;
	}
	
	private static void regenerateConfFile(File confFile) {
		// Restore conf file if missing. In case of non-portable version, conf file will be missing the first time the program is started.
		InputStream in = Main.class.getResourceAsStream(confFile.getName());
		try {
			ConfLoader.load(in, ProgramConf.class);
			URL url = Resources.getResource(Main.class, confFile.getName());
			Util.getParentFile(confFile).mkdirs();
			Files.copy(
				Resources.newInputStreamSupplier(url), confFile);
		}
		catch (Exception e1) {
			AppUtil.showStackTraceInOwnDisplay(e1);
		}
		finally {
			Closeables.closeQuietly(in);
		}
	}
		
	private static File loadSettingsConf(@Nullable File pathOverride) {
		AppUtil.checkConstInitialized();
		AppUtil.ensureNoDisplay();

		File confFile;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			confFile = new File("bin/settings-conf.txt");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			confFile = new File(pathOverride, "settings-conf.txt");
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			confFile = new File(appDataDir, "conf/settings-conf.txt");
		}

		try {
			ConfLoader.load(confFile, SettingsConf.class, true);
		}
		catch (IOException e) {
			AppUtil.showStackTraceInOwnDisplay(e);
		}
		return confFile;
	}
	
	private static void saveSettingsConfFile() {
		// Try to save the settings. This may not be possible, for example when the user has burned the program onto a CD-ROM.
		if (settingsConfFile.canWrite()) {
			try {
				String comment = SettingsConf.loadHeaderComment();
				ConfLoader.save(settingsConfFile, SettingsConf.class, comment);
			}
			catch (IOException e) {
				AppUtil.showStackTraceInOwnDisplay(e);
			}
		}
	}
		
	@NotNull
	private static File getIndexParentDir(@Nullable File pathOverride) {
		File indexParentDir;
		if (SystemConf.Bool.IsDevelopmentVersion.get()) {
			indexParentDir = new File("bin/indexes");
		}
		else if (pathOverride != null && !pathOverride.isFile()) {
			pathOverride.mkdirs();
			indexParentDir = pathOverride;
		}
		else {
			File appDataDir = AppUtil.getAppDataDir();
			if (SystemConf.Bool.IsPortable.get())
				indexParentDir = new File(appDataDir, "indexes");
			else
				indexParentDir = appDataDir;
		}
		indexParentDir.mkdirs();
		return indexParentDir;
	}
	
	private static void loadIndexRegistryHeadless(@NotNull File indexParentDir) {
		int cacheCapacity = ProgramConf.Int.UnpackCacheCapacity.get();
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(indexParentDir, cacheCapacity, reporterCapacity);
		
		try {
			indexRegistry.load(Cancelable.nullCancelable);
			final IndexingQueue queue = indexRegistry.getQueue();
			
			// TODO - evtQueueEmpty
			queue.evtQueueEmpty.add(new Event.Listener<Void>() {
				public void update(Void eventData) {
					indexRegistry.getSearcher().shutdown();
					queue.shutdown(new CancelHandler() {
						public CancelAction cancel() {
							return CancelAction.KEEP;
						}
					});
				}
			});
			
			for (LuceneIndex index : indexRegistry.getIndexes())
				queue.addTask(index, IndexAction.UPDATE);
		}
		catch (IOException e) {
			Util.printErr(e);
		}
	}
	
	/** Checks for multiple loaded DocFetcher jars. This should be called before creating the display. */
	private static void checkMultipleDocFetcherJars() {
		if (SystemConf.Bool.IsDevelopmentVersion.get())
			return;
		if (!(AppUtil.isPortable() || Util.IS_WINDOWS))
			return;
		Pattern p = Pattern.compile("net\\.sourceforge\\.docfetcher.*\\.jar");
		List<File> dfJars = new LinkedList<File>();
		for (File jarFile : Util.listFiles(new File("lib")))
			if (p.matcher(jarFile.getName()).matches())
				dfJars.add(jarFile);
		if (dfJars.size() == 1)
			return;
		assert !dfJars.isEmpty();
		String msg = Msg.multiple_docfetcher_jars.format(Util.join("\n", dfJars));
		AppUtil.showErrorOnStart(msg, false);
	}
	
	private static void loadIndexRegistry(@NotNull final Shell mainShell, @NotNull File indexParentDir) {
		Util.assertSwtThread();
		final Display display = mainShell.getDisplay();

		int cacheCapacity = ProgramConf.Int.UnpackCacheCapacity.get();
		int reporterCapacity = ProgramConf.Int.MaxLinesInProgressPanel.get();
		indexRegistry = new IndexRegistry(indexParentDir, cacheCapacity, reporterCapacity);
		final Daemon daemon = new Daemon(indexRegistry);
		IndexingQueue queue = indexRegistry.getQueue();

		queue.evtWorkerThreadTerminated.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				daemon.writeIndexesToFile();
			}
		});
		
		// TODO - evtQueueEmpty
		// Event for when the task queue is empty. Shows a message on the status bar that the indexing is complete
		queue.evtQueueEmpty.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				/*
				Util.runAsyncExec(statusBar.getIndexLabel(), new Runnable() {
					public void run() {
						statusBar.setIndexStatus(true);
						statusBar.setIndexLabelVisible(true);
						statusBar.setIndexLabelText("Done Indexing");
					}
				});
				*/
			}
		});
	
		new Thread(Application.class.getName() + " (load index registry)") {
			public void run() {
				try {
					final IndexLoadingProblems loadingProblems = indexRegistry.load(new Cancelable() {
						public boolean isCanceled() {
							return display.isDisposed();
						}
					});
					
					// Program may have been shut down while it was loading the indexes
					if (display.isDisposed())
						return;

					/*
					 * Install folder watches on the user's document folders.
					 * Should be done *after* the index registry is loaded:
					 * The index registry will try to install its own folder watch during loading, and if we set up this folder watcher 
					 * before loading the registry, we might take up all the allowed watches, so that there's none left for the registry.
					 */
					folderWatcher = new FolderWatcher(indexRegistry);

					// Show error message when watch limit is reached
					folderWatcher.evtWatchLimitError.add(new Event.Listener<String>() {
						public void update(final String eventData) {
							Util.runAsyncExec(mainShell, new Runnable() {
								public void run() {
									InfoDialog dialog = new InfoDialog(mainShell);
									dialog.setTitle(Msg.system_error.get());
									dialog.setImage(SWT.ICON_ERROR);
									dialog.setText(eventData);
									dialog.open();
								}
							});
						}
					});

					// Must be called *after* the indexes have been loaded
					daemon.enqueueUpdateTasks();
					
					// Confirm deletion of obsolete files inside the index folder
					if (ProgramConf.Bool.ReportObsoleteIndexFiles.get()	&& !loadingProblems.getObsoleteFiles().isEmpty()) {
						Util.runSyncExec(mainShell, new Runnable() {
							public void run() {
								reportObsoleteIndexFiles(mainShell,	indexRegistry.getIndexParentDir(), loadingProblems.getObsoleteFiles());
							}
						});
					}

					// Show error messages if some indexes couldn't be loaded
					if (!loadingProblems.getCorruptedIndexes().isEmpty()) {
						StringBuilder msg = new StringBuilder(Msg.corrupted_indexes.get());
						for (CorruptedIndex index : loadingProblems.getCorruptedIndexes()) {
							msg.append("\n\n");
							String indexName = index.index.getRootFolder().getDisplayName();
							String errorMsg = index.ioException.getMessage();
							msg.append(Msg.index.format(indexName));
							msg.append("\n");
							msg.append(Msg.error.format(errorMsg));
						}
						AppUtil.showError(msg.toString(), true, false);
					}
				}
				catch (IOException e) {
					if (display.isDisposed())
						AppUtil.showStackTraceInOwnDisplay(e);
					else
						AppUtil.showStackTrace(e);
				}
			}
		}.start();
	}
	
	private static void reportObsoleteIndexFiles(@NotNull Shell mainShell, @NotNull File indexDir, @NotNull List<File> filesToDelete) {
		ListConfirmDialog dialog = new ListConfirmDialog(mainShell, SWT.ICON_INFORMATION);
		dialog.setTitle(Msg.confirm_operation.get());
		dialog.setText(Msg.delete_obsolete_index_files.format(indexDir.getPath()));
		dialog.setButtonLabels(Msg.delete_bt.get(), Msg.keep.get());
		
		filesToDelete = new ArrayList<File>(filesToDelete);
		Collections.sort(filesToDelete, new Comparator<File>() {
			public int compare(File o1, File o2) {
				boolean d1 = o1.isDirectory();
				boolean d2 = o2.isDirectory();
				if (d1 && !d2)
					return -1;
				if (!d1 && d2)
					return 1;
				return AlphanumComparator.ignoreCaseInstance.compare(o1.getName(), o2.getName());
			}
		});
		
		for (File file : filesToDelete) {
			Image img = (file.isDirectory() ? Img.FOLDER : Img.FILE).get();
			dialog.addItem(img, file.getName());
		}
		
		dialog.evtLinkClicked.add(new Event.Listener<String>() {
			public void update(String eventData) {
				Util.launch(eventData);
			}
		});
		
		if (dialog.open()) {
			for (File file : filesToDelete) {
				try {
					Util.deleteRecursively(file);
				}
				catch (IOException e) {
					Util.printErr(e);
				}
			}
		}
	}
	
	private static void handleCrash(@NotNull Throwable t) {
		for (OutOfMemoryError e : Iterables.filter(Throwables.getCausalChain(t), OutOfMemoryError.class)) {
			UtilGui.showOutOfMemoryMessage(shell, e);
			return;
		}
		AppUtil.showStackTrace(t);
	}
	
	private static void initCocoaMenu(@NotNull Display display) {
		if (!Util.IS_MAC_OS_X)
			return;

		CocoaUIEnhancer cocoaUIEnhancer = new CocoaUIEnhancer(ProgramConf.Str.AppName.get());
		cocoaUIEnhancer.hookApplicationMenu(display, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event event) {
				// shell.close(); // Not necessary
			}
		}, new Runnable() {
			public void run() {
				String name = SystemConf.Str.ProgramName.get();
				String version = SystemConf.Str.ProgramVersion.get();
				AppUtil.showInfo(name + " " + version);
			}
		}, new Runnable() {
			public void run() {
				PrefDialog prefDialog = new PrefDialog(shell, programConfFile);
				prefDialog.evtOKClicked.add(new Event.Listener<Void> () {
				    public void update(Void eventData) {
				    	saveSettingsConfFile();
				    }
				});
				prefDialog.open();
			}
		});
	}
	
	/* Sets up system tray hiding. */
	private static void initSystemTrayHider() {
		systemTrayHider = new SystemTrayHider(shell);
		final ResultDocument[] lastDoc = new ResultDocument[1];

		systemTrayHider.evtHiding.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				lastDoc[0] = previewPanel.clear();
			}
		});

		systemTrayHider.evtRestored.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				if (lastDoc[0] != null) {
					previewPanel.setPreview(lastDoc[0]);
					lastDoc[0] = null;
				}
				searchBar.setFocus();
			}
		});

		systemTrayHider.evtShutdown.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				systemTrayShutdown = true;
				shell.close();
			}
		});
	}
		
	private static void initHotkey() {
		try {
			hotkeyHandler = new HotkeyHandler();
		}
		catch (UnsupportedOperationException e) {
			return;
		}
		catch (Throwable e) {
			Util.printErr(e);
			return;
		}

		hotkeyHandler.evtHotkeyPressed.add(new Event.Listener<Void> () {
			public void update(Void eventData) {
				Util.runSyncExec(shell, new Runnable() {
					public void run() {
						if (systemTrayHider.isHidden()) {
							systemTrayHider.restore();
						}
						else {
							shell.setMinimized(false);
							shell.setVisible(true);
							shell.forceActive();
							searchBar.setFocus();
						}
					}
				});
			}
		});
		hotkeyHandler.evtHotkeyConflict.add(new Event.Listener<int[]> () {
			public void update(int[] eventData) {
				String key = UtilGui.toString(eventData);
				AppUtil.showError(Msg.hotkey_in_use.format(key), false, true);

				// Don't open preferences dialog when the hotkey conflict occurs at startup.
				if (shell.isVisible()) {
					PrefDialog prefDialog = new PrefDialog(shell, programConfFile);
					prefDialog.evtOKClicked.add(new Event.Listener<Void> () {
					    public void update(Void eventData) {
					    	saveSettingsConfFile();
					    }
					});
					prefDialog.open();
				}
			}
		});
		hotkeyHandler.registerHotkey();
	}
	
	private static void initGlobalKeys(@NotNull Display display) {
		 // This filter must be added to SWT.KeyDown, otherwise we won't be able to prevent the events from propagating further.
		display.addFilter(SWT.KeyDown, new Listener() {
			public void handleEvent(org.eclipse.swt.widgets.Event e) {
				// Disable global keys when the main shell is inactive
				if (Display.getCurrent().getActiveShell() != shell)
					return;

				e.doit = false;
				int m = e.stateMask;
				int k = e.keyCode;

				if (k == SWT.F1)
					showManual();
				else if ((m & (SWT.ALT | SWT.CTRL | SWT.COMMAND)) != 0 && k == 'f') {
					searchBar.setFocus();
				}
				else {
					e.doit = true;
				}
			}
		});
	}
	
	private static void showManual() {
		File file = ManualLocator.getManualFile();
		if (file != null) {
			if (previewPanel.setHtmlFile(file))
				panelForm.getTabPanel().selectPreview();
			else
				Util.launch(file);
		}
		else {
			String msg = Msg.file_not_found.get() + "\n" + ManualLocator.manualFilename;
			AppUtil.showError(msg, true, true);
		}
	}
	
	private static void handleShellClosed(@NotNull ShellEvent e) {
		if (SettingsConf.Bool.CloseToTray.get() && !systemTrayShutdown && !Util.IS_UBUNTU_UNITY) {
			e.doit = false;
			systemTrayHider.hide();
		} else {
			e.doit = indexRegistry.getQueue().shutdown(new CancelHandler() {
				public CancelAction cancel() {
					return confirmExit();
				}
			});
			if (!e.doit)
				return;

			// Clear search history
			if (SettingsConf.Bool.ClearSearchHistoryOnExit.get())
				SettingsConf.StrList.SearchHistory.set();

			/*
			 * Note: The getSearcher() call below will block until the searcher is available. 
			 * If we run this inside the GUI thread, we won't let go of the GUI lock, 
			 * causing the program to deadlock when the user tries to close the program before all indexes have been loaded.
			 */
			new Thread() {
				public void run() {
					// The folder watcher will be null if the program is shut down while loading the indexes					
					if (folderWatcher != null)
						folderWatcher.shutdown();

					if (hotkeyHandler != null)
						hotkeyHandler.shutdown();

					indexRegistry.getSearcher().shutdown();
				}
			}.start();
		}
	}
	
	@Nullable
	private static CancelAction confirmExit() {
		MultipleChoiceDialog<CancelAction> dialog = new MultipleChoiceDialog<CancelAction>(shell);
		dialog.setTitle(Msg.abort_indexing.get());
		dialog.setText(Msg.keep_partial_index_on_exit.get());
		dialog.addButton(Msg.keep.get(), CancelAction.KEEP);
		dialog.addButton(Msg.discard.get(), CancelAction.DISCARD);
		dialog.addButton(Msg.dont_exit.get(), null);
		return dialog.open();
	}
}
