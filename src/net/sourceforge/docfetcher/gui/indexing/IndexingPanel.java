package net.sourceforge.docfetcher.gui.indexing;

import java.io.File;
import java.util.List;

import net.sourceforge.docfetcher.enums.Img;
import net.sourceforge.docfetcher.enums.Msg;
import net.sourceforge.docfetcher.enums.ProgramConf;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.model.IndexRegistry;
import net.sourceforge.docfetcher.model.LuceneIndex;
import net.sourceforge.docfetcher.model.index.IndexingConfig;
import net.sourceforge.docfetcher.model.index.IndexingError;
import net.sourceforge.docfetcher.model.index.IndexingInfo;
import net.sourceforge.docfetcher.model.index.Task;
import net.sourceforge.docfetcher.model.index.DelegatingReporter.ExistingMessagesHandler;
import net.sourceforge.docfetcher.model.index.IndexingQueue.ExistingTasksHandler;
import net.sourceforge.docfetcher.model.index.Task.CancelAction;
import net.sourceforge.docfetcher.model.index.Task.CancelHandler;
import net.sourceforge.docfetcher.model.index.Task.IndexAction;
import net.sourceforge.docfetcher.model.index.Task.TaskState;
import net.sourceforge.docfetcher.model.index.file.FileIndex;
import net.sourceforge.docfetcher.model.index.outlook.OutlookIndex;
import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.annotations.NotNull;
import net.sourceforge.docfetcher.util.annotations.Nullable;
import net.sourceforge.docfetcher.util.gui.DropDownMenuManager;
import net.sourceforge.docfetcher.util.gui.MenuAction;
import net.sourceforge.docfetcher.util.gui.TabFolderFactory;
import net.sourceforge.docfetcher.util.gui.ToolItemFactory;
import net.sourceforge.docfetcher.util.gui.dialog.MultipleChoiceDialog;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;

public class IndexingPanel extends Composite{
	
	private final CTabFolder tabFolder;
	private final IndexRegistry indexRegistry;
	
	private boolean childDialogOpen = false;
	
	@NotNull private Event.Listener<Task> addedListener;
	@NotNull private Event.Listener<Task> removedListener;
	
	public IndexingPanel (@NotNull final Composite parent, @NotNull final IndexRegistry indexRegistry){
		super(parent, SWT.NONE);
		Util.checkNotNull(indexRegistry, parent);
		this.indexRegistry = indexRegistry;
		
		this.setLayout(Util.createFillLayout(5));
		
		// Create tabfolder
		boolean curvyTabs = ProgramConf.Bool.CurvyTabs.get();
		boolean coloredTabs = ProgramConf.Bool.ColoredTabs.get();
		tabFolder = TabFolderFactory.create(this, true, curvyTabs, coloredTabs);
		
		// Create tabfolder toolbar
		ToolBar toolBar = new ToolBar(tabFolder, SWT.FLAT);
		tabFolder.setTopRight(toolBar);
		initToolBarMenu(toolBar);
		
		// For some unknown reason, the focus always goes to the ToolBar items
		toolBar.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				tabFolder.forceFocus();
			}
		});
		
		initEventHandlers();
	}
	
	@NotNull
	private void initToolBarMenu(@NotNull ToolBar toolBar) {
		ToolItemFactory tif = new ToolItemFactory(toolBar);
		
		final ToolItem addItem = tif.image(Img.ADD.get()).toolTip(Msg.add_to_queue.get()).create();
		
		addItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				abstract class ChildDialogAction extends MenuAction {
					public ChildDialogAction(Image image, String label) {
						super(image, label);
					}
					public final void run() {
						// Only one child dialog can be open at any time
						assert !childDialogOpen;
						
						childDialogOpen = true;
						boolean success = doRun();
						childDialogOpen = false;
						
						// TODO Check if this is needed |||||||||||||||||||||||
						if (!success && tabFolder.getItemCount() == 0)
							indexRegistry.getQueue().removeListeners(addedListener, removedListener);
					}
					protected abstract boolean doRun();
				}
				
				DropDownMenuManager menuManager = new DropDownMenuManager(addItem, tabFolder);
				
				menuManager.add(new ChildDialogAction(Img.FOLDER.get(), Msg.add_folder.get()) {
					public boolean doRun() {
						return IndexPanel.createFileTaskFromDialog(IndexingPanel.this.getShell(), indexRegistry, true);
					}
				});

				menuManager.addSeparator();

				menuManager.add(new ChildDialogAction(Img.PACKAGE.get(), Msg.add_archive.get()) {
					public boolean doRun() {
						return IndexPanel.createFileTaskFromDialog(IndexingPanel.this.getShell(), indexRegistry, false);
					}
				});

				menuManager.add(new ChildDialogAction(Img.EMAIL.get(), Msg.add_outlook_pst.get()) {
					public boolean doRun() {
						return IndexPanel.createOutlookTaskFromDialog(IndexingPanel.this.getShell(), indexRegistry);
					}
				});
				
				menuManager.add(new MenuAction(Img.CLIPBOARD.get(), Msg.add_from_clipboard.get()) {
					public void run() {
						IndexPanel.createTaskFromClipboard(IndexingPanel.this.getShell(), indexRegistry);
					}
				});
				
				menuManager.show();
			}
		});
	}
	
	private void initEventHandlers() {
		addedListener = new Event.Listener<Task>() {
			public void update(final Task task) {
				Util.runSwtSafe(tabFolder, new Runnable() {
					public void run() {
						boolean isUpdate = task.is(IndexAction.UPDATE);
						boolean noTabs = tabFolder.getItemCount() == 0;
						addTab(task, !isUpdate || noTabs);
					}
				});
			}
		};
		
		removedListener = new Event.Listener<Task>() {
			public void update(final Task task) {
				
				Util.runSwtSafe(tabFolder, new Runnable() {
					public void run() {
						for (CTabItem item : tabFolder.getItems()) {
							if (item.getData() == task) {
								item.dispose();
								break;
							}
						}
						
						/* If there are no more tabs and no child dialogs are open, close the indexing dialog. 
						if (!childDialogOpen && tabFolder.getItemCount() == 0)
							indexRegistry.getQueue().removeListeners(addedListener, removedListener);
						*/
					}
				});
			}
		};
		
		/* Hook onto the indexing queue, i.e. register the listeners and create tabs for existing tasks as necessary. */
		indexRegistry.getQueue().addListeners(new ExistingTasksHandler() {
			public void handleExistingTasks(List<Task> tasks) {
				boolean selectTab = tabFolder.getItemCount() == 0;
				for (Task task : tasks) {
					addTab(task, selectTab);
					selectTab = false;
				}
			}
		}, addedListener, removedListener);
		
		/* When the indexing dialog is closed, cancel all tasks and unregister the listeners. */
		IndexingPanel.this.getShell().addShellListener(new ShellAdapter() {
			public void shellClosed(final ShellEvent e) {
				indexRegistry.getQueue().removeAll(new CancelHandler() {
					public CancelAction cancel() {
						CancelAction action = confirmCancel();
						e.doit = action != null;
						return action;
					}
				}, addedListener, removedListener);
			}
		});
		
		// TODO - Figure out how to fix the tab closing problem
		// Handle closing of tabs by the user
		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(final CTabFolderEvent event) {
				if (tabFolder.getItemCount() == 1) {
					/*
					 * Closing the last tab automatically triggers the closing of the shell. However, there can be a considerable delay 
					 * between the former and the latter, allowing the user to click on the close button of the shell before the shell 
					 * is closed automatically. If the user does that, there will be the strange effect of DocFetcher asking for 
					 * keep/discard/cancel confirmation *twice*: First after clicking the close button of the tab, then after clicking 
					 * the close button of the shell. To avoid this, when the user tries to close the last tab, we'll close the shell instead.
					 * 
					 * Note that event.doit must be set to false here: When the shell is about to be closed and the confirmation 
					 * dialog shows up, the user could click on the 'Cancel' button, in which case the tab should remain open.
					 */
					
					//event.doit = false;
					return;
				}
				Task task = (Task) event.item.getData();
				task.remove(new CancelHandler() {
					public CancelAction cancel() {
						CancelAction action = confirmCancel();
						event.doit = action != null;
						return action;
					}
				});
			}
		});
	}
	
	private void addTab(@NotNull final Task task, boolean selectTab) {
		/* Create and configure tab item */
		final CTabItem tabItem = new CTabItem(tabFolder, SWT.CLOSE);
		tabItem.setData(task);
		LuceneIndex index = task.getLuceneIndex();
		
		File rootFile = index.getCanonicalRootFile();
		String nameOrLetter = Util.getNameOrLetter(rootFile, ":\\");
		tabItem.setText(Util.truncate(nameOrLetter));
		tabItem.setToolTipText(Util.getSystemAbsPath(rootFile));
		
		if (task.is(TaskState.READY))
			tabItem.setImage(Img.TREE.get());
		else
			tabItem.setImage(Img.CHECK.get());
		
		/*
		 * The tab item's control will not be disposed when the tab item is disposed, so this dispose listener is necessary. Note that the 
		 * control to be disposed might be either the configuration panel or the progress panel, so calling configPanel.dispose() is not correct.
		 */
		tabItem.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				tabItem.getControl().dispose();
			}
		});

		final IndexingConfig config = index.getConfig();
		
		if (task.is(IndexAction.UPDATE) || !task.is(TaskState.NOT_READY)) {
			switchToProgressPanel(task, tabItem, config);
		}
		else {
			final ConfigPanel configPanel;
			if (index instanceof FileIndex)
				configPanel = new FileConfigPanel(tabFolder, index);
			else if (index instanceof OutlookIndex)
				configPanel = new OutlookConfigPanel(tabFolder, index);
			else
				throw new IllegalStateException();
			tabItem.setControl(configPanel.getControl());

			// Move focus away from tab item, or else the tab title will be underlined.
			configPanel.getControl().setFocus();

			configPanel.evtRunButtonClicked.add(new Event.Listener<Void>() {
				public void update(Void eventData) {
					tabItem.setImage(Img.TREE.get());
					configPanel.getControl().dispose();
					switchToProgressPanel(task, tabItem, config);
					task.setReady();
					
					/*
					 * Switch to next waiting tab. This is thread-safe even though it's a check-then-act operation
					 * because only the user can change the task state from 'not ready' to 'ready'.
					 */
					for (CTabItem candidateItem : tabFolder.getItems()) {
						Task candidateTask = (Task) candidateItem.getData();
						if (!candidateTask.is(TaskState.READY)) {
							tabFolder.setSelection(candidateItem);
							break;
						}
					}
				}
			});
		}

		if (selectTab)
			tabFolder.setSelection(tabItem);
	}
	
	private void switchToProgressPanel(@NotNull final Task task, @NotNull final CTabItem tabItem, @NotNull final IndexingConfig config) {
		ProgressPanel progressPanel = new ProgressPanel(tabFolder);
		tabItem.setControl(progressPanel.getControl());
		final ProgressReporter reporter = new ProgressReporter(progressPanel);
		
		task.attachReporter(reporter, new ExistingMessagesHandler() {
			public void handleMessages(	List<IndexingInfo> infos, List<IndexingError> errors) {
				/* Dangerous section: This method runs under the lock of the DelegatingReporter instance, so beware of lock-ordering deadlocks. */
				for (IndexingInfo info : infos)
					reporter.info(info);
				for (IndexingError error : errors)
					reporter.fail(error);
			}
		});
		
		task.evtFinished.add(new Event.Listener<Boolean>() {
			public void update(Boolean eventData) {
				if (eventData) {
					Util.runAsyncExec(tabItem, new Runnable() {
						public void run() {
							tabItem.setImage(Img.WARNING_BIG.get());
						}
					});
				}
			}
		});
		
		/* For unknown reasons, without this line the progress table won't scroll to the bottom until the next message comes in. */
		progressPanel.getProgressTable().scrollToBottom();
		
		progressPanel.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				task.detachReporter(reporter);
			}
		});
	}
	
	@Nullable
	private CancelAction confirmCancel() {
		MultipleChoiceDialog<CancelAction> dialog = new MultipleChoiceDialog<CancelAction>(IndexingPanel.this.getShell());
		dialog.setTitle(Msg.abort_indexing.get());
		dialog.setText(Msg.keep_partial_index.get());
		dialog.addButton(Msg.keep.get(), CancelAction.KEEP);
		dialog.addButton(Msg.discard.get(), CancelAction.DISCARD);
		dialog.addButton(Msg.dont_abort.get(), null);
		return dialog.open();
	}
	
}