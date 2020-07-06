package com.example.myphotostock

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_photo_gallery.*


class PhotoGalleryActivity : AppCompatActivity() {

    internal lateinit var auth: FirebaseAuth
    internal lateinit var refDatabase: DatabaseReference

    private lateinit var listOfPhotosInAlbum: MutableList<Photo>
    private lateinit var refDbListOfPhotosInAlbum: DatabaseReference

    private lateinit var idAlbum: String
    private lateinit var titleAlbum: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Auth
        auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid

        //Database
        val firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        refDatabase = firebase.getReference("$userID")

        auth.addAuthStateListener {
            if(auth.currentUser == null){
                this.finish()
            }
        }

        idAlbum = intent.getStringExtra("idAlbum")
        titleAlbum = intent.getStringExtra("titleAlbum")

        refDbListOfPhotosInAlbum = this.refDatabase.child("listOfPhotos").child(idAlbum)
        refDbListOfPhotosInAlbum.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(databaseSnapshot: DataSnapshot) {
                listOfPhotosInAlbum = mutableListOf()

                for(i in databaseSnapshot.children){
                    val url: String = i.getValue(String::class.java) ?: ""
                    val newRow = Photo(url, idAlbum, i.ref.key.toString()+".jpg")
                    listOfPhotosInAlbum.add(newRow)
                }

                setupGridView()
            }

        })

        setContentView(R.layout.activity_photo_gallery)

        setTitle(titleAlbum)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

    }

    private fun setupGridView() {
        val adapter = AdapterPhoto(this, listOfPhotosInAlbum)
        GV_photos.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

}