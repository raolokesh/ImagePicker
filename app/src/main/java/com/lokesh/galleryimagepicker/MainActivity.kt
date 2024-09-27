package com.lokesh.galleryimagepicker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lokesh.galleryimagepicker.databinding.ActivityMainBinding
import com.lokesh.imagepicker.CameraActivity


class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private var launcher: ActivityResultLauncher<Intent>? = null

    var IMAGE_URL_LIST: String = "imageUrlList"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        launcher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                if (data != null) {
                    val resultList = data.getStringArrayListExtra(IMAGE_URL_LIST) ?: ArrayList()
                    val mutableResultList = resultList.toMutableList()
                    mainBinding.text.text = mutableResultList.toString()
                    // Handle the scanned result here
                    Log.d("MainActivity", "Scanned Result: ${mutableResultList.toString()}")
                }
            }
        }

        mainBinding.openGallery.setOnClickListener {
            CameraActivity.setImageCount(5)
            launcher!!.launch(Intent(this, CameraActivity::class.java))
        }

    }
}