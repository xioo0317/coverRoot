package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.remote.LocalApiClient
import org.json.JSONObject

/**
 * 功能页 ViewModel：与 coverRoot 后端 (local_api) 通讯。
 *
 * 功能页取代了原“模块页”，用于展示与调试后端提供的各项功能。
 */
class FeatureViewModel : ViewModel() {

    /** 一条功能项（对应后端一个 action 或调试端点） */
    data class Feature(
        val id: String,
        val title: String,
        val subtitle: String,
        val kind: Kind,
    ) {
        enum class Kind { ACTION, DEBUG }
    }

    /** 单次功能执行结果 */
    data class FeatureResult(
        val featureId: String,
        val ok: Boolean,
        val prettyJson: String,
        val summary: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    data class UiState(
        val isRefreshing: Boolean = false,
        /** 连接状态: null = 未知，true = 后端在线，false = 后端不可达 */
        val backendOnline: Boolean? = null,
        val backendVersion: String? = null,
        val apiVersion: String? = null,
        val features: List<Feature> = listOf(
            Feature("version", "版本信息", "获取后端服务版本与 API 版本", Feature.Kind.ACTION),
            Feature("detect", "环境检测", "检测 root 环境状态（KSU / APatch / Magisk / SusFS）", Feature.Kind.ACTION),
            Feature("debug", "调试信息", "后端 PID、root 状态、已注册功能列表", Feature.Kind.ACTION),
        ),
        /** 每个功能的最近一次结果（featureId -> result） */
        val results: Map<String, FeatureResult> = emptyMap(),
    )

    sealed class Effect {
        data class ShowToast(val message: String) : Effect()
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    /** 首次进入页面: 拉取版本信息以探测后端在线状态。 */
    fun initialize() {
        if (_uiState.value.backendOnline == null && !_uiState.value.isRefreshing) {
            refresh()
        }
    }

    /** 刷新: 调 version action 探测后端并更新在线状态。 */
    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            when (val r = LocalApiClient.executeAction("version")) {
                is LocalApiClient.ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            backendOnline = true,
                            backendVersion = r.json.optString("server_version"),
                            apiVersion = r.json.optString("api_version"),
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false, backendOnline = false) }
                }
            }
        }
    }

    /** 执行一个功能项。 */
    fun executeFeature(feature: Feature) {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(results = state.results + (feature.id to FeatureResult(
                    featureId = feature.id, ok = true, prettyJson = "", summary = "…"
                )))
            }
            val result = when (feature.kind) {
                Feature.Kind.ACTION -> LocalApiClient.executeAction(feature.id)
                Feature.Kind.DEBUG -> LocalApiClient.fetchDebugInfo()
            }
            val entry = when (result) {
                is LocalApiClient.ApiResult.Success -> FeatureResult(
                    featureId = feature.id,
                    ok = true,
                    prettyJson = pretty(result.json),
                    summary = result.json.optString("status", "ok"),
                )
                is LocalApiClient.ApiResult.ApiError -> FeatureResult(
                    featureId = feature.id,
                    ok = false,
                    prettyJson = result.json?.let { pretty(it) } ?: "",
                    summary = result.message,
                )
                is LocalApiClient.ApiResult.Unreachable -> FeatureResult(
                    featureId = feature.id,
                    ok = false,
                    prettyJson = "",
                    summary = result.reason,
                )
            }
            _uiState.update { state ->
                state.copy(results = state.results + (feature.id to entry))
            }
            if (!entry.ok) {
                _effect.send(Effect.ShowToast("${feature.title}: ${entry.summary}"))
            }
        }
    }

    private fun pretty(json: JSONObject): String =
        runCatching { json.toString(2) }.getOrDefault(json.toString())
}
