package com.example.myphotostock

import android.app.Activity
import android.content.Context
import android.content.DialogInterface.OnShowListener
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.baoyz.swipemenulistview.SwipeMenu
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.alert_input_text.view.*
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.fragment_albums.view.*
import java.lang.Exception
import java.util.*


class AlbumsFragment : Fragment() {

    internal lateinit var auth: FirebaseAuth
    private lateinit var userID: String

    private lateinit var listOfAlbums: MutableList<PhotoAlbum>
    private lateinit var refDbPhotoAlbum: DatabaseReference
    private lateinit var mStorageRef: StorageReference
    private lateinit var listOfPhotosInAlbum: MutableList<Photo>
    private lateinit var act: MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_albums, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        act = activity as MainActivity
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Auth
        auth = FirebaseAuth.getInstance()
        userID = auth.currentUser?.uid.toString()

        mStorageRef = FirebaseStorage.getInstance().reference

        (activity as AppCompatActivity).supportActionBar?.show()
        activity?.setTitle(resources.getString(R.string.albums))

        refDbPhotoAlbum = (activity as MainActivity).refDatabase.child("photoAlbum")
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

                setupListView()
            }

        })

        fab_add.setOnClickListener {
            showAlertAddNewAlbum()
        }

        //Swipe to delete

        val creator = SwipeMenuCreator { menu ->

            // create "delete" item
            val deleteItem = SwipeMenuItem(activity)
            // set item background
            deleteItem.background = ColorDrawable(Color.rgb(0xF9, 0x3F, 0x25))
            // set item width
            deleteItem.width = 200
            // set a icon
            deleteItem.setIcon(R.drawable.ic_baseline_delete_24)
            // add to menu
            menu.addMenuItem(deleteItem)
        }

        // set creator
        LV_albums.setMenuCreator(creator)

        //Swipe to delete - listener

        LV_albums.setOnMenuItemClickListener(object : SwipeMenuListView.OnMenuItemClickListener {
            override fun onMenuItemClick(position: Int, menu: SwipeMenu?, index: Int): Boolean {
                when (index) {
                    0 -> {
                        alertAndDeleteFromDatabase(position)
                    }
                }
                return false
            }
        })

    }

    private fun alertAndDeleteFromDatabase(position: Int) {

        MaterialAlertDialogBuilder(activity!!)
            .setTitle(resources.getString(R.string.delete_question))
            .setMessage(resources.getString(R.string.not_back))
            .setNegativeButton(resources.getString(R.string.no)) { dialog, which ->
                // nothing
            }
            .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->

                val refDbListOfPhotosInAlbum = (activity as MainActivity).refDatabase.child("listOfPhotos").child(listOfAlbums[position].albumId)

                refDbListOfPhotosInAlbum.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                        Log.d("test", "Error while downloading list of photos")
                        Toast.makeText(act, resources.getString(R.string.e_delete_album_fail), Toast.LENGTH_LONG).show()
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        listOfPhotosInAlbum = mutableListOf()

                        for(i in snapshot.children){
                            val url: String = i.getValue(String::class.java) ?: ""
                            val newRow = Photo(url, listOfAlbums[position].albumId, i.ref.key.toString()+".jpg")
                            listOfPhotosInAlbum.add(newRow)
                        }
                        Log.d("test", "List of photo downloaded successfully!")

                        val listOfPhotosToDelete = listOfPhotosInAlbum.map { it.photoName }

                        Log.d("test", "Deleting photos...")
                        listOfPhotosToDelete.forEach {
                            deletePhotoFromAlbumInStorage(it)
                        }

                        Log.d("test", "Deleting photo posts from database...")
                        refDbListOfPhotosInAlbum.removeValue()

                        Log.d("test", "Deleting album from database...")
                        refDbPhotoAlbum.child("${listOfAlbums[position].albumId}").removeValue()

                        Toast.makeText(act, resources.getString(R.string.p_album_deleted), Toast.LENGTH_LONG).show()

                    }

                })


            }
            .show()

    }

    private fun deletePhotoFromAlbumInStorage(photoName: String) {
        val mStorageRefUser: StorageReference = mStorageRef.child( userID )
        val mStorageRefUserImages = mStorageRefUser.child("images")
        val desertDeleteRef = mStorageRefUserImages.child(photoName)

        val deleteTask = desertDeleteRef.delete()
        deleteTask.addOnSuccessListener(object: OnSuccessListener<Void?> {
            override fun onSuccess(dataResult: Void?) {
                Log.d("test","Photo $photoName successfully removed from Firebase Storage")
            }
        }).addOnFailureListener(object : OnFailureListener {
            override fun onFailure(p0: Exception) {
                Log.d("test","Error during removing photo $photoName from Firebase Storage")
            }
        })
    }

    private fun showAlertAddNewAlbum() {
        val dialogBuilder = AlertDialog.Builder(activity!!)
        dialogBuilder.setTitle(resources.getString(R.string.alert_add_new_album))
        val view = LayoutInflater.from(activity!!).inflate(R.layout.alert_input_text, null)

        dialogBuilder.setView(view)

        dialogBuilder.setPositiveButton(resources.getString(R.string.add)) { dialog, which ->
            if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                addNewAlbum(view.ET_inputText.text.toString())
            }
        }

        dialogBuilder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->

        }


        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setOnShowListener(OnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(activity!!, R.color.colorAccent))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(activity!!, R.color.danger_color))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                    addNewAlbum(view.ET_inputText.text.toString())
                    alertDialog.dismiss()
                }
            }
        })

        alertDialog.show()
    }

    private fun addNewAlbum(text: String) {
        val firebaseInput = PhotoAlbum(text, "${Date().time}")
        refDbPhotoAlbum.child(firebaseInput.albumId).setValue(firebaseInput.title)
    }

    private fun setupListView() {

        val albumsAdapter: ArrayAdapter<String> = ArrayAdapter<String>(activity, R.layout.cell_one_title_item, R.id.TV_Title, listOfAlbums.map { it.title } )

        if (LV_albums != null ) {
            LV_albums.adapter = albumsAdapter

            LV_albums.setOnItemClickListener {
                    parent, view, position, id ->
                val intent = Intent(activity, PhotoGalleryActivity::class.java)
                val item = listOfAlbums[id.toInt()]
                intent.putExtra("idAlbum", item.albumId)
                intent.putExtra("titleAlbum", item.title)
                startActivity(intent)
            }
        }

    }

}