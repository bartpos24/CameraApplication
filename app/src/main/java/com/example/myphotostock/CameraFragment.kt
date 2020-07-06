package com.example.myphotostock

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.CameraX.unbindAll
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myphotostock.databinding.FragmentCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_image_preview.*
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue


private const val REQUEST_CODE_PERMISSIONS = 1

private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class CameraFragment : Fragment() {
    companion object {
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

    private var _binding: FragmentCameraBinding? = null
    private lateinit var imageCapture: ImageCapture
    private val binding get() = _binding!!
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var previewView: PreviewView
    private lateinit var imagePreview: Preview
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var outputDirectory: File
    var imagePath = ""
    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_camera, container, false)
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        val view = binding.root
        previewView = binding.previewView

        cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)
        if (allPermissionsGranted()) {
            previewView.post { startCamera() }
        } else {
            requestPermissions(
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        outputDirectory = getOutputDirectory(requireContext())

        binding.buttonScanner.setOnClickListener {
            Log.d("test", "ScannerButton")
            barcode_scanner()
        }

        binding.cameraCaptureButton.setOnClickListener {

            if (allPermissionsGranted()) {
                when (flashMode) {
                    ImageCapture.FLASH_MODE_OFF ->
                        cameraControl.enableTorch(false)


                    ImageCapture.FLASH_MODE_ON ->
                        cameraControl.enableTorch(true)
                }
                takePicture()
                Handler().postDelayed({activity?.let {
                    val intent = Intent(activity, ImagePreviewActivity::class.java)
                    intent.putExtra("imagePath", imagePath)
                    intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                    startActivity(intent)

                }},500)
                Log.i("FRAGMENT", imagePath)
            } else {
                requestPermissions(
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }

        }
        binding.cameraTorchButton.setOnClickListener {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF ->{
                    flashMode = ImageCapture.FLASH_MODE_ON
                    binding.cameraTorchButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_flash_on_24dp
                        )
                    )
                }
                ImageCapture.FLASH_MODE_ON ->{
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    binding.cameraTorchButton.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_flash_off_24dp
                        ))
                }else->flashMode=ImageCapture.FLASH_MODE_OFF
            }
        }

        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "CAMERA Permission granted")
                fragmentManager?.beginTransaction()?.replace(R.id.fragmentContainer, CameraFragment())?.commit()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fragmentManager?.beginTransaction()?.replace(R.id.fragmentContainer, CameraFragment())?.commit()
    }

    private fun barcode_scanner() {
        val intent = Intent(activity, BarcodeScannerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
        startActivity(intent)
    }

    private fun takePicture() {
        val file = createFile(
            outputDirectory,
            FILENAME,
            PHOTO_EXTENSION
        )

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    previewView.post {
                        //Toast.makeText(context, resources.getString(R.string.p_photo_capture), Toast.LENGTH_LONG).show()
                        Log.d("test", resources.getString(R.string.p_photo_capture)+" ${file.absolutePath}")
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    val msg = resources.getString(R.string.e_photo_capture)
                    Log.e("CameraFragment", "Photo capture failed: ${exception.message}", exception)
                    previewView.post {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        Log.d("test", resources.getString(R.string.e_photo_capture)+" ${exception.message}")
                    }
                }
            })
        imagePath = file.absolutePath

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(resources.getString(R.string.camera))
        (activity as AppCompatActivity).supportActionBar?.hide()

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


    @SuppressLint("ClickableViewAccessibility", "RestrictedApi")
    private fun startCamera() {
        CameraX.unbindAll()

        imagePreview = Preview.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(previewView.display.rotation)
        }.build()
        imageCapture = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(previewView.display.rotation)
        }.build()
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val camera =
                cameraProvider.bindToLifecycle(this, cameraSelector, imagePreview, imageCapture)
            cameraControl = camera.cameraControl
            cameraInfo = camera.cameraInfo
            //Auto-focus every X seconds
            previewView.afterMeasured {
                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    previewView.width.toFloat(), previewView.height.toFloat()
                )
                val centerWidth = previewView.width.toFloat() / 2
                val centerHeight = previewView.height.toFloat() / 2
                //create a point on the center of the view
                val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
                try {
                    camera.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                        ).apply {
                            //auto-focus every 1 seconds
                            setAutoCancelDuration(1, TimeUnit.SECONDS)
                        }.build()
                    )
                } catch (e: CameraInfoUnavailableException) {
                    Log.d("ERROR", "cannot access camera", e)
                }
            }
            //Focus on-tap
            previewView.afterMeasured {
                previewView.setOnTouchListener { _, event ->
                    return@setOnTouchListener when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                previewView.width.toFloat(), previewView.height.toFloat()
                            )
                            val autoFocusPoint = factory.createPoint(event.x, event.y)
                            try {
                                camera.cameraControl.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                        autoFocusPoint,
                                        FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                                )
                            } catch (e: CameraInfoUnavailableException) {
                                Log.d("ERROR", "cannot access camera", e)
                            }
                            true
                        }
                        else -> false // Unhandled event.
                    }
                }
            }
            previewView.preferredImplementationMode = PreviewView.ImplementationMode.TEXTURE_VIEW
            imagePreview.setSurfaceProvider(previewView.createSurfaceProvider())
        }, ContextCompat.getMainExecutor(context))
    }

    private inline fun View.afterMeasured(crossinline block: () -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    block()
                }
            }
        })
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context!!, it) == PackageManager.PERMISSION_GRANTED
    }
}



