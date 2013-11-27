package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.gui.indexing.IndexingPanel;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public abstract class TabPanel extends Composite{
	
	private final PreviewPanel previewPanel;
	private final FilterPanel filterPanel;
	private final IndexingPanel indexingPanel;
	
	private final CTabFolder folder;
	private final CTabItem preview;
	private final CTabItem filter;
	private final CTabItem index;
	
	public TabPanel(Composite parent) {
		super(parent, SWT.BORDER);
		setLayout(new FillLayout());
		
		folder = new CTabFolder(this, SWT.BORDER);
		folder.setSimple(false);
		
		preview = new CTabItem(folder, SWT.NONE);
		preview.setText("Preview");
		previewPanel = createPreviewPanel(folder);
		preview.setControl(previewPanel);
		
		filter = new CTabItem(folder, SWT.NONE);
		filterPanel = createFilterPanel(folder);
		filter.setText("Filters");
		filter.setControl(filterPanel);
		
		index = new CTabItem(folder, SWT.NONE);
		index.setText("Index Status");
		indexingPanel = createIndexPanel(folder);
		index.setControl(indexingPanel);
		
		selectPreview();
		
	}
	
	protected abstract PreviewPanel createPreviewPanel(Composite parent);
	protected abstract FilterPanel createFilterPanel(Composite parent);
	protected abstract IndexingPanel createIndexPanel(Composite parent);
	
	public PreviewPanel getPreviewPanel(){ return previewPanel; }
	public FilterPanel getFilterPanel(){ return filterPanel; }
	public IndexingPanel getIndexPanel(){ return indexingPanel; }
	
	public void selectPreview(){ folder.setSelection(preview); }
	public void selectFilter(){ folder.setSelection(filter); }
	public void selectIndex(){ folder.setSelection(index); }
}
