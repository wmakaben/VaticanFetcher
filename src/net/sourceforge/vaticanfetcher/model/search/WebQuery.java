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

package net.sourceforge.vaticanfetcher.model.search;

import java.util.Collection;

import net.sourceforge.vaticanfetcher.model.LuceneIndex;
import net.sourceforge.vaticanfetcher.model.parse.Parser;
import net.sourceforge.vaticanfetcher.util.Util;
import net.sourceforge.vaticanfetcher.util.annotations.NotNull;
import net.sourceforge.vaticanfetcher.util.annotations.Nullable;

public final class WebQuery {
	
	final String query;
	final int pageIndex;
	@Nullable Long minSize;
	@Nullable Long maxSize;
	@Nullable Collection<Parser> parsers;
	@Nullable Collection<LuceneIndex> indexes;

	/**
	 * Constructs a new query object for the given query string. The given zero-based page index specifies which page from 
	 * the full set of results will be returned by the {@link Searcher} instance this query will be submitted to.
	 * <p>
	 * If the specified page index is out of range, it will be clamped. The clamped page index 
	 * (i.e. the "actual" page index) can be retrieved from the result object via {@link Searcher.ResultPage#pageIndex}.
	 * <p>
	 * If the caller does not know in advance how many pages will be available (this is usually the case when 
	 * the initial query is submitted), a page index of 0 should be specified.
	 */
	public WebQuery(@NotNull String query, int pageIndex) {
		this.query = Util.checkNotNull(query);
		this.pageIndex = pageIndex;
	}

	/** Sets the file types to be included in the results. If null is specified, all types will be included. */
	public void setIncludedTypes(@Nullable Collection<Parser> parsers) {
		this.parsers = parsers;
	}
	
	/** Sets the locations to be included in the results. If null is specified, all locations will be included. */
	public void setIncludedIndexes(@Nullable Collection<LuceneIndex> indexes) {
		this.indexes = indexes;
	}
	
}
