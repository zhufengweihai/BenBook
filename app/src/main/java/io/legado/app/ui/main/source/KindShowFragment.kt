package io.legado.app.ui.main.source

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.databinding.FragmentKindShowBinding
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KindShowFragment() : VMBaseFragment<SourceShowViewModel>(R.layout.fragment_kind_show), KindShowAdapter.CallBack {
    constructor(sourceUrl: String?,exploreUrl:String?) : this() {
        val bundle = Bundle()
        bundle.putString("sourceUrl", sourceUrl)
        bundle.putString("exploreUrl", exploreUrl)
        arguments = bundle
    }

    val sourceUrl: String? get() = arguments?.getString("sourceUrl")
    val exploreUrl: String? get() = arguments?.getString("exploreUrl")
    override val viewModel by viewModels<SourceShowViewModel>()
    private val binding by viewBinding(FragmentKindShowBinding::bind)
    private val adapter by lazy { KindShowAdapter(requireContext(), this) }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        lifecycleScope.launch {
            sourceUrl?.let { exploreUrl?.let { it1 -> viewModel.setUrls(it, it1) } }
            viewModel.bookFlow.collectLatest { adapter.submitData(it) }
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
        adapter.addLoadStateListener { loadState ->
            when {
                loadState.refresh is LoadState.Loading -> {
                    binding.loadMoreView.startLoad()
                }
                loadState.refresh is LoadState.Error -> {
                    binding.loadMoreView.error(getString(R.string.error_get_content))
                }
                loadState.append is LoadState.Loading -> {
                    binding.loadMoreView.startLoad()
                }
                loadState.append is LoadState.Error -> {
                    binding.loadMoreView.error(getString(R.string.error_get_content))
                }
                loadState.append is LoadState.NotLoading && loadState.append.endOfPaginationReached -> {
                    binding.loadMoreView.noMore()
                }
                else -> {
                    binding.loadMoreView.stopLoad()
                }
            }
        }
    }

    override fun checkInBookshelf(name: String, author: String, action: (isIn: Boolean) -> Unit) {
        lifecycleScope.launch { action.invoke(viewModel.isInBookShelf(name, author)) }
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }
}