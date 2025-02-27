package com.files.video.downloader.videoplayerdownloader.downloader.ui.browser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseFragment
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.FragmentBrowserBinding
import com.files.video.downloader.videoplayerdownloader.downloader.dialog.RatingDialog
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.helper.SharedPreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.TabManagerProvider
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.browser.webTab.WebTabFactory
import com.files.video.downloader.videoplayerdownloader.downloader.ui.guide.GuideActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.history.HistoryActivity
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabModelViewModel
import com.files.video.downloader.videoplayerdownloader.downloader.ui.tab.TabsActivity
import com.files.video.downloader.videoplayerdownloader.downloader.util.AdsConstant
import com.files.video.downloader.videoplayerdownloader.downloader.util.EventBusHelper
import com.files.video.downloader.videoplayerdownloader.downloader.util.KeyboardUtils
import com.files.video.downloader.videoplayerdownloader.downloader.util.UpdateEvent
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.play.core.review.ReviewManagerFactory
import com.nlbn.ads.callback.NativeCallback
import com.nlbn.ads.util.Admob
import com.nlbn.ads.util.ConsentHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject
import kotlin.system.exitProcess

@AndroidEntryPoint
class BrowserFragment : BaseFragment<FragmentBrowserBinding>() {


    private val tabModelViewModel: TabModelViewModel by viewModels()

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

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

        binding.tvGoogle.isSelected = true
        binding.tvFacebook.isSelected = true
        binding.tvInstagram.isSelected = true
        binding.tvTiktok.isSelected = true
        binding.tvVimeo.isSelected = true
        binding.tvImdb.isSelected = true
        binding.tvDailyMotion.isSelected = true

        binding.imgGuide.setOnClickListener {
            startActivityResult.launch(GuideActivity.newIntent(requireContext()).apply {
                putExtra("open", "home")
            })
        }

//        tabViewModels.listTabWeb.observe(viewLifecycleOwner) {
//            binding.tvTab.text = it.size.toString()
//        }

        tabModelViewModel.queryAllTabModel().observe(viewLifecycleOwner) {
            binding.tvTab.text = it.size.toString()
        }

        binding.tvTab.setOnClickListener {
            startActivityResult.launch(Intent(requireContext(), TabsActivity::class.java).apply {
                putExtra("open", "home")
            })
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
            newTab("https://www.google.com")
        }

        binding.layoutFacebook.setOnClickListener {
            newTab("https://www.facebook.com/watch")
        }

        binding.layoutInstagram.setOnClickListener {
            newTab("https://www.instagram.com")
        }

        binding.layoutTiktok.setOnClickListener {
            newTab("https://www.tiktok.com")
        }

        binding.layoutVimeo.setOnClickListener {
            newTab("https://vimeo.com")
        }

        binding.layoutImdb.setOnClickListener {
            newTab("https://www.imdb.com")
        }

        binding.layoutDailyMotion.setOnClickListener {
            newTab("https://www.dailymotion.com")
        }

        binding.layoutBookmark.setOnClickListener {
            startActivityResult.launch(HistoryActivity.newIntent(requireContext(), "bookmark")
                .apply {
                    putExtra("start", "home")
                })
        }

        binding.layoutHistory.setOnClickListener {
            startActivityResult.launch(HistoryActivity.newIntent(requireContext(), "history")
                .apply {
                    putExtra("start", "home")
                })
        }

        binding.edtSearch.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.edtSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.edtSearch.clearFocus()
                lifecycleScope.launch {
                    delay(400)
                    newTab((binding.edtSearch as EditText).text.toString())
                    binding.edtSearch.text.clear()
                }
                false
            } else false
        }

        binding.imgSearch.setOnClickListener {
            if (binding.edtSearch.text.isNotEmpty()) {
                newTab((binding.edtSearch as EditText).text.toString())
                binding.edtSearch.text.clear()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBusHelper.register(this)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBusHelper.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: UpdateEvent) {
        Log.d("ntt", "onEvent: $event")
        if (event.name == "Ads") {
            if (ConsentHelper.getInstance(requireActivity()).canRequestAds()) {
                showAdsNativeHome()
            }
        }

        if (event.name == "hide_ads") {
            binding.frAds.visibility = View.GONE
        } else if (event.name == "show_ads") {
            binding.frAds.visibility = View.VISIBLE
        }
    }

    private fun showAdsNativeHome() {

        if (AdsConstant.isLoadNativeHome && Admob.getInstance().isLoadFullAds) {

            if (AdsConstant.nativeAdsHome != null) {
                AdsConstant.nativeAdsHome?.let {
                    val adView = LayoutInflater.from(requireActivity())
                        .inflate(R.layout.layout_ads_native_update, null)
                    val nativeAdView = adView as NativeAdView
                    binding.frAds.removeAllViews()
                    binding.frAds.addView(adView)

                    Admob.getInstance().pushAdsToViewCustom(it, nativeAdView)

                }

            } else {
                Admob.getInstance().loadNativeAd(context,
                    getString(R.string.native_home),
                    object : NativeCallback() {
                        override fun onNativeAdLoaded(nativeAd: NativeAd) {
                            val adView = LayoutInflater.from(context)
                                .inflate(R.layout.layout_ads_native_update, null)
                            val nativeAdView = adView as NativeAdView
                            binding.frAds.removeAllViews()
                            binding.frAds.addView(adView)

                            Admob.getInstance().pushAdsToViewCustom(nativeAd, nativeAdView)

                        }

                        override fun onAdFailedToLoad() {
                            binding.frAds.removeAllViews()
                        }

                    })
            }
        } else {
            binding.frAds.removeAllViews()
            binding.frAds.visibility = View.GONE
        }
    }


    private fun openNewTab(input: String) {
        if (input.isNotEmpty()) {
            val webTab = WebTabFactory.createWebTabFromInput(input)

            tabViewModels.addNewTab(webTab)

            val intent = Intent(requireContext(), WebTabActivity::class.java)
            val bundle = Bundle()
            bundle.putSerializable("webtab", webTab)
            intent.putExtras(bundle)
            startActivity(intent)

        }
    }

    private fun newTab(input: String) {
        val webTab = WebTabFactory.createWebTabFromInput(input)

        val tabModel = WebTabFactory.createTabModelFromInput(input)

        lifecycleScope.launch(Dispatchers.IO) {
            tabModelViewModel.insertTabModel(tabModel)

            withContext(Dispatchers.Main) {
                val intent = Intent(requireContext(), WebTabActivity::class.java)
                val bundle = Bundle()
                bundle.putSerializable("webtab", webTab)
                intent.putExtras(bundle)
                intent.putExtra("open", "home")
                startActivityResult.launch(intent)
            }
        }


//        tabViewModels.addNewTab(webTab)


    }

    val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("ntt", "startActivityResult: ")
        if (AdsConstant.isDownloadSuccessfully) {
            AdsConstant.isDownloadSuccessfully = false
            val countBack = preferenceHelper.getCountBackHome()

            Log.d("ntt", "startActivityResult: " + countBack)

            if (!preferenceHelper.isRate() && (countBack % 2 == 1)) {
                showDialogRate()
            } else {
                preferenceHelper.increaseCountBackHome()

            }
        }
    }

    private fun showDialogRate() {
        binding.frAds.visibility = View.GONE
        val ratingDialog = RatingDialog(requireContext())
        ratingDialog.init(requireContext(), object : RatingDialog.OnPress {
            override fun sendThank() {
                preferenceHelper.forceRated()
                ratingDialog.dismiss()

                Toast.makeText(
                    requireContext(), getString(R.string.string_thank_for_rate), Toast.LENGTH_SHORT
                ).show()


            }

            override fun rating() {
                val manager = ReviewManagerFactory.create(requireContext())
                val request = manager.requestReviewFlow()
                request.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                        flow.addOnSuccessListener {
                            preferenceHelper.forceRated()
                            ratingDialog.dismiss()

                        }
                    } else {
                        preferenceHelper.forceRated()
                        ratingDialog.dismiss()

                    }
                }
            }

            override fun later() {
                ratingDialog.dismiss()

                preferenceHelper.increaseCountBackHome()

            }

        })

        ratingDialog.show()

        ratingDialog.setOnDismissListener {
            binding.frAds.visibility = View.VISIBLE
        }
    }


}