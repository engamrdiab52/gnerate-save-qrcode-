package com.amrabdelhamiddiab.generatesaveqrcode

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.amrabdelhamiddiab.generatesaveqrcode.databinding.FragmentQRGeneratorBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class QRGeneratorFragment : Fragment() {
    companion object {
        const val REQUEST_CODE: Int = 1001
    }

    private lateinit var binding: FragmentQRGeneratorBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var bitmap: Bitmap
    private var permissionGranted : Boolean =false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                permissionGranted =true
                // Do if the permission is granted
                Toast.makeText(requireContext(), "permission Already granted", Toast.LENGTH_LONG)
                    .show()

            } else {
                // Do otherwise
                //   Toast.makeText(requireContext(), "permission Denied", Toast.LENGTH_LONG).show()
                showPermissionDeniedDialog()
            }
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_q_r_generator, container, false)
        askForExternalStoragePermission()

        val text = "Helloaaaaaaaaaaaaaaaa"
        //  val bitmap = BitmapFactory.decodeFile(file.getPath())
        val encoder = BarcodeEncoder()
         bitmap = encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400)

        binding.buttonSave.setOnClickListener {
            if (permissionGranted) {
                // Permissions are already granted, do your stuff
                saveImage(bitmap, requireActivity(), "Waiting App")
                Toast.makeText(requireContext(), "Image saved", Toast.LENGTH_LONG).show()

            }
        }
        binding.buttonGenerateQr.setOnClickListener {
            binding.imageView.setImageBitmap(bitmap)
        }
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun askForExternalStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R){
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }else{
            permissionGranted = true
        }

    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Permission is denied, Please allow permissions from App Settings.")
            .setPositiveButton("App Settings",
                DialogInterface.OnClickListener { _, _ ->
                    // send to app settings if permission is denied permanently
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package",requireActivity().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                })
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun saveImage(
        bitmap: Bitmap,
        context: Context,
        folderName: String
    ) {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = contentValues()
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folderName")
            values.put(MediaStore.Images.Media.IS_PENDING, true)
            // RELATIVE_PATH and IS_PENDING are introduced in API 29.

            val uri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                //here u don't access the file directly ,but through content resolver
                saveImageToStream(bitmap, context.contentResolver.openOutputStream(uri))
                values.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, values, null, null)
            }
        } else {
            val directory = File(Environment.getExternalStorageDirectory().toString() + File.separator + folderName)
            // getExternalStorageDirectory is deprecated in API 29

            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".png"
            val file = File(directory, fileName)
            saveImageToStream(bitmap, FileOutputStream(file))
            val values = contentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            // .DATA is deprecated in API 29
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
    }
    private fun contentValues() : ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        return values
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}