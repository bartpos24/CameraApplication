package com.example.myphotostock

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.core.app.NavUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_show_scanner_record.*
import kotlinx.android.synthetic.main.alert_input_text.*
import java.util.*

class ShowScannerRecordActivity : AppCompatActivity() {

    private lateinit var idRecord: String
    private lateinit var contextRecord: String
    private lateinit var idOfList: String
    private lateinit var nameOfList: String

    internal lateinit var auth: FirebaseAuth
    private lateinit var refDbScannerRecords: DatabaseReference
    internal lateinit var refDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        idRecord = intent.getStringExtra("idRecord")
        contextRecord = intent.getStringExtra("contextRecord")

        idOfList = intent.getStringExtra("idList")
        nameOfList = intent.getStringExtra("nameList")

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

        setContentView(R.layout.activity_show_scanner_record)


        supportActionBar?.setTitle("")

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTextTextMultiLine.setText(contextRecord)

        fab_save.setOnClickListener {
            saveEditedRecord()
        }

    }

    private fun saveEditedRecord() {
        val text = editTextTextMultiLine.text.toString()
        if (!text.trim().isEmpty()) {
            refDbScannerRecords.child(idRecord).setValue(text)
            onBackPressed()
        }

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