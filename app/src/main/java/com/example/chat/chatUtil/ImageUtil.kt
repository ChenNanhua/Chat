package com.example.chat.chatUtil

import android.content.ContentUris
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.chat.MyApplication
import com.example.chat.R
import com.example.chat.chatUtil.TinyUtil.loge
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
        try {
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
        }catch (e:java.lang.Exception){
            e.printStackTrace()
            return null
        }
        return null
    }

    //通过输入的uri获取bitmap
    fun getBitmapFromUri(uri: Uri): Bitmap? {
        try {
            MyApplication.context.contentResolver.openFileDescriptor(uri, "r").use {
                return BitmapFactory.decodeFileDescriptor(it?.fileDescriptor)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    //R.drawable转bitmap
    fun getBitmapFromResource(resource: Int = R.drawable.none): Bitmap {
        return BitmapFactory.decodeResource(MyApplication.context.resources, resource)
    }

    //通过输入的uri获取FileDescriptor
    fun getInputStream(uri: Uri): InputStream? {
        return MyApplication.context.contentResolver.openInputStream(uri)
    }

    /*
    通过ID获取MediaStore.Images.Media.EXTERNAL_CONTENT_URI下的图片路径
    Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
    调用系统图册获取的Uri content://com.android.providers.media.documents/document/image%3A65768
    得到的Uri:  content://media/external/images/media/65768
    */
    fun getExternalUriFromUri(uri: Uri): Uri {
        with(uri.toString().split("%3A")) {
            return ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, this[this.size - 1].toLong()
            )
        }
    }
}