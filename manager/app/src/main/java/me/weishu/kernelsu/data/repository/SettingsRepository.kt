package me.weishu.kernelsu.data.repository

interface SettingsRepository {
    var uiMode: String
    var themeMode: Int
    var miuixMonet: Boolean
    var keyColor: Int
    var colorStyle: String
    var colorSpec: String
    var enablePredictiveBack: Boolean
    var enableSwipeDismiss: Boolean
    var pagerInterceptionMode: Int
    var enableBlur: Boolean
    var enableFloatingBottomBar: Boolean
    var enableFloatingBottomBarBlur: Boolean
    var enableNavigationBadge: Boolean
    var navigationRailExpanded: Boolean
    var pageScale: Float
    var moduleDescriptionMaxLines: Int
    var moduleSortEnabledFirst: Boolean
    var moduleSortActionFirst: Boolean
}
