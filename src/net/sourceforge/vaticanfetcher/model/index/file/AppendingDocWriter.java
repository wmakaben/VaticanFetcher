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

package net.sourceforge.vaticanfetcher.model.index.file;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vaticanfetcher.model.Fields;
import net.sourceforge.vaticanfetcher.model.parse.ParseResult;
import net.sourceforge.vaticanfetcher.util.annotations.NotNull;
import net.sourceforge.vaticanfetcher.util.annotations.Nullable;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;

final class AppendingDocWriter extends LuceneDocWriter {
	
	@Nullable private Document luceneDoc;
	
	protected boolean appendMetadata() {
		// Only append metadata for the first document
		return luceneDoc == null;
	}
	
	public void write(@NotNull FileDocument doc, @NotNull Document luceneDoc, boolean added) throws IOException {
		if (this.luceneDoc == null)
			this.luceneDoc = luceneDoc;
		else
			for (Fieldable field : luceneDoc.getFields(Fields.CONTENT.key()))
				this.luceneDoc.add(field);
	}

	public void update(@NotNull FileDocument doc, @NotNull File file, @NotNull ParseResult parseResult) throws IOException {
		throw new UnsupportedOperationException();
	}

	public void delete(@NotNull String uid) throws IOException {
		throw new UnsupportedOperationException();
	}
	
	@Nullable
	public Document getLuceneDoc() {
		return luceneDoc;
	}

}
