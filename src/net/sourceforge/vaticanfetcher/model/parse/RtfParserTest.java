/*******************************************************************************
 * Copyright (c) 2013 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.vaticanfetcher.model.parse;

import java.io.File;

import net.sourceforge.vaticanfetcher.TestFiles;
import net.sourceforge.vaticanfetcher.model.Cancelable;
import net.sourceforge.vaticanfetcher.model.index.IndexingError;
import net.sourceforge.vaticanfetcher.model.index.IndexingReporter;
import net.sourceforge.vaticanfetcher.model.index.file.FileIndex;

import org.junit.Test;

public final class RtfParserTest {
	
	@Test
	public void testTextMakerRtf() {
		/*
		 * RTF files created by TextMaker caused parse errors in vaticanfetcher
		 * 1.1.6 and earlier. See:
		 * http://sourceforge.net/p/docfetcher/discussion/702424/thread/8a3dd4f6/
		 */
		
		// Require assertions to be enabled
		boolean assertionsEnabled = false;
		assert assertionsEnabled = true; // Intentional side-effect
		if (!assertionsEnabled)
			throw new IllegalStateException("Assertions must be enabled.");
		
		File dir = TestFiles.textmaker_rtf.get();
		FileIndex index = new FileIndex(null, dir);
		IndexingReporter reporter = new IndexingReporter() {
			public void fail(IndexingError error) {
				throw new AssertionError();
			}
		};
		index.update(reporter, Cancelable.nullCancelable);
	}

}
