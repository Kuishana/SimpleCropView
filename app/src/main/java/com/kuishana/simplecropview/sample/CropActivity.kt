package com.kuishana.simplecropview.sample

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_crop.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class CropActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        back.setOnClickListener { finish() }
        save.setOnClickListener {
            val bitmap = simpleCropView.crop()
            bitmap?.let {
                try {
                    val file = File(cacheDir, "temp_file_crop_image")
                    val fileOutputStream = FileOutputStream(file)
                    it.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)//对文件大小有要求可在此压缩
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    val intent = Intent()
                    intent.data = Uri.fromFile(file)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        intent?.let {
            val uri = it.data
            uri?.let {
                try {
                    val parcelFileDescriptor = contentResolver.openFileDescriptor(it, "r")
                    parcelFileDescriptor?.let {
                        //post，确保simpleCropView大小已确定
                        simpleCropView.post {
                            simpleCropView.setBitmapRegionDecoder(BitmapRegionDecoder.newInstance(it.fileDescriptor, false))
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
