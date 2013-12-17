/*******************************************************************************
 * Copyright (c) 2011 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.vaticanfetcher.gui.pref;

import net.sourceforge.vaticanfetcher.enums.SettingsConf;
import net.sourceforge.vaticanfetcher.gui.UtilGui;
import net.sourceforge.vaticanfetcher.gui.pref.PrefDialog.PrefOption;
import net.sourceforge.vaticanfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;

final class HotkeyOption extends PrefOption {
	
	@NotNull private StyledLabel st;
	@NotNull private int[] hotkey;

	public HotkeyOption(@NotNull String labelText) {
		super(labelText);
	}

	public void createControls(@NotNull Composite parent) {
		st = PrefDialog.createLabeledStyledLabel(parent, labelText);
		st.setCursor(st.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

		hotkey = SettingsConf.IntArray.HotkeyToFront.get();
		st.setText(UtilGui.toString(hotkey));

		st.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				HotkeyDialog dialog = new HotkeyDialog(st.getShell());
				hotkey = dialog.open();
				st.setText(UtilGui.toString(hotkey));
			}
		});
	}

	protected void restoreDefault() {
		hotkey = SettingsConf.IntArray.HotkeyToFront.defaultValue;
		st.setText(UtilGui.toString(hotkey));
	}

	protected void save() {	SettingsConf.IntArray.HotkeyToFront.set(hotkey); }
	
}