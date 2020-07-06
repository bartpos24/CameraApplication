package com.example.myphotostock

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_image_preview.*
import java.io.File


class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var listOfAlbums: MutableList<PhotoAlbum>
    internal lateinit var auth: FirebaseAuth
    internal lateinit var refDatabase: DatabaseReference
    private lateinit var refDbPhotoAlbum: DatabaseReference
    private lateinit var refDbPhotoListAlbum: DatabaseReference
    private lateinit var mStorageRef: StorageReference
    private lateinit var userIDglobal: String
    private lateinit var imagePathGlobal: String
    private lateinit var thisActivity: Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        thisActivity = this

        setContentView(R.layout.activity_image_preview)
        supportActionBar?.hide()
        //absolute path to the photo
        val imagePath=intent.getStringExtra("imagePath")
        imagePathGlobal = imagePath

        //Auth
        auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid
        userID?.let {
            userIDglobal = it
        }

        //Database
        val firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        refDatabase = firebase.getReference("$userID")

        refDbPhotoAlbum = this.refDatabase.child("photoAlbum")
        refDbPhotoListAlbum = this.refDatabase.child("listOfPhotos")

        refDbPhotoAlbum.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(databaseSnapshot: DataSnapshot) {
                listOfAlbums = mutableListOf()

                for(i in databaseSnapshot.children){
                    val title: String = i.getValue(String::class.java) ?: ""
                    val newRow = PhotoAlbum(title, i.ref.key.toString())
                    listOfAlbums.add(newRow)
                }

            }

        })

        auth.addAuthStateListener {
            if(auth.currentUser == null){
                this.finish()
            }
        }

        if ( isStoragePermissionGranted()){
            try {
                //val inputStream = Uri.fromFile(file)
              //val bitmap=BitmapFactory.decodeStream(contentResolver.openInputStream(inputStream))
                Handler().postDelayed({val bitmap=BitmapFactory.decodeFile(imagePath)
                    val rotatedImage = rotatebitmap(bitmap)
                    image_preview.setImageBitmap(rotatedImage)},2000)

            }catch (e:Exception){
                Log.e("ImagePreviewActivity",e.toString())
            }


        }

        button_delete.setOnClickListener {
            val image=File(imagePath)
            image.delete()
            finish()
        }
        button_accept.setOnClickListener{
            Log.d("test", "SaveButtonClicked")
            showAlertChooseAlbum()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 2) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "READ_EXTERNAL_STORAGE Permission granted")
                recreate()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun sendPhotoToCloud(choosenAlbum: Int) {
        Log.d("test", "StartFunction")

        //File
        val file = Uri.fromFile(File("$imagePathGlobal"))
        Log.d("test", "FileCreated")

        //Reference
        mStorageRef = FirebaseStorage.getInstance().reference
        val mStorageRefUser: StorageReference? = mStorageRef.child("$userIDglobal")
        val mStorageRefUserImages = mStorageRefUser!!.child("images")

        val riversRef = mStorageRefUserImages.child("${file.lastPathSegment}")

        Log.d("test", "References created")

        Log.d("test", "StartUploadTask")
        // Upload Task
        val uploadTask = riversRef.putFile(file)


        val urlTask = uploadTask.continueWithTask { task ->
            Log.d("test", "StartContinueWithTask")
            if (!task.isSuccessful) {
                Log.d("test", "TaskNotSiccessful")
                task.exception?.let {
                    Log.d("test", "Exception")
                    throw it
                }
            }
            riversRef.downloadUrl
        }.addOnCompleteListener { task ->
            Log.d("test", "OnCompleteListener")
            if (task.isSuccessful) {
                Log.d("test", "TaskIsSuccessful")
                val downloadUrl = task.result
                downloadUrl?.let { addPhotoToDatabase(choosenAlbum, it) }
            } else {
                Log.d("test", "TaskIsNotSuccessful")
                // Handle failures
                // ...
            }
        }

        Log.d("test", "End")
    }

    private fun addPhotoToDatabase(choosenAlbum: Int, downloadUrl: Uri) {
        val firebaseInput = Photo(downloadUrl.toString(), listOfAlbums[choosenAlbum].albumId, Uri.fromFile(File("$imagePathGlobal")).lastPathSegment.dropLast(4))
        refDbPhotoListAlbum.child(firebaseInput.albumId).child(firebaseInput.photoName).setValue(firebaseInput.urlToFile)
    }

    private fun showAlertChooseAlbum() {
        val singleItems = listOfAlbums.map { it.title }.toTypedArray()
        var choosen: Int
        if (singleItems.isEmpty())
        {
            MaterialAlertDialogBuilder(this)
                .setMessage(resources.getString(R.string.no_album_created))
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->

                }
                .show()
        } else {
            val checkedItem = 0
                choosen = 0

            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.Choose_album))
                .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                    choosen = -1
                }
                .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                    if (choosen > -1) {
                        Log.d("test", "Start save operations")
                        sendPhotoToCloud(choosen)
                        File(imagePathGlobal).delete()
                        finish()
                    }
                }
                // Single-choice items (initialized with checked item)
                .setSingleChoiceItems(singleItems, checkedItem) { dialog, which ->
                    Log.d("test", "Choosen album no. "+which.toString())
                    choosen = which
                }
                .show()
        }

    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    2
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            true
        }
    }

    private fun rotatebitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        val rot: Float
        val screenOrientation = (this.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation
        when(screenOrientation) {
            Surface.ROTATION_0 -> rot = 90.0f
            Surface.ROTATION_90 -> rot = 360.0f
            Surface.ROTATION_180 -> rot = 0.0f
            else -> rot = 180.0f
        }
        
        matrix.postRotate(rot)

        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
        return rotatedBitmap
    }


}