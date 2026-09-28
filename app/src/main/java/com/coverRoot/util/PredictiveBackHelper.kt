package com.coverRoot.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass

fun setPredictiveBackEnabled(context: Context, enabled: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
    runCatching {
        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback"
        )
        val appInfo = context.applicationInfo
        val method = ApplicationInfo::class.java.getDeclaredMethod(
            "setEnableOnBackInvokedCallback",
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        method.invoke(appInfo, enabled)
    }
}
