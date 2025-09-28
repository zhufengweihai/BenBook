package io.legado.app.ui.main.source

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexboxLayout
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.databinding.FragmentSourceDetailBinding
import io.legado.app.databinding.ItemFilletTextBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.source.exploreKinds
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.activity
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.launch

class SourceDetailFragment() : VMBaseFragment<SourceDetailViewModel>(R.layout.fragment_source_detail),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    constructor(bookSourceUrl: String) : this() {
        val bundle = Bundle()
        bundle.putString("bookSourceUrl", bookSourceUrl)
        bundle.putBoolean("attachToActivity", true)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")
    override val viewModel by viewModels<SourceDetailViewModel>()
    private val binding by viewBinding(FragmentSourceDetailBinding::bind)
    private var sourceUrl: String? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        lifecycleScope.launch {
            sourceUrl = arguments?.getString("bookSourceUrl") ?: AppConfig.bookSourceUrl
            sourceUrl?.let {
                appDb.bookSourceDao.getBookSource(it)?.let { bookSource ->
                    binding.tvBookSource.text = bookSource.bookSourceName
                    upKindList(binding.flexbox, it, bookSource.exploreKinds())
                }
            }
        }
        binding.tvMoreSource.setTextColor(ThemeStore.accentColor(requireContext()))
        binding.tvMoreSource.setOnClickListener {
            AppConfig.bookSourceUrl = null
            postEvent(EventBus.NOTIFY_MAIN, false)
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.source_detail, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.action_edit -> {
                startActivity<BookSourceEditActivity> {
                    sourceUrl?.let { putExtra("sourceUrl", sourceUrl) }
                }
            }
        }
    }

    private fun upKindList(flexbox: FlexboxLayout, sourceUrl: String, kinds: List<ExploreKind>) {
        if (kinds.isNotEmpty()) kotlin.runCatching {
            val inflater = LayoutInflater.from(requireContext())
            flexbox.visible()
            kinds.forEach { kind ->
                val tv = ItemFilletTextBinding.inflate(inflater, flexbox, false).root
                flexbox.addView(tv)
                tv.text = kind.title
                val lp = tv.layoutParams as FlexboxLayout.LayoutParams
                kind.style().let { style ->
                    lp.flexGrow = style.layout_flexGrow
                    lp.flexShrink = style.layout_flexShrink
                    lp.alignSelf = style.alignSelf()
                    lp.flexBasisPercent = style.layout_flexBasisPercent
                    lp.isWrapBefore = style.layout_wrapBefore
                }
                if (kind.url.isNullOrBlank()) {
                    tv.setOnClickListener(null)
                } else {
                    tv.setOnClickListener {
                        if (kind.title.startsWith("ERROR:")) {
                            it.activity?.showDialogFragment(TextDialog("ERROR", kind.url))
                        } else {
                            openExplore(sourceUrl, kind.title, kind.url)
                        }
                    }
                }
            }
        }
    }

    private fun openExplore(sourceUrl: String, title: String, exploreUrl: String?) {
        if (exploreUrl.isNullOrBlank()) return
        startActivity<ExploreShowActivity> {
            putExtra("exploreName", title)
            putExtra("sourceUrl", sourceUrl)
            putExtra("exploreUrl", exploreUrl)
        }
    }
}