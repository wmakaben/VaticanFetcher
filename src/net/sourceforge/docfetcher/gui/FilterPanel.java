package net.sourceforge.docfetcher.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import net.sourceforge.docfetcher.gui.filter.IndexPanel;

public abstract class FilterPanel extends Composite {

	private CLabel createIndex;
	private Composite comp;
	private IndexPanel indexPanel;

	public FilterPanel(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new FormLayout());
		
		createIndex = new CLabel(this, SWT.NONE);
		createIndex.setText("Search Scope\nRight click to create an index");
		
		FormData fd = new FormData();
		createIndex.setLayoutData(fd);
		
		comp = new Composite(this, SWT.NONE);
		comp.setLayout(new FillLayout());
		
		indexPanel = createIndexPanel(comp);
		
		fd = new FormData();
		fd.top = new FormAttachment(createIndex, 5);
		fd.bottom = new FormAttachment(100, -5);
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(100, -5);
		comp.setLayoutData(fd);
	}
	
	protected abstract IndexPanel createIndexPanel(Composite parent);
	
	public IndexPanel getIndexPanel(){ return indexPanel; }

}
