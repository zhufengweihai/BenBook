package io.legado.app.ui.main.source

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.ItemSearchBinding
import io.legado.app.help.config.AppConfig

class KindShowAdapter(val context: Context, val callBack: CallBack) :
    PagingDataAdapter<SearchBook, KindShowAdapter.KindViewHolder>(DIFF_CALLBACK) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KindViewHolder {
        return KindViewHolder(ItemSearchBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: KindViewHolder, position: Int) {
        getItem(holder.layoutPosition)?.let { bind(holder.binding, it) }
        holder.itemView.setOnClickListener {
            getItem(holder.layoutPosition)?.let {
                callBack.showBookInfo(it.toBook())
            }
        }
    }

    private fun bind(binding: ItemSearchBinding, item: SearchBook) {
        binding.run {
            tvName.text = item.name
            tvAuthor.text = context.getString(R.string.author_show, item.author)
            callBack.checkInBookshelf(item.name, item.author, { ivInBookshelf.isVisible = it })
            tvLasted.isGone = item.latestChapterTitle.isNullOrEmpty()
            tvLasted.text = context.getString(R.string.lasted_show, item.latestChapterTitle)
            tvIntroduce.text = item.trimIntro(context)
            val kinds = item.getKindList()
            llKind.isGone = kinds.isEmpty()
            llKind.setLabels(kinds)
            ivCover.load(item.coverUrl, item.name, item.author, AppConfig.loadCoverOnlyWifi, item.origin)
        }

    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchBook>() {
            override fun areItemsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem.bookUrl == newItem.bookUrl
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: SearchBook, newItem: SearchBook): Boolean {
                return oldItem == newItem
            }
        }
    }

    class KindViewHolder(var binding: ItemSearchBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    interface CallBack {
        fun checkInBookshelf(name: String, author: String, action: (isIn: Boolean) -> Unit)
        fun showBookInfo(book: Book)
    }
}