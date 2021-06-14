package com.example.chat.chatUtil

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.chat.MyApplication
import java.io.InputStream
import java.util.*


object ImageUtil {
    private const val tag = "StorageUtil"
    private val path = "${Environment.DIRECTORY_PICTURES}/Chat/"

    fun getName(name: String = ""): String {
        if (name == "")
            return UUID.randomUUID().toString()
        return name
    }

    //通过图片name查询数据库中的图片，返回uri
    fun getUri(name: String): Uri {
        MyApplication.context.contentResolver?.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            "${MediaStore.MediaColumns.DISPLAY_NAME} like ?",
            arrayOf("$name%"),
            null
        )?.use {
            if (it.moveToFirst()) {
                LogUtil.d(tag, "查询图片时获取的图片数量：${it.count}")
                //id即为图片在文件系统中的id
                val id = it.getString(it.getColumnIndex(MediaStore.MediaColumns._ID))
                //通过id拼凑出图片uri并返回
                return Uri.parse("content://media/external/images/media/$id")
            }
        }
        return Uri.parse("")
    }

    //保存bitmap到相册(适配安卓11)
    fun saveBitmapToPicture(bitmap: Bitmap, name: String, type: String = "image/jpeg"): Uri? {
        val imageTime = System.currentTimeMillis()
        val resolver = MyApplication.context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            when (type) {
                "image/jpeg" -> put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                "image/png" -> put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            }
            put(MediaStore.MediaColumns.DATE_ADDED, imageTime / 1000)
            put(MediaStore.MediaColumns.DATE_MODIFIED, imageTime / 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, path)    //保存在本APP命名的文件夹下
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        LogUtil.d(tag, "保存到相册的URI: $uri")
        uri?.let { trueUri ->
            resolver.openOutputStream(uri).use {
                when (type) {
                    "image/jpeg" -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    "image/png" -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    else -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }
            }
            return trueUri
        }
        return null
    }

    //通过输入的uri获取bitmap
    fun getBitmapFromUri(uri: Uri): Bitmap? {
        if (uri.toString()=="")
            return null
        try {
            MyApplication.context.contentResolver.openFileDescriptor(uri, "r").use {
                return BitmapFactory.decodeFileDescriptor(it?.fileDescriptor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    //通过输入的uri获取FileDescriptor
    fun getInputStream(uri: Uri): InputStream? {
        return MyApplication.context.contentResolver.openInputStream(uri)
    }
}