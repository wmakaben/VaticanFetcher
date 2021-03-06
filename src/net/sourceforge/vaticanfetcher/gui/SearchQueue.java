
package net.sourceforge.vaticanfetcher.gui;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sourceforge.vaticanfetcher.enums.Msg;
import net.sourceforge.vaticanfetcher.enums.ProgramConf;
import net.sourceforge.vaticanfetcher.gui.ResultPanel.HeaderMode;
import net.sourceforge.vaticanfetcher.gui.filter.IndexPanel;
import net.sourceforge.vaticanfetcher.model.IndexRegistry;
import net.sourceforge.vaticanfetcher.model.LuceneIndex;
import net.sourceforge.vaticanfetcher.model.TreeCheckState;
import net.sourceforge.vaticanfetcher.model.search.ResultDocument;
import net.sourceforge.vaticanfetcher.model.search.SearchException;
import net.sourceforge.vaticanfetcher.model.search.Searcher;
import net.sourceforge.vaticanfetcher.util.AppUtil;
import net.sourceforge.vaticanfetcher.util.CheckedOutOfMemoryError;
import net.sourceforge.vaticanfetcher.util.Event;
import net.sourceforge.vaticanfetcher.util.Util;
import net.sourceforge.vaticanfetcher.util.annotations.NotNull;
import net.sourceforge.vaticanfetcher.util.annotations.Nullable;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

import com.google.common.base.Strings;

public final class SearchQueue {
	
	private static enum GuiEvent {
		SEARCH_OR_LIST, LOCATION
	}
	
	private static final String spaces = Strings.repeat(" ", 5);

	private final SearchBar searchBar;
	private final IndexPanel indexPanel;
	private final ResultPanel resultPanel;
	private final StatusBar statusBar;
	
	private final Thread thread;
	private final Lock lock = new ReentrantLock(true);
	private final Condition queueNotEmpty = lock.newCondition();
	private final EnumSet<GuiEvent> queue = EnumSet.noneOf(GuiEvent.class);
	
	@Nullable private volatile String query;
	@Nullable private volatile Set<String> listDocIds;
	@Nullable private List<ResultDocument> results;
	@Nullable private TreeCheckState treeCheckState;
	
	public SearchQueue(	@NotNull SearchBar searchBar, @NotNull IndexPanel indexPanel, @NotNull ResultPanel resultPanel,	@NotNull StatusBar statusBar) {
		Util.checkNotNull(searchBar, indexPanel, resultPanel, statusBar);
		this.searchBar = searchBar;
		this.indexPanel = indexPanel;
		this.resultPanel = resultPanel;
		this.statusBar = statusBar;
		
		// Updates info display in status bar when files are selected
		resultPanel.evtSelection.add(new Event.Listener<List<ResultDocument>>() {
			public void update(List<ResultDocument> eventData) {
				updateResultStatus();
			}
		});
		
		thread = new Thread(SearchQueue.class.getName()) {
			public void run() {
				while (threadLoop());
			}
		};
		thread.start();
		
		initListeners();
	}
	
	private void initListeners() {
		searchBar.getControl().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				thread.interrupt();
			}
		});
		
		searchBar.evtSearch.add(new Event.Listener<String>() {
			public void update(String eventData) {
				lock.lock();
				try {
					query = eventData;
					searchBar.setEnabled(false);
					queue.add(GuiEvent.SEARCH_OR_LIST);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
		
		indexPanel.evtCheckStatesChanged.add(new Event.Listener<Void>() {
			public void update(Void eventData) {
				lock.lock();
				try {
					queue.add(GuiEvent.LOCATION);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
		
		indexPanel.evtListDocuments.add(new Event.Listener<Set<String>>() {
			public void update(Set<String> eventData) {
				lock.lock();
				try {
					listDocIds = eventData;
					queue.add(GuiEvent.SEARCH_OR_LIST);
					queueNotEmpty.signal();
				}
				finally {
					lock.unlock();
				}
			}
		});
	}
	
	private boolean threadLoop() {
		final EnumSet<GuiEvent> queueCopy;
		final String query;
		final Set<String> listDocIds;
		
		lock.lock();
		try {
			while (queue.isEmpty())
				queueNotEmpty.await();
			
			queueCopy = EnumSet.copyOf(queue);
			queue.clear();
			query = this.query;
			listDocIds = this.listDocIds;
			this.query = null;
			this.listDocIds = null;
		}
		catch (InterruptedException e) {
			return false;
		}
		finally {
			lock.unlock();
		}
		
		IndexRegistry indexRegistry = indexPanel.getIndexRegistry();
		
		// Run search
		if (queueCopy.contains(GuiEvent.SEARCH_OR_LIST)) {
			try {
				Searcher searcher = indexRegistry.getSearcher(); // might block
				
				/*
				 * Bug #3538102: The returned searcher is null if IndexRegistry.getSearcher() was blocking and the thread is interrupted. This can happen as follows: 
				 * (1) The user has a lot of indexes and/or the indexes are very large, so that loading them on startup takes a long time. 
				 * (2) During startup, when the indexes are loaded, the user enters something into the search field and presses Enter. 
				 * (3) VaticanFetcher blocks because it can't start searching until all indexes have been loaded. Seeing that the program has apparently frozen, 
				 * the user closes the program. This interrupts the searcher thread, causing the IndexRegistry.getSearcher() method to unblock and return null.
				 */
				if (searcher == null)
					return false;
				
				if (query != null)
					results = searcher.search(query);
				else if (listDocIds != null)
					results = searcher.list(listDocIds);
				else
					throw new IllegalStateException();
			}
			catch (SearchException e) {
				AppUtil.showError(e.getMessage(), true, true);
				Util.runSyncExec(searchBar.getControl(), new Runnable() {
					public void run() {
						searchBar.setEnabled(true);
					}
				});
				
				// Don't return yet, we might have to update the filters
				results = null;
			}
			catch (CheckedOutOfMemoryError e) {
				UtilGui.showOutOfMemoryMessage(searchBar.getControl(), e);
			}
		}
		
		// Build location filter
		if (treeCheckState == null || queueCopy.contains(GuiEvent.LOCATION))
			treeCheckState = indexRegistry.getTreeCheckState();
		
		/* No need to update the result panel if the user changed the filter settings before having run any searches. */
		if (results == null)
			return true;
		
		final List<ResultDocument> visibleResults = new ArrayList<ResultDocument>();

		// Apply filters
		for (ResultDocument doc : results) {
			if (!treeCheckState.isChecked(doc.getParentPath()))
				continue;
			visibleResults.add(doc);
		}
		
		boolean filesFound = false;
		boolean emailsFound = false;
		for (LuceneIndex index : indexRegistry.getIndexes()) {
			if (index.isEmailIndex())
				emailsFound = true;
			else
				filesFound = true;
		}
		final HeaderMode mode = HeaderMode.getInstance(filesFound, emailsFound);
		
		// Set results
		Util.runSyncExec(searchBar.getControl(), new Runnable() {
			public void run() {
				resultPanel.setResults(visibleResults, mode);
				resultPanel.sortByColumn(ProgramConf.Int.InitialSorting.get());
				if (queueCopy.contains(GuiEvent.SEARCH_OR_LIST))
					resultPanel.getControl().setFocus();
				updateResultStatus(); // Must be done *after* setting the results
				searchBar.setEnabled(true);
				
				if (query != null)
					searchBar.addToSearchHistory(query);
			}
		});
		
		return true;
	}
	
	private void updateResultStatus() {
		int resultCount = resultPanel.getItemCount();
		String msg = Msg.num_results.format(resultCount);
		if (resultCount >= Searcher.MAX_RESULTS)
			msg += "+";
		int selCount = resultPanel.getSelection().size();
		if (selCount > 1)
			msg += spaces + Msg.num_sel_results.format(selCount);
		statusBar.setResultLabelVisible(true);
		statusBar.setResultLabelText(msg);
	}

}
