package me.weishu.kernelsu.ui.viewmodel

import android.os.Build
import android.system.Os
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.getKernelVersion
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.screen.home.HomeUiState
import me.weishu.kernelsu.ui.screen.home.SystemInfo
import me.weishu.kernelsu.ui.screen.home.getManagerVersion
import me.weishu.kernelsu.ui.util.checkNewVersion
import me.weishu.kernelsu.ui.util.module.LatestVersionInfo
import me.weishu.kernelsu.ui.util.resolveDeviceName
import java.io.File

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(buildState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val baseState = withContext(Dispatchers.IO) { buildState() }
            _uiState.update { baseState }
            if (baseState.checkUpdateEnabled) {
                val latestVersionInfo = withContext(Dispatchers.IO) { checkNewVersion() }
                _uiState.update { it.copy(latestVersionInfo = latestVersionInfo) }
            }
        }
    }

    private fun buildState(): HomeUiState {
        val kernelVersion = getKernelVersion()
        val managerVersion = getManagerVersion(ksuApp)
        val uapiVersion = 4

        return HomeUiState(
            kernelVersion = kernelVersion,
            ksuVersion = 1,
            lkmMode = null,
            isLkmBundled = false,
            isManager = true,
            isManagerPrBuild = BuildConfig.IS_PR_BUILD,
            isKernelPrBuild = false,
            requiresNewKernel = false,
            requiresNewManager = false,
            kernelUAPIVersion = uapiVersion,
            managerUAPIVersion = uapiVersion,
            isRootAvailable = true,
            isSafeMode = false,
            isLateLoadMode = false,
            checkUpdateEnabled = SettingsRepositoryImpl().checkUpdate,
            latestVersionInfo = LatestVersionInfo(),
            currentManagerVersionCode = managerVersion.versionCode,
            systemInfo = SystemInfo(
                kernelVersion = Os.uname().release,
                managerVersion = "${managerVersion.versionName} (${managerVersion.versionCode}-${uapiVersion})",
                deviceModel = resolveDeviceName(),
                fingerprint = Build.FINGERPRINT,
                selinuxStatus = getSELinuxStatusRaw(),
                seccompStatus = runCatching {
                    Os.prctl(21 /* PR_GET_SECCOMP */, 0, 0, 0, 0)
                }.getOrDefault(-1),
            ),
        )
    }

    private fun getSELinuxStatusRaw(): String {
        if (!File("/sys/fs/selinux").exists()) return "Disabled"
        return try {
            if (File("/sys/fs/selinux/enforce").readText().trim() == "1") "Enforcing" else "Permissive"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
