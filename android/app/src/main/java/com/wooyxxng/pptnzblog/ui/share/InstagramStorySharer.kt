package com.wooyxxng.pptnzblog.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 캡처된 공유 카드 Bitmap을 인스타그램 스토리로 공유한다.
 * 인스타그램이 설치되어 있지 않으면 표준 공유 시트로 폴백한다.
 * iOS의 `InstagramStorySharer`(UIPasteboard 기반)에 대응하는 Android 방식:
 * FileProvider로 content:// URI를 만들어 `com.instagram.share.ADD_TO_STORY` 인텐트에 전달한다.
 */
object InstagramStorySharer {
    private const val BACKGROUND_COLOR = "#FFFFFA"
    private const val INSTAGRAM_PACKAGE = "com.instagram.android"

    fun share(context: Context, bitmap: Bitmap) {
        val uri = saveToCache(context, bitmap) ?: return

        if (isInstagramInstalled(context)) {
            val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "image/png")
                putExtra("interactive_asset_uri", uri)
                putExtra("top_background_color", BACKGROUND_COLOR)
                putExtra("bottom_background_color", BACKGROUND_COLOR)
                setPackage(INSTAGRAM_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(INSTAGRAM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } else {
            shareFallback(context, uri)
        }
    }

    private fun shareFallback(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun isInstagramInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(INSTAGRAM_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun saveToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val file = File(shareDir, "share_card_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
