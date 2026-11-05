package com.moses.batmantv

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        web.setBackgroundColor(Color.BLACK)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = WebViewClient()
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    override fun onBackPressed() {}

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        web.evaluateJavascript("window.goHome&&goHome()", null)
    }

    private fun iconB64(d: Drawable): String {
        val bmp = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, 96, 96)
        d.draw(c)
        val o = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, o)
        return "data:image/png;base64," + Base64.encodeToString(o.toByteArray(), Base64.NO_WRAP)
    }

    inner class Bridge {
        @JavascriptInterface
        fun launch(pkg: String) {
            val pm = packageManager
            val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
        }

        @JavascriptInterface
        fun openUrl(u: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
            }
        }

        @JavascriptInterface
        fun getSys(): String {
            val mi = ActivityManager.MemoryInfo()
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
            val sf = StatFs(Environment.getDataDirectory().path)
            return JSONObject()
                .put("rt", mi.totalMem).put("ra", mi.availMem)
                .put("st", sf.totalBytes).put("sf", sf.availableBytes).toString()
        }

        @JavascriptInterface
        fun requestApps() {
            Thread {
                val pm = packageManager
                val seen = HashSet<String>()
                val arr = JSONArray()
                for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
                    val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
                    for (ri in pm.queryIntentActivities(q, 0)) {
                        val p = ri.activityInfo.packageName
                        if (p == packageName || !seen.add(p)) continue
                        try {
                            arr.put(JSONObject().put("n", ri.loadLabel(pm).toString()).put("p", p).put("i", iconB64(ri.loadIcon(pm))))
                        } catch (e: Exception) {
                        }
                    }
                }
                runOnUiThread {
                    web.evaluateJavascript("window.onApps(" + JSONObject.quote(arr.toString()) + ")", null)
                }
            }.start()
        }
    }
}
