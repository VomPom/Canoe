package com.voxeldev.canoe

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.voxeldev.canoe.root.integration.LinkHandler

/**
 * @author nvoxel
 */
class AndroidLinkHandler(private val context: Context) : LinkHandler {

    private var isCustomTabsSupported: Boolean? = null

    override fun openLink(url: String) {
        if (checkCustomTabsSupported()) {
            openUsingCustomTab(url = url)
        } else {
            openUsingImplicitIntent(url = url)
        }
    }

    private fun checkCustomTabsSupported(): Boolean =
        isCustomTabsSupported ?: run {
            val serviceIntent = Intent(SERVICE_ACTION)
            serviceIntent.setPackage(CHROME_PACKAGE)
            val packageName = context.packageManager.queryIntentServices(serviceIntent, 0)
            return packageName.isNotEmpty()
        }

    private fun openUsingImplicitIntent(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        )
    }

    private fun openUsingCustomTab(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        CustomTabsIntent.Builder().build()
        intent.launchUrl(context, Uri.parse(url))
    }

    private companion object {
        const val SERVICE_ACTION = "android.support.customtabs.action.CustomTabsService"
        const val CHROME_PACKAGE = "com.android.chrome"
    }
}