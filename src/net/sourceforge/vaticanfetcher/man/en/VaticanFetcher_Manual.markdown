Introduction
============

Vatican Fetcher is a desktop search application that allows you to search the contents of files on your computer.

**Index-based Search**: Directly searching through a large number of documents is impractically slow, so Vatican Fetcher requires that you create `indexes` for folders you want to search in. Folders only need to be indexed once and then you can search through that folder as many times as you want.

**Creating an Index**: Go to the `Filters` tab in the large tab panel on the right (or bottom) side and right-click on the `Search Scope` area. Select `Create Index From > Folder`, choose the folder, and select the configurations for the indexing (default works fine). Click `Run` and wait for the folder indexing to finish. Folders can also be indexed from the `Index Status` tab by clicking on the button in the top right of the panel.

**Searching**: Enter one or more words to search for in the text field in the top left and press `Enter`. The results are displayed in the big result panel on the left (or top)

**Result and Previews**: To the right of the result panel (or below), is the tab panel which consists of the `Preview`, `Filters`, `Index Status` tabs. If you select a file on the result panel, the preview panel will display a text-only preview of the file's contents. To bring up this manual, just click the question mark button in the top right. The preview panel includes highlighting of search terms and a built-in web browser for HTML files. Files can also be double clicked on the result panel to open the file in an external program

**Sorting**: By clicking on the result panel's column header, you can sort the results by that column value

**Filtering**: In the `Filters` tab, you can select which folders you want to search through by checking/unchecking the items in the `Search Scope` area.

**Index Updates**: Changes to indexed folders (additions, removals, modifications) are updated automatically by the Vatican Fetcher's folder watcher when Vatican Fetcher is running and a daemon when it is not running. In cases where the Vatican Fetcher is unable to automatically update a folder (network shares), the updates can be done manually by right-clicking on the folder in the `Search Scope`area and selecting the option to update that folder.

* * *

<a name="Advanced_Usage"></a> <!-- Do not translate this line, just copy it verbatim. -->

Advanced Usage
==============

**Query Syntax**: Searches are not limited to a simple word lookup. There are many features including searching for a certain phrase and searching for words with a common start. For the full list of supported search features, see the [query syntax section](VaticanFetcher_Manual_files/Query_Syntax.html).

**Indexing Configuration Options**: For a detailed discussion of all options on the indexing configuration window, click [here](VaticanFetcher_Manual_files/Indexing_Options.html).<br>Some interesting configuration options are:

* ***Customizable file extensions***: File extensions for plain text files and zip archives are fully customizable.
* ***File Exclusion***: You can exclude certain files from indexing based on regular expressions.
* ***Mime type detection***: Without mime type detection, Vatican Fetcher will just look at a file's extension (e.g. `'.doc'`) to determine its file type. With mime type detection, Vatican Fetcher will also peek into the file's contents to see if it can find any better type info. This is slower than just checking the file extension, but it's useful for files that have the wrong file extension.
* ***HTML pairing***: HTML files and its associated folder are treated as a single document.

**Regular Expressions**: Both the file exclusion and the mime type detection rely on so-called *regular expressions*. These are user-defined patterns that Vatican Fetcher will match against filenames or filepaths. For example, to exclude all files starting with the word "journal", you can use this regular expression: `journal.*`. Note that this is slightly different from Vatican Fetcher's query syntax, where you would omit the dot: `journal*`. If you want to know more about regular expressions, read this [brief introduction](VaticanFetcher_Manual_files/Regular_Expressions.html).

* * *

<a name="Other Notes"></a> <!-- Do not translate this line, just copy it verbatim. -->

Other Notes
==========================

**Raising the memory limit**: Vatican Fetcher, like all Java programs, has a fixed limit on memory usage, known as the *Java heap size*. This memory limit must be set on startup, and the default value is 256&nbsp;MB. If you try to index a very large number of files, and/or if some of the indexed files are really huge, then chances are Vatican Fetcher will hit that memory limit. If this ever happens, you might want to [raise the memory limit](VaticanFetcher_Manual_files/Memory_Limit.html).

**Don't index system folders**: In contrast to other desktop search applications, Vatican Fetcher was not designed for indexing system folders such as `C:`or `C:\Windows`. Doing so is discouraged for the following reasons:

1. ***Slowdown***: The files in system folders are modified very frequently. Folder watching slows down your computer when it has to constantly update the system folder indexes.
2. ***Memory issues***: Vatican Fetcher keeps a tiny representations of your files in memory and system folders usually contain a very large number of files, so Vatican Fetcher will be more leikely to run out of memory if you index system folders.
3. ***Waste of resources, worse search results***: Indexing system folders is most likely a waste of indexing time and disk space and will also pollute your search results with unneeded system files. So, for the best results in the least amount of time, just index what you need.

**Unicode support**: Vatican Fetcher has full Unicode support for all document formats except CHM files. In case of plain text files, Vatican Fetcher has to use [certain heuristics](http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html) to guess the correct encoding, since plain text files don't contain any explicit encoding information.

**CHM files**: CHM files are not supported on Mac OS&nbsp;X and 64-bit Java. This means on Windows and Linux you might have to replace your 64-bit Java runtime with its 32-bit counterpart to get support for CHM files.

**Archive support**:Vatican Fetcher currently supports the following archive formats: zip and derived formats, 7z, rar and the whole tar.* family. Additionally, executable zip and 7z archives are supported as well, but not executable rar archives. Vatican Fetcher treats all archives as ordinary folders, and can also handle deep nesting of archives.<!-- this line should end with two spaces -->
With that said, support for zip and 7z archives is best in terms of robustness and speed. On the other hand, indexing of tar.gz, tar.bz2 and similar formats tends to be less efficient. This is due to the fact that these formats don't have an internal "summary" of the archive contents, which forces Vatican Fetcher to unpack the entire archive rather than only individual archive entries. Bottom line: If you have the choice, compress your files either as zip or 7z archives for maximum compatibility with Vatican Fetcher.

**Vatican Fetcher daemon**: The daemon is a simple program with low memory footprint and CPU usage because it doesn't do much besides watching folders. It most likely won't cause your computer to slow down or crash.

* * *

<a name="Subpages"></a> <!-- Do not translate this line, just copy it verbatim. -->

Manual Subpages
===============
* [Query syntax](VaticanFetcher_Manual_files/Query_Syntax.html)
* [Indexing options](VaticanFetcher_Manual_files/Indexing_Options.html)
* [Regular expressions](VaticanFetcher_Manual_files/Regular_Expressions.html)
* [How to raise the memory limit](VaticanFetcher_Manual_files/Memory_Limit.html)
* [How to raise the folder watch limit (Linux)](VaticanFetcher_Manual_files/Watch_Limit.html)
* [Preferences](VaticanFetcher_Manual_files/Preferences.html)
