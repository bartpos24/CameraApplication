package com.example.myphotostock

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_photo_from_gallery_preview.*
import kotlinx.android.synthetic.main.cell_photo.view.*
import java.io.File
import java.lang.Exception

class PhotoFromGalleryPreviewActivity : AppCompatActivity() {

    internal lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    internal lateinit var refDatabase: DatabaseReference
    private lateinit var refDbListOfPhotos: DatabaseReference

    private lateinit var mStorageRef: StorageReference

    private lateinit var photo: Photo

    private var isActionBarVisible: Boolean = true

    private var thisMainActivity = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_from_gallery_preview)
        //supportActionBar?.hide()
        supportActionBar?.show()
        isActionBarVisible = true
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("")

        //Auth
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()

        //Database
        val firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        refDatabase = firebase.getReference("$userID")

        mStorageRef = FirebaseStorage.getInstance().reference

        auth.addAuthStateListener {
            if(auth.currentUser == null){
                this.finish()
            }
        }

        refDbListOfPhotos = refDatabase.child("listOfPhotos")

        IV_previewSinglePhotoGallery.setOnClickListener {
            if (isActionBarVisible) {
                supportActionBar?.hide()
                isActionBarVisible = false
            } else {
                supportActionBar?.show()
                isActionBarVisible = true
            }
        }

        photo = Photo(intent.getStringExtra("urlP"), intent.getStringExtra("idAlbumP"), intent.getStringExtra("nameP"))

        //Picasso.get().load(photo.urlToFile).error(R.drawable.ic_baseline_block_24).placeholder(R.drawable.ic_baseline_cloud_download_24).into(IV_previewSinglePhotoGallery)

        Glide.with(thisMainActivity)
            .load(photo.urlToFile)
            .apply(RequestOptions().placeholder(R.drawable.ic_baseline_cloud_download_24).error(R.drawable.ic_baseline_block_24))
            .into(IV_previewSinglePhotoGallery)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.top_menu_photo_gallery_preview, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        super.onOptionsItemSelected(item)

        when (item?.itemId) {
            android.R.id.home -> { onBackPressed() }
            R.id.app_bar_change_album -> {
                alertChangeAlbum()
            }
            R.id.app_bar_delete_photo -> {
                Log.d("test","button2")
                alertAndDeleteFromDatabaseAndStorage()
            }
        }

        return true
    }

    private fun alertChangeAlbum() {
        Log.d("test", "Show dialog change album")
        val refDbListOfAlbum = refDatabase.child("photoAlbum")

        refDbListOfAlbum.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
                Log.d("test", "Error while downloading list of albums")
                Toast.makeText(thisMainActivity, resources.getString(R.string.e_change_album), Toast.LENGTH_LONG).show()
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val listOfAlbums: MutableList<PhotoAlbum> = mutableListOf()

                for(i in snapshot.children){
                    val title: String = i.getValue(String::class.java) ?: ""
                    val newRow = PhotoAlbum(title, i.ref.key.toString())
                    listOfAlbums.add(newRow)
                }
                Log.d("test", "List of albums downloaded successfully!")

                val singleItems = listOfAlbums.map { it.title }.toTypedArray()
                var choosen: Int

                Log.d("test", "Showing dialog...")

                if (singleItems.isEmpty())
                {
                    MaterialAlertDialogBuilder(thisMainActivity)
                        .setMessage(resources.getString(R.string.no_album_created))
                        .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->

                        }
                        .show()
                } else {
                    Log.d("test", "Searching album to default select...")
                    val checkedItem = listOfAlbums.indexOfFirst { it.albumId == photo.albumId }
                    choosen = 0

                    MaterialAlertDialogBuilder(thisMainActivity)
                        .setTitle(resources.getString(R.string.Choose_album))
                        .setNeutralButton(resources.getString(R.string.cancel)) { dialog, which ->
                            choosen = -1
                        }
                        .setPositiveButton(resources.getString(R.string.ok)) { dialog, which ->
                            if (choosen > -1) {
                                Log.d("test", "Changing album...")
                                Log.d("test", "Deleting from old album")
                                refDbListOfPhotos.child(photo.albumId).child(photo.photoName.dropLast(4)).removeValue()
                                Log.d("test", "Adding to new album")
                                refDbListOfPhotos.child(listOfAlbums[choosen].albumId).child(photo.photoName.dropLast(4)).setValue(photo.urlToFile)

                                Log.d("test", "Changing album operation completed")
                                photo.albumId = listOfAlbums[choosen].albumId
                                intent.putExtra("idAlbumP", photo.albumId)
                                Log.d("test", "Changing album id in photo completed")

                                Toast.makeText(thisMainActivity, resources.getString(R.string.p_changed_album), Toast.LENGTH_LONG).show()
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
        })


    }

    private fun alertAndDeleteFromDatabaseAndStorage() {

        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_photo_question))
            .setMessage(resources.getString(R.string.not_back))
            .setNegativeButton(resources.getString(R.string.no)) { dialog, which ->
                // nothing
            }
            .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->
                val mStorageRefUser: StorageReference = mStorageRef.child(userID)
                val mStorageRefUserImages = mStorageRefUser.child("images")
                val desertDeleteRef = mStorageRefUserImages.child(photo.photoName)

                val deleteTask = desertDeleteRef.delete()
                deleteTask.addOnSuccessListener(object: OnSuccessListener<Void?> {
                    override fun onSuccess(dataResult: Void?) {
                        Log.d("test","Photo successfully removed from Firebase Storage")
                        refDbListOfPhotos.child(photo.albumId).child(photo.photoName.dropLast(4)).removeValue().addOnSuccessListener(object : OnSuccessListener<Void?> {
                            override fun onSuccess(p0: Void?) {
                                Log.d("test","Post about photo deleted from Firebase Realtime Database")
                            }
                        })
                        Toast.makeText(thisMainActivity, resources.getString(R.string.p_photo_deleted), Toast.LENGTH_LONG).show()
                        finish()
                    }
                }).addOnFailureListener(object : OnFailureListener {
                    override fun onFailure(p0: Exception) {
                        Log.d("test","Error during removing photo from Firebase Storage")
                        Toast.makeText(thisMainActivity, resources.getString(R.string.e_photo_deleted_fail), Toast.LENGTH_LONG).show()
                    }
                })

            }
            .show()

    }

}