package com.files.video.downloader.videoplayerdownloader.downloader.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.files.video.downloader.videoplayerdownloader.downloader.R
import com.files.video.downloader.videoplayerdownloader.downloader.base.BaseActivity
import com.files.video.downloader.videoplayerdownloader.downloader.databinding.ActivityPermissionBinding
import com.files.video.downloader.videoplayerdownloader.downloader.helper.PreferenceHelper
import com.files.video.downloader.videoplayerdownloader.downloader.ui.disclaimers.DisclaimersActivity
import com.nlbn.ads.util.AppOpenManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PermissionActivity : BaseActivity<ActivityPermissionBinding>() {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private val REQUEST_CODE_STORAGE = 3
    private val REQUEST_CODE_NOTIFICATION = 4

    private var isPermissionStorage = false
    private var isPermissionNotification = false

    private var checkOpen = false

    override fun setBinding(layoutInflater: LayoutInflater): ActivityPermissionBinding {
        return ActivityPermissionBinding.inflate(layoutInflater)
    }

    override fun initView() {

        checkStoragePermission()
        checkNotificationPermission()
        checkTextGo()

        binding.btnSkip.setOnClickListener {
            preferenceHelper.hidePermission()
            startActivity(DisclaimersActivity.newIntent(this))
        }

        binding.btnAllowStorage.setOnClickListener {
            requestPermissionStorage()
        }

        binding.btnAllowNotification.setOnClickListener {
            requestPermissionNotification()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionStorage = true
                binding.btnAllowStorage.isEnabled = false
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_on)
            } else {
                isPermissionStorage = false
                binding.btnAllowStorage.isEnabled = true
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_off)
            }
        } else {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionStorage = true
                binding.btnAllowStorage.isEnabled = false
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_on)
            } else {
                isPermissionStorage = false
                binding.btnAllowStorage.isEnabled = true
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_off)
            }
        }

    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            binding.llNotificationPermission.visibility = View.VISIBLE
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                isPermissionNotification = true
                binding.btnAllowNotification.isEnabled = false
                binding.btnAllowNotification.setBackgroundResource(R.drawable.ic_switch_on)
                binding.btnSkip.setText(R.string.string_continue)
            } else {
                isPermissionNotification = false
                binding.btnAllowNotification.isEnabled = true
                binding.btnAllowNotification.setBackgroundResource(R.drawable.ic_switch_off)
                binding.btnSkip.setText(R.string.string_skip)
            }
        } else {
            isPermissionNotification = true
            binding.llNotificationPermission.visibility = View.GONE
        }

    }

    private fun requestPermissionStorage() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            storageActivityResultLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            //Android is below 13(R)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_STORAGE
            )
        }
    }

    private fun requestPermissionNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                REQUEST_CODE_NOTIFICATION
            )
        }
    }

    private val storageActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!it) {
                isPermissionStorage = false
                binding.btnAllowStorage.isEnabled = true
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_off)

                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setCancelable(false)

                alertDialog.setMessage(getString(R.string.string_you_need_to_enable_permission_to_use_this_features))
                alertDialog.setButton(
                    -1,
                    getString(R.string.go_to_setting)
                ) { dialogInterface, i ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    startActivityForResult(intent, 12)

                    checkOpen = true

                    alertDialog.dismiss()
                }

                alertDialog.show()
            } else {
                binding.btnAllowStorage.isEnabled = false
                isPermissionStorage = true
                binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_on)

            }

            checkTextGo()

        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_STORAGE) {
            if (grantResults.isNotEmpty()) {
                val read = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (read && write) {
                    isPermissionStorage = true
                    binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_on)
                    binding.btnAllowStorage.isEnabled = false
                } else {
                    isPermissionStorage = false
                    binding.btnAllowStorage.setBackgroundResource(R.drawable.ic_switch_off)
                    binding.btnAllowStorage.isEnabled = true

                    val alertDialog = AlertDialog.Builder(this).create()
                    alertDialog.setCancelable(false)

                    alertDialog.setMessage(getString(R.string.string_you_need_to_enable_permission_to_use_this_features))
                    alertDialog.setButton(
                        -1,
                        getString(R.string.go_to_setting)
                    ) { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri

//                        startActivityForResult(intent, 1234)
                        startActivityResult.launch(intent)

                        checkOpen = true

                        alertDialog.dismiss()
                    }

                    alertDialog.show()
                }
            }

        } else if (requestCode == REQUEST_CODE_NOTIFICATION) {
            if (grantResults.isNotEmpty()) {
                val notificationPer = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (notificationPer) {
                    isPermissionNotification = true
                    binding.btnAllowNotification.setBackgroundResource(R.drawable.ic_switch_on)
                    binding.btnAllowNotification.isEnabled = false
                } else {
                    isPermissionNotification = false
                    binding.btnAllowNotification.setBackgroundResource(R.drawable.ic_switch_off)
                    binding.btnAllowNotification.isEnabled = true

                    val alertDialog = AlertDialog.Builder(this).create()
                    alertDialog.setCancelable(false)

                    alertDialog.setMessage(getString(R.string.string_you_need_to_enable_permission_to_use_this_features))
                    alertDialog.setButton(
                        -1,
                        getString(R.string.go_to_setting)
                    ) { _, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri

                        startActivityResult.launch(intent)
//                        startActivityForResult(intent, 1234)

                        checkOpen = true

                        alertDialog.dismiss()
                    }

                    alertDialog.show()
                }
            }

        }

        checkTextGo()
    }

    val startActivityResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        checkNotificationPermission()

        checkStoragePermission()

        checkTextGo()
    }


    override fun onResume() {
        super.onResume()

    }

    private fun checkTextGo() {
        if (!isPermissionStorage || !isPermissionNotification) {
            binding.btnSkip.text = getString(R.string.string_skip)
        } else {
            binding.btnSkip.text = getString(R.string.string_continue)
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, PermissionActivity::class.java)
        }
    }

}