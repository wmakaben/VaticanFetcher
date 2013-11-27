package net.sourceforge.docfetcher.gui;

import net.sourceforge.docfetcher.util.Event;
import net.sourceforge.docfetcher.util.Util;
import net.sourceforge.docfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;

public class StatusBar extends Composite{

	private CLabel resultLabel;
	private CLabel indexLabel;
	private boolean isDone;
	
	public final Event<Void> evtIndexLabelClicked = new Event<Void>();
	
	public StatusBar(@NotNull Composite parent) {
		super(parent, SWT.BORDER);
		setLayout(new FormLayout());
		
		// Creates the result label - displays # of files found in a search
		resultLabel = new CLabel(this, SWT.NONE);
		resultLabel.setVisible(false);
		
		// Creates the index label - displays if there is an indexing running or if the indexing has finished
		indexLabel = new CLabel(this,SWT.RIGHT);
		indexLabel.setVisible(false);
		
		indexLabel.addMouseListener(new MouseAdapter(){
			public void mouseUp(MouseEvent e) {
				evtIndexLabelClicked.fire(null);
			}
		});
		
		isDone = true;
		
		// Sets up the layout of the status bar
		FormData fd = new FormData();
		fd = new FormData();
		fd.left = new FormAttachment(0, 5);
		fd.right = new FormAttachment(0, 100);
		resultLabel.setLayoutData(fd);
		fd = new FormData();
		fd.right = new FormAttachment(100, -5);
		fd.left = new FormAttachment(resultLabel, 5);
		indexLabel.setLayoutData(fd);
		
		Util.addMouseHighlighter(indexLabel);
	}
	
	// Result label - getter/setter methods
	public CLabel getResultLabel(){ return resultLabel; }
	public boolean isResultLabelVisible(){ return resultLabel.isVisible(); }
	public void setResultLabelVisible(boolean setVisible){ resultLabel.setVisible(setVisible); }
	public void setResultLabelText(String text){ resultLabel.setText(text); }
	
	// Index label - getter/setter methods
	public CLabel getIndexLabel(){ return indexLabel; }
	public boolean isIndexLabelVisible(){ return indexLabel.isVisible(); }
	public void setIndexLabelVisible(boolean setVisible){ indexLabel.setVisible(setVisible); }
	public void setIndexLabelText(String text){ indexLabel.setText(text); }
	public void setIndexLabelImage(Image image){ indexLabel.setImage(image); }
	public void setIndexStatus(boolean doneIndexing){ isDone = doneIndexing; }
	public boolean isDoneIndexing(){ return isDone; }
	
}
