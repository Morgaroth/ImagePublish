package io.github.morgaroth.android.imagepublish

import android.app.Activity
import android.content.Intent
import android.content.Intent.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import com.drew.imaging.ImageMetadataReader
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import android.os.Environment.getExternalStorageDirectory


class ShareActivity : Activity() {
    private val TAG = "Share"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (ACTION_SEND == action && type.startsWith("image/")) handleSendImage(intent)
        else if (ACTION_SEND_MULTIPLE == action && type.startsWith("image/")) handleSendMultipleImages(intent)
        else {
            Log.w(TAG, "not recognized action $type $action $intent")
        }
    }

    private fun handleSendImage(intent: Intent) {
        Log.i(TAG, "Handling image")
        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_STREAM)
        if (imageUri != null) {
            Log.i(TAG, imageUri.path)
//            listExif(imageUri)
            val file = rewriteFile(imageUri)
            publishFile(file)
        }
    }

    private fun publishFile(f: File) {
        val share = Intent(ACTION_SEND)
        share.putExtra(EXTRA_STREAM, Uri.fromFile(f))
        share.type = "image/jpeg"
        startActivity(Intent.createChooser(share, "Share Exifless Image"))
    }

    private fun listExif(tmpFile: Uri) {
        val metadata = ImageMetadataReader.readMetadata(contentResolver.openInputStream(tmpFile))
        val tags = metadata.directories.flatMap { it.tags }
        tags.forEach { Log.i(TAG, "(${it.tagType}) ${it.tagName}: ${it.description}") }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        Log.i(TAG, "Handling multiple images")
        val imageUris = intent.getParcelableArrayListExtra<Uri>(EXTRA_STREAM)
        if (imageUris != null) {
        }
    }

    fun rewriteFile(sourceuri: Uri): File {
        if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()) {
            Log.w(TAG, "illegal external storage")
        }
        val inputStream = contentResolver.openInputStream(sourceuri)
        val pngDir = object : File(Environment.getExternalStorageDirectory(), "Android/data/io.github.morgaroth.android.imagepublish/images") {}
        if (!pngDir.exists()) Log.i(TAG, "creating directories end with " + pngDir.mkdirs())
        val outputFile = object : File(pngDir, sourceuri.lastPathSegment + ".jpg") {}
        Log.i(TAG, "output ${outputFile.absolutePath}")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(outputFile, false))
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            bos.flush()
            return outputFile
        } finally {
            bos?.close()
        }
    }
}
