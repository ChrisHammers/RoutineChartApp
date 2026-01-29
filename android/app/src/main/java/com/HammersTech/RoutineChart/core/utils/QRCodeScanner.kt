package com.HammersTech.RoutineChart.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * QR Code scanner using ML Kit and CameraX
 * Phase 2.2: QR Family Joining
 */
class QRCodeScanner(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private var cameraExecutor: ExecutorService? = null
    private val barcodeScanner = BarcodeScanning.getClient()

    var onCodeScanned: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request camera permission
     * Note: This must be called from an Activity
     */
    fun requestCameraPermission(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit,
    ) {
        val launcher =
            activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                onResult(isGranted)
            }
        launcher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Start scanning for QR codes
     * @param previewView The PreviewView to display the camera preview
     */
    fun startScanning(previewView: PreviewView) {
        if (!hasCameraPermission()) {
            onError?.invoke("Camera permission not granted")
            return
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(cameraProvider, previewView)
            } catch (e: Exception) {
                AppLogger.UI.error("Failed to start camera", e)
                onError?.invoke("Failed to start camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Stop scanning
     */
    fun stopScanning() {
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    private fun bindCameraUseCases(
        cameraProvider: ProcessCameraProvider,
        previewView: PreviewView,
    ) {
        val preview =
            Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

        val imageAnalysis =
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image =
                                InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees,
                                )

                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.format == Barcode.FORMAT_QR_CODE) {
                                            barcode.rawValue?.let { value ->
                                                // Stop scanning after first successful scan
                                                stopScanning()
                                                onCodeScanned?.invoke(value)
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    AppLogger.UI.error("Failed to process barcode", e)
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis,
            )
        } catch (e: Exception) {
            AppLogger.UI.error("Failed to bind camera", e)
            onError?.invoke("Failed to bind camera: ${e.message}")
        }
    }
}
