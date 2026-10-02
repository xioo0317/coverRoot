package me.weishu.kernelsu.data.repository

interface SettingsRepository {
    var uiMode: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enablePredictiveBack: Boolean
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var moduleSortEnabledFirst: Boolean
    var moduleSortActionFirst: Boolean
    var checkUpdate: Boolean
    var appLanguage: String
}
