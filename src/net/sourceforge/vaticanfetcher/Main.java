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

package net.sourceforge.vaticanfetcher;

import java.lang.reflect.Method;

import net.sourceforge.vaticanfetcher.util.SwtJarLoader;

public final class Main {
	
	private Main() {
	}

	public static void main(String[] args) throws Exception {
		SwtJarLoader.loadSwtJar();
		String appClassName = "net.sourceforge.vaticanfetcher.gui.Application";
		Class<?> appClass = Class.forName(appClassName);
		Class<?>[] paramTypes = new Class<?>[] {String[].class};
		Method launchMethod = appClass.getMethod("main", paramTypes);
		launchMethod.invoke(null, new Object[] {args});
	}

}