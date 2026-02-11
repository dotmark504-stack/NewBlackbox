package top.niunaijun.blackboxa.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.github.nukc.stateview.StateView
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator
import kotlinx.coroutines.launch
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.app.AppManager
import top.niunaijun.blackboxa.ui.theme.BlackBoxExpressiveTheme
import top.niunaijun.blackboxa.view.apps.AppsFragment
import top.niunaijun.blackboxa.view.base.LoadingActivity
import top.niunaijun.blackboxa.view.fake.FakeManagerActivity
import top.niunaijun.blackboxa.view.list.ListActivity
import top.niunaijun.blackboxa.view.setting.SettingActivity

class MainActivity : LoadingActivity() {

    private lateinit var mViewPagerAdapter: ViewPagerAdapter
    private lateinit var composeViewPager: ViewPager2
    private lateinit var composeDotsIndicator: WormDotsIndicator

    private val fragmentList = mutableListOf<AppsFragment>()
    private var currentUser = 0
    private var isFloatButtonVisible by mutableStateOf(true)

    companion object {
        private const val TAG = "MainActivity"
        private const val STORAGE_PERMISSION_REQUEST_CODE = 1001

        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            BlackBoxCore.get().onBeforeMainActivityOnCreate(this)

            initViewPagerData()
            setContent {
                BlackBoxExpressiveTheme {
                    MainScreen(
                            getRemark = { AppManager.mRemarkSharedPreferences.getString("Remark$currentUser", "User $currentUser") ?: "User $currentUser" },
                            onEditRemark = ::showRemarkDialog,
                            onMenuAction = ::handleMenuAction,
                            onInstallClicked = ::openInstallList,
                    )
                }
            }

            checkStoragePermission()
            checkVpnPermission()
            BlackBoxCore.get().onAfterMainActivityOnCreate(this)
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in onCreate: ${e.message}")
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen(
            getRemark: () -> String,
            onEditRemark: () -> Unit,
            onMenuAction: (MainMenuAction) -> Unit,
            onInstallClicked: () -> Unit,
    ) {
        val snackBarState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        var menuExpanded by remember { mutableStateOf(false) }

        Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    TopAppBar(
                            title = {
                                Column {
                                    Text(text = getString(R.string.app_name))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                                text = getRemark(),
                                                style = MaterialTheme.typography.bodySmall,
                                        )
                                        IconButton(onClick = onEditRemark) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit user remark")
                                        }
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                    DropdownMenuItem(text = { Text("GitHub") }, onClick = {
                                        menuExpanded = false
                                        onMenuAction(MainMenuAction.OpenGitHub)
                                    })
                                    DropdownMenuItem(text = { Text(getString(R.string.setting)) }, onClick = {
                                        menuExpanded = false
                                        onMenuAction(MainMenuAction.OpenSettings)
                                    })
                                    DropdownMenuItem(text = { Text("Telegram") }, onClick = {
                                        menuExpanded = false
                                        onMenuAction(MainMenuAction.OpenTelegram)
                                    })
                                    DropdownMenuItem(text = { Text(getString(R.string.fake_location)) }, onClick = {
                                        menuExpanded = false
                                        onMenuAction(MainMenuAction.OpenFakeLocation)
                                    })
                                }
                            },
                    )
                },
                floatingActionButton = {
                    if (isFloatButtonVisible) {
                        FloatingActionButton(onClick = {
                            onInstallClicked()
                            scope.launch {
                                snackBarState.showSnackbar(getString(R.string.add_app))
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = getString(R.string.add_app))
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackBarState) },
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AndroidView(
                            factory = { context ->
                                StateView(context).apply {
                                    id = R.id.stateView
                                }
                            },
                    )
                    AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                composeViewPager = ViewPager2(context).apply {
                                    id = R.id.viewPager
                                    adapter = mViewPagerAdapter
                                    registerOnPageChangeCallback(
                                            object : ViewPager2.OnPageChangeCallback() {
                                                override fun onPageSelected(position: Int) {
                                                    super.onPageSelected(position)
                                                    currentUser = fragmentList[position].userID
                                                }
                                            },
                                    )
                                }
                                composeViewPager
                            },
                    )
                }
                AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        factory = { context ->
                            composeDotsIndicator = WormDotsIndicator(context)
                            composeDotsIndicator.setViewPager2(composeViewPager)
                            composeDotsIndicator
                        },
                )
            }
        }

    }

    private fun initViewPagerData() {
        val userList = BlackBoxCore.get().users
        fragmentList.clear()
        userList.forEach { fragmentList.add(AppsFragment.newInstance(it.id)) }
        currentUser = userList.firstOrNull()?.id ?: 0
        fragmentList.add(AppsFragment.newInstance(userList.size))
        mViewPagerAdapter = ViewPagerAdapter(this)
        mViewPagerAdapter.replaceData(fragmentList)
    }

    private fun showRemarkDialog() {
        MaterialDialog(this).show {
            title(res = R.string.userRemark)
            input(
                    hintRes = R.string.userRemark,
                    prefill = AppManager.mRemarkSharedPreferences.getString("Remark$currentUser", "User $currentUser"),
            ) { _, input ->
                AppManager.mRemarkSharedPreferences.edit {
                    putString("Remark$currentUser", input.toString())
                }
            }
            positiveButton(res = R.string.done)
            negativeButton(res = R.string.cancel)
        }
    }

    private fun openInstallList() {
        val userId = composeViewPager.currentItem
        val intent = Intent(this, ListActivity::class.java)
        intent.putExtra("userID", userId)
        apkPathResult.launch(intent)
    }

    private fun handleMenuAction(action: MainMenuAction) {
        when (action) {
            MainMenuAction.OpenGitHub -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ALEX5402/NewBlackbox")))
            MainMenuAction.OpenSettings -> SettingActivity.start(this)
            MainMenuAction.OpenTelegram -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/newblackboxa")))
            MainMenuAction.OpenFakeLocation -> {
                val intent = Intent(this, FakeManagerActivity::class.java)
                intent.putExtra("userID", 0)
                startActivity(intent)
            }
        }
    }

    fun showFloatButton(show: Boolean) {
        if (isFloatButtonVisible == show) return
        runOnUiThread {
            isFloatButtonVisible = show
        }
    }

    private enum class MainMenuAction {
        OpenGitHub,
        OpenSettings,
        OpenTelegram,
        OpenFakeLocation,
    }

    private fun checkStoragePermission() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                if (!android.os.Environment.isExternalStorageManager()) {
                    Log.w(TAG, "MANAGE_EXTERNAL_STORAGE permission not granted")
                    showStoragePermissionDialog()
                }
            } else {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                this,
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                        this,
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    Log.w(
                            TAG,
                            "Storage permissions not granted on Android ${android.os.Build.VERSION.SDK_INT}"
                    )
                    requestLegacyStoragePermission()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking storage permission: ${e.message}")
        }
    }

    private fun requestLegacyStoragePermission() {
        try {
            androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    STORAGE_PERMISSION_REQUEST_CODE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting storage permission: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                            grantResults.all {
                                it == android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
            ) {
                Log.d(TAG, "Storage permissions granted")
            } else {
                Log.w(TAG, "Storage permissions denied")
            }
        }
    }

    private fun showStoragePermissionDialog() {
        try {
            MaterialDialog(this).show {
                title(text = "Storage Permission Required")
                message(
                        text =
                                "This app needs 'All Files Access' permission to properly run sandboxed apps. Without this permission, some apps may not work correctly.\n\nPlease grant permission in the next screen."
                )
                positiveButton(text = "Grant Permission") { openAllFilesAccessSettings() }
                negativeButton(text = "Later") { Log.w(TAG, "User postponed storage permission") }
                cancelable(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing storage permission dialog: ${e.message}")
        }
    }

    private fun openAllFilesAccessSettings() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intent =
                        Intent(
                                android.provider.Settings
                                        .ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                        )
                intent.data = Uri.parse("package:$packageName")
                storagePermissionResult.launch(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening storage settings: ${e.message}")
            try {
                val intent =
                        Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionResult.launch(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening fallback storage settings: ${e2.message}")
            }
        }
    }

    private val storagePermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        if (android.os.Environment.isExternalStorageManager()) {
                            Log.d(TAG, "Storage permission granted!")
                        } else {
                            Log.w(TAG, "Storage permission still not granted")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling storage permission result: ${e.message}")
                }
            }

    private fun checkVpnPermission() {
        try {
            val vpnIntent = VpnService.prepare(this)
            if (vpnIntent != null) {
                Log.d(TAG, "VPN permission not granted, requesting...")
                vpnPermissionResult.launch(vpnIntent)
            } else {
                Log.d(TAG, "VPN permission already granted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking VPN permission: ${e.message}")
        }
    }

    private val vpnPermissionResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                try {
                    if (result.resultCode == RESULT_OK) {
                        Log.d(TAG, "VPN permission granted!")
                    } else {
                        Log.w(TAG, "VPN permission denied by user")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling VPN permission result: ${e.message}")
                }
            }

    private fun showErrorDialog(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "Error")
                message(text = message)
                positiveButton(text = "OK") { finish() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing error dialog: ${e.message}")
            finish()
        }
    }

    fun scanUser() {
        try {
            val userList = BlackBoxCore.get().users
            if (fragmentList.size == userList.size) {
                fragmentList.add(AppsFragment.newInstance(fragmentList.size))
            } else if (fragmentList.size > userList.size + 1) {
                fragmentList.removeLast()
            }
            mViewPagerAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error in scanUser: ${e.message}")
        }
    }

    private val apkPathResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                try {
                    if (it.resultCode == RESULT_OK) {
                        it.data?.let { data ->
                            val userId = data.getIntExtra("userID", 0)
                            val source = data.getStringExtra("source")
                            if (source != null) {
                                fragmentList[userId].installApk(source)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling APK path result: ${e.message}")
                }
            }
}
