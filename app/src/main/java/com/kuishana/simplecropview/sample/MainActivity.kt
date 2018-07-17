package com.kuishana.simplecropview.sample

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

const val REQUEST_CODE_PICK_IMG = 1
const val REQUEST_CODE_CROP_IMG = 2

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        changeHeadImage.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(intent, REQUEST_CODE_PICK_IMG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_PICK_IMG) {
                data?.let {
                    val intent = Intent(this, CropActivity::class.java)
                    intent.data = it.data
                    startActivityForResult(intent, REQUEST_CODE_CROP_IMG)
                }
            } else if (requestCode == REQUEST_CODE_CROP_IMG) {
                data?.let {
                    headImage.setImageURI(null)
                    headImage.setImageURI(it.data)
                }
            }
        }
    }
}
