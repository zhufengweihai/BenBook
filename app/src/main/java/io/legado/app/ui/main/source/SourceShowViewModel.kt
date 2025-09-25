package io.legado.app.ui.main.source

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.cachedIn
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.model.webBook.WebBook
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


class SourceShowViewModel(application: Application) : BaseViewModel(application) {
    private var exploreUrl: String? = null
    private val bookSource = MutableStateFlow(BookSource())
    private var books = hashSetOf<SearchBook>()

    fun isInBookShelf(name: String, author: String): Boolean {
        return appDb.bookDao.has(name, author)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val bookFlow: Flow<PagingData<SearchBook>> = bookSource.filter { it.bookSourceUrl.isNotEmpty() }.flatMapLatest {
        Pager(PagingConfig(pageSize = 20)) { KindShowPagingSource(it) }.flow.cachedIn(
            viewModelScope
        )
    }

    fun setUrls(sourceUrl: String, exploreUrl: String?) {
        this.exploreUrl = exploreUrl
        exploreUrl?.let { execute { appDb.bookSourceDao.getBookSource(sourceUrl)?.let { bookSource.value = it } } }

    }

    inner class KindShowPagingSource(val source: BookSource) : PagingSource<Int, SearchBook>() {
        override fun getRefreshKey(state: PagingState<Int, SearchBook>): Int? {
            return state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            }
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchBook> {
            val pageNumber = params.key ?: 1
            return try {
                withTimeout(30000L) {
                    withContext(IO) {
                        val searchBooks = WebBook.exploreBookAwait(source, exploreUrl!!, pageNumber)
                        if (searchBooks.isEmpty() || books.containsAll(searchBooks)) {
                            return@withContext LoadResult.Page(
                                data = emptyList(),
                                prevKey = null,
                                nextKey = null
                            )
                        }
                        books.addAll(searchBooks)
                        appDb.searchBookDao.insert(*searchBooks.toTypedArray())
                        LoadResult.Page(
                            data = searchBooks,
                            prevKey = if (pageNumber == 1) null else pageNumber - 1,
                            nextKey = pageNumber + 1
                        )
                    }
                }
            } catch (e: Exception) {
                LoadResult.Error(e)
            }
        }
    }
}