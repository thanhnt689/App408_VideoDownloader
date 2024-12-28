package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentBrowserBinding
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.TabManagerProvider
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabFactory
import com.files.video.downloader.videoplayerdownloader.downloader.ui.guide.GuideActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BrowserFragment : BaseFragment<FragmentBrowserBinding>() {

    companion object {
        fun newInstance() = BrowserFragment()
        var DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36"

        // TODO different agents for different androids
        var MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Mobile Safari/537.36"
    }

    override fun getViewBinding(): FragmentBrowserBinding {
        return FragmentBrowserBinding.inflate(layoutInflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.imgGuide.setOnClickListener {
            startActivity(HistoryActivity.newIntent(requireContext()))
        }

        binding.layoutSearch.setOnClickListener {
            if (!aVoidDoubleClick()) {
//                ViewUtils.hideView(true, binding.layoutTitleSearch, 350)
//                ViewUtils.showView(true, binding.layoutSearching, 350)
                binding.edtSearch.requestFocus()
                binding.edtSearch.setText("")
                Handler().postDelayed({
                    KeyboardUtils.showSoftKeyboard(requireActivity())
                }, 300)

            }
        }

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                val input = s.toString()
//                homeViewModel.searchTextInput.set(input)
//                if (!(input.startsWith("http://") || input.startsWith("https://"))) {
//                    homeViewModel.showSuggestions()
//                }
//                homeViewModel.homePublishSubject.onNext(input)
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        binding.layoutGoogle.setOnClickListener {
            openNewTab("https://www.google.com")
        }

        binding.layoutFacebook.setOnClickListener {
            openNewTab("https://www.facebook.com/watch")
        }

        binding.layoutInstagram.setOnClickListener {
            openNewTab("https://www.instagram.com")
        }

        binding.layoutTiktok.setOnClickListener {
            openNewTab("https://www.tiktok.com")
        }

        binding.layoutVimeo.setOnClickListener {
            openNewTab("https://vimeo.com")
        }

        binding.layoutImdb.setOnClickListener {
            openNewTab("https://www.imdb.com")
        }

        binding.layoutDailyMotion.setOnClickListener {
            openNewTab("https://www.dailymotion.com")
        }
    }

    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            val webTab = WebTabFactory.createWebTabFromInput(input)

            val intent = Intent(requireContext(), WebTabActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("webtab", webTab)
            intent.putExtras(bundle)
            startActivity(intent)

        }
    }

}