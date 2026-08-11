package com.wooyxxng.pptnzblog.ui.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * 캡처된 공유 카드 Bitmap을 인스타그램 스토리로 공유한다.
 * 인스타그램이 설치되어 있지 않으면 갤러리에 이미지를 저장한다.
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
                type = "image/png"
                putExtra("interactive_asset_uri", uri)
                putExtra("top_background_color", BACKGROUND_COLOR)
                putExtra("bottom_background_color", BACKGROUND_COLOR)
                putExtra("source_application", context.packageName)
                setPackage(INSTAGRAM_PACKAGE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.grantUriPermission(INSTAGRAM_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } else {
            saveToGallery(context, bitmap)
        }
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap) {
        val fileName = "pptnzblog_share_${System.currentTimeMillis()}.png"
        val saved = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                uri != null
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, fileName, null) != null
            }
        } catch (e: Exception) {
            false
        }

        val message = if (saved) "이미지를 갤러리에 저장했어요" else "이미지 저장에 실패했어요"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
