package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.gui.indexing.IndexingPanel;
import net.sourceforge.docfetcher.gui.preview.PreviewPanel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public abstract class PanelForm extends SashForm{
	
	private ResultPanel resultPanel;
	private TabPanel tabPanel;
	
	private boolean isHorizontal;

	public PanelForm(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FillLayout());
		
		isHorizontal = true;	// Change this to take in saved preferences
		this.setOrientation(isHorizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
		
		resultPanel = createResultPanel(this);
		tabPanel = createTabPanel(this);
	}
	
	protected abstract ResultPanel createResultPanel(Composite parent);
	protected abstract TabPanel createTabPanel(Composite parent);
	
	public ResultPanel getResultPanel(){ return resultPanel; }
	public TabPanel getTabPanel(){ return tabPanel; }
	public PreviewPanel getPreviewPanel(){ return tabPanel.getPreviewPanel(); }
	public FilterPanel getFilterPanel(){ return tabPanel.getFilterPanel(); }
	public IndexingPanel getIndexPanel(){ return tabPanel.getIndexPanel(); }
	
	public boolean isHorizontal(){ return isHorizontal; }
	
	/** Changes the orientation of the panel to horizontal or vertical */
	public void switchOrientation(){
		isHorizontal = !isHorizontal;
		this.setOrientation(isHorizontal ? SWT.HORIZONTAL : SWT.VERTICAL);
	}
	
	
}
