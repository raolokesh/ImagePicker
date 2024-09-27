package com.lokesh.imagepicker

import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.lokesh.imagepicker.adapter.ImageAdapter
import com.lokesh.imagepicker.databinding.ActivityCameraBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {
    private lateinit var cameraBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService


    private lateinit var adapter: ImageAdapter
    private lateinit var imageUrls: MutableList<String>


    var IMAGE_URL_LIST: String = "imageUrlList"

    // flash state defined used during image capture
    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // gallery used
    private val imageUrlsGallery = ArrayList<String>()

    companion object {
        private var imageCount: Int = 0
        fun setImageCount(i: Int) {
            this.imageCount = i
        }

        private const val TAG = "CameraApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            android.Manifest.permission.CAMERA,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }.toTypedArray()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraBinding = ActivityCameraBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(cameraBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageUrls = mutableListOf<String>()
        // Request camera permissions
        if (allPermissionsGranted()) {
            openCamera()
            loadImagesFromGallery()
        } else {
            requestPermissions()
        }

        // Button clicked listener to take picture
        cameraBinding.imageClick.setOnClickListener { takephoto() }
        cameraBinding.cancelButton.setOnClickListener { finish() }
        cameraBinding.flashButton.setOnClickListener { flashLight() }
        cameraBinding.flipButton.setOnClickListener { flipCamera() }
        cameraBinding.galleryButton.setOnClickListener { openGallery() }

        cameraBinding.sendButton.setOnClickListener { sendResult() }

        // Recycler view layout for image clicked
        cameraBinding.imageClicked.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = ImageAdapter(this, imageUrls)
        cameraBinding.imageClicked.adapter = adapter


        // camera Executor is used to bind the lifecycle of cameras to the lifecycle owner
        cameraExecutor = Executors.newSingleThreadExecutor()


    }

    private fun sendResult() {
        val resultIntent = Intent()
        resultIntent.putStringArrayListExtra(IMAGE_URL_LIST, java.util.ArrayList(imageUrls))
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun flipCamera() {
        when (cameraSelector) {
            CameraSelector.DEFAULT_BACK_CAMERA -> {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                openCamera()
            }

            CameraSelector.DEFAULT_FRONT_CAMERA -> {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                openCamera()
            }
        }

    }


    private fun flashLight() {
        when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                flashMode = ImageCapture.FLASH_MODE_ON
                imageCapture?.flashMode = flashMode
                cameraBinding.flashButton.setBackgroundResource(R.drawable.baseline_flash_on_24)
            }

            ImageCapture.FLASH_MODE_ON -> {
                flashMode = ImageCapture.FLASH_MODE_AUTO
                imageCapture?.flashMode = flashMode
                cameraBinding.flashButton.setBackgroundResource(R.drawable.baseline_flash_auto_24)
            }

            ImageCapture.FLASH_MODE_AUTO -> {
                flashMode = ImageCapture.FLASH_MODE_OFF
                imageCapture?.flashMode = flashMode
                cameraBinding.flashButton.setBackgroundResource(R.drawable.baseline_flash_off_24)
            }
        }

    }

    // request permissions for camera and storage in the companion object REQUIRED_PERMISSIONS

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    // activity result launch for permissions to camera and storage
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                baseContext, "Permission request denied", Toast.LENGTH_SHORT
            ).show()
        } else {
            openCamera()
        }
    }

    // when image click button is clicked this method is called
    private fun takephoto() {

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        if (imageUrls.size <= imageCount) {
            // Create time stamped name and MediaStore entry.
            val name = SimpleDateFormat(
                FILENAME_FORMAT,
                Locale.getDefault()
            ).format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
                }
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                    contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
                ).build()

            // Set up image capture listener, which is triggered after photo has
            // been taken
            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        imageUrls.add(output.savedUri.toString())
                        adapter.notifyDataSetChanged()
                        val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    }
                })
        } else {
            Toast.makeText(this, "You can select up to $imageCount images", Toast.LENGTH_SHORT)
                .show()
        }


    }


    // After get permission open camera this method is called & image Capture listener is setup
    private fun openCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraBinding.viewFinder.surfaceProvider)
                }

            // here is image capture listener is setup for taking photo
            imageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e("CameraActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }

    // load image from gallery and added to imageUrlsGallery
    private fun loadImagesFromGallery() {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val selection = "${MediaStore.Images.Media.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("image/jpeg") // You can include more MIME types as needed
        val sortOrder =
            "${MediaStore.Images.Media.DATE_TAKEN} DESC" // Sort by date taken, latest first

        val cursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val imagePath = it.getString(columnIndex)
                imageUrlsGallery.add(imagePath)
            }
        }
    }


    private fun openGallery() {
        if (imageUrlsGallery.isNotEmpty()) {
            val maxSelection = imageCount - imageUrls.size // Set your maximum selection limit here
            val galleryFragment =
                GalleryBottomSheetFragment(imageUrlsGallery, maxSelection) { selectedImages ->
                    // Handle the selected images here
                    // For example, you can store them or use them in your activity
                    imageUrls.clear()
                    imageUrls.addAll(selectedImages)
                    adapter.notifyDataSetChanged()
//                    updateImageUrls(selectedImages)

                }
            galleryFragment.show(supportFragmentManager, galleryFragment.tag)
        } else {
            Toast.makeText(this, "No images found", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateImageUrls(selectedImages: List<String>) {
        // Create a set of existing URLs for quick lookup
        val existingUrls = imageUrls.toSet()

        // Add only new items from selectedImages to imageUrls
        for (image in selectedImages) {
            if (image !in existingUrls) {
                imageUrls.add(image)
            }
        }
        adapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }


}