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
            simpleCropView.crop()?.let {
                try {
                    val file = File(cacheDir, "temp_file_crop_image")
                    FileOutputStream(file).run {
                        it.compress(Bitmap.CompressFormat.JPEG, 100, this)//对文件大小有要求可在此压缩
                        flush()
                        close()
                    }
                    Intent().run {
                        data = Uri.fromFile(file)
                        setResult(Activity.RESULT_OK, this)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            finish()
        }

        intent?.run {
            data?.let {
                try {
                    contentResolver.openFileDescriptor(it, "r").run {
                        //post，确保simpleCropView大小已确定
                        simpleCropView.post {
                            try {
                                simpleCropView.setBitmapRegionDecoder(BitmapRegionDecoder.newInstance(fileDescriptor, false))
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
