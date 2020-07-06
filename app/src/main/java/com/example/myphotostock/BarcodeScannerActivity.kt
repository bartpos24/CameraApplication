package com.example.myphotostock

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Camera.Parameters.FOCUS_MODE_AUTO
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.MenuItem
import android.view.SurfaceHolder
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_barcode_scanner.*
import java.io.IOException
import java.util.*

class BarcodeScannerActivity : AppCompatActivity() {

    internal lateinit var auth: FirebaseAuth
    internal lateinit var refDatabase: DatabaseReference

    private lateinit var listOfScannerList: MutableList<ScannerList>
    private lateinit var refDbScannerLists: DatabaseReference

    var thisAct: Activity = this
    var lastChoosenSpinnerItem = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode_scanner)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(resources.getString(R.string.scanner_title))

        //Auth
        auth = FirebaseAuth.getInstance()
        val userID = auth.currentUser?.uid

        //Database
        val firebase: FirebaseDatabase = FirebaseDatabase.getInstance()
        refDatabase = firebase.getReference("$userID")

        refDbScannerLists  = refDatabase.child("ScannerLists")

        auth.addAuthStateListener {
            if(auth.currentUser == null){
                this.finish()
            }
        }

        refDbScannerLists.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {

            }

            override fun onDataChange(databaseSnapshot: DataSnapshot) {
                listOfScannerList = mutableListOf()

                for(i in databaseSnapshot.children){
                    val name: String = i.child("name").getValue().toString()
                    val newRow = ScannerList(i.ref.key.toString(), name)
                    listOfScannerList.add(newRow)
                }

                loadSpinner()
            }

        })


        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        val cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(300,400)
            .setAutoFocusEnabled(true)
            .setFocusMode(FOCUS_MODE_AUTO)
            .setRequestedFps(15.0f)
            .build()

        btn_addItem.setOnClickListener {
            Log.d("test", "Add button clicked")
            addScannedItemToList()
        }

        SV_camera_view.getHolder().addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            thisAct,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(thisAct,
                            arrayOf(Manifest.permission.CAMERA), 1234)
                        return
                    }
                    cameraSource.start(SV_camera_view.getHolder())
                } catch (ie: IOException) {
                    Log.e("CAMERA SOURCE", ie.message)
                }
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }

        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}
            override fun receiveDetections(detections: Detections<Barcode>) {
                val barcodes: SparseArray<Barcode> = detections.detectedItems

                if (barcodes.size() != 0) {
                    TV_code_info.post(Runnable
                    // Use the post method of the TextView
                    {
                        TV_code_info.setText( // Update the TextView
                            barcodes.valueAt(0).displayValue
                        )
                    })
                }
            }
        })

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 1234) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("test", "CAMERA Permission granted")
                recreate()
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun loadSpinner() {
        if (spinner != null) {
            val adapter = ArrayAdapter(this,
                R.layout.cell_spinner, R.id.TV_spinnerItemName, listOfScannerList.map { it.name }
            )
            spinner.adapter = adapter
        }
        spinner.setSelection(lastChoosenSpinnerItem)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        super.onOptionsItemSelected(item)

        when (item?.itemId) {
            android.R.id.home -> { onBackPressed() }
        }

        return true
    }

    private fun addScannedItemToList() {
        Log.d("test", "Run function addScannedItemToList")
        val scanResult = TV_code_info.text.toString()
        if (scanResult.trim().isEmpty()) {
            Log.d("test", "Empty scanner result")
            Toast.makeText(thisAct, resources.getString(R.string.e_no_scan_data) ,Toast.LENGTH_LONG).show()
        } else {
            if (spinner != null && spinner.selectedItem != null && listOfScannerList.isNotEmpty()) {
                Log.d("test", "Start adding record to list...")
                lastChoosenSpinnerItem = spinner.selectedItemId.toInt()
                val listId = listOfScannerList[spinner.selectedItemId.toInt()].id
                val refDbScannerRecords: DatabaseReference = refDbScannerLists.child(listId).child("contentOfList")
                val newScanRec = ScannerRecord("${Date().time}", TV_code_info.text.toString(), listId)
                refDbScannerRecords.child(newScanRec.idRecord).setValue(newScanRec.content)
                Toast.makeText(thisAct, resources.getString(R.string.p_added_to_list) ,Toast.LENGTH_LONG).show()
                Log.d("test", "Record added to list")
            } else {
                Toast.makeText(thisAct, resources.getString(R.string.e_no_scanner_list) ,Toast.LENGTH_LONG).show()
                Log.d("test", "No scanner list")
            }
        }
    }

}

