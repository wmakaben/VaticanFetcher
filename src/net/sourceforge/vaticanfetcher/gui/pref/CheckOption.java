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
/**
 * @author Tran Nam Quang
 */

package net.sourceforge.vaticanfetcher.gui.pref;

import net.sourceforge.vaticanfetcher.enums.SettingsConf;
import net.sourceforge.vaticanfetcher.gui.pref.PrefDialog.PrefOption;
import net.sourceforge.vaticanfetcher.util.Util;
import net.sourceforge.vaticanfetcher.util.annotations.NotNull;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

final class CheckOption extends PrefOption {
	
	private final SettingsConf.Bool enumOption;
	@NotNull private Button bt;

	public CheckOption(	@NotNull String labelText, @NotNull SettingsConf.Bool enumOption) {
		super(labelText);
		this.enumOption = enumOption;
	}

	public void createControls(@NotNull Composite parent) {
		bt = Util.createCheckButton(parent, labelText);
		bt.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false, 2, 1));
		bt.setSelection(enumOption.get());
	}

	protected void restoreDefault() {bt.setSelection(enumOption.defaultValue);}

	protected void save() {enumOption.set(bt.getSelection());}
	
}