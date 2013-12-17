/*******************************************************************************
 * Copyright (c) 2012 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.vaticanfetcher.model.index.file;

import net.sourceforge.vaticanfetcher.util.annotations.NotNull;

final class WrappedStackOverflowError extends Error {

	private static final long serialVersionUID = 1L;
	
	public WrappedStackOverflowError(@NotNull String msg, @NotNull StackOverflowError cause) {
		super(msg, cause);
	}

}
