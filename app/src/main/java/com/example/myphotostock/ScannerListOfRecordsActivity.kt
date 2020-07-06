package com.example.myphotostock

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.Window
import android.view.Window.FEATURE_ACTION_BAR
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NavUtils
import androidx.core.content.ContextCompat
import com.baoyz.swipemenulistview.SwipeMenu
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_scanner_list_of_records.*
import kotlinx.android.synthetic.main.alert_input_text.view.*
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.fragment_scanner.*
import java.util.*

class ScannerListOfRecordsActivity : AppCompatActivity() {

    private lateinit var listOfRecords: MutableList<ScannerRecord>
    private lateinit var idOfList: String
    private lateinit var nameOfList: String
    internal lateinit var auth: FirebaseAuth
    private lateinit var refDbScannerRecords: DatabaseReference
    internal lateinit var refDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        idOfList = intent.getStringExtra("idListScanner")
        nameOfList = intent.getStringExtra("nameListScanner")

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

        refDbScannerRecords = refDatabase.child("ScannerLists").child("${idOfList}").child("contentOfList")

        setContentView(R.layout.activity_scanner_list_of_records)
        setTitle(nameOfList)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        refDbScannerRecords.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(databaseSnapshot: DataSnapshot) {
                listOfRecords = mutableListOf()

                for(i in databaseSnapshot.children){
                    val content: String = i.getValue(String::class.java) ?: ""
                    val newRow = ScannerRecord(i.ref.key.toString(), content, idOfList)
                    listOfRecords.add(newRow)
                }

                setupListView()
            }

        })

        fab_add_rec.setOnClickListener {
            showAlertAddNewScannerRecord()
        }

        //Swipe to delete

        val creator = SwipeMenuCreator { menu ->

            // create "delete" item
            val deleteItem = SwipeMenuItem(this)
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
        LV_scanner_records.setMenuCreator(creator)

        //Swipe to delete - listener

        LV_scanner_records.setOnMenuItemClickListener(object : SwipeMenuListView.OnMenuItemClickListener {
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

        MaterialAlertDialogBuilder(this)
            .setTitle(resources.getString(R.string.delete_question))
            .setMessage(resources.getString(R.string.not_back))
            .setNegativeButton(resources.getString(R.string.no)) { dialog, which ->
                // nothing
            }
            .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->
                refDbScannerRecords.child("${listOfRecords[position].idRecord}").removeValue()
            }
            .show()

    }

    private fun showAlertAddNewScannerRecord() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(resources.getString(R.string.alert_add_new_scanner_record))
        val view = LayoutInflater.from(this).inflate(R.layout.alert_input_text, null)

        dialogBuilder.setView(view)

        dialogBuilder.setPositiveButton(resources.getString(R.string.add)) { dialog, which ->
            if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                addNewRecord(view.ET_inputText.text.toString())
            }
        }

        dialogBuilder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->

        }


        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setOnShowListener(DialogInterface.OnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.danger_color))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                    addNewRecord(view.ET_inputText.text.toString())
                    alertDialog.dismiss()
                }
            }
        })

        alertDialog.show()
    }

    private fun addNewRecord(text: String) {
        val firebaseInput = ScannerRecord("${Date().time}", text, idOfList)
        refDbScannerRecords.child(firebaseInput.idRecord).setValue(firebaseInput.content)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    private fun setupListView() {

        val scannerListAdapter: ArrayAdapter<String> = ArrayAdapter(this, R.layout.cell_one_title_item, R.id.TV_Title, listOfRecords.map { it.content } )

        LV_scanner_records.adapter = scannerListAdapter

        LV_scanner_records.setOnItemClickListener {
                parent, view, position, id ->
            val intent = Intent(this, ShowScannerRecordActivity::class.java)
            val item = listOfRecords[id.toInt()]
            intent.putExtra("contextRecord", item.content)
            intent.putExtra("idRecord", item.idRecord)
            intent.putExtra("nameList", nameOfList)
            intent.putExtra("idList", idOfList)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            startActivity(intent)
        }

    }

}