package com.example.myphotostock

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.baoyz.swipemenulistview.SwipeMenu
import com.baoyz.swipemenulistview.SwipeMenuCreator
import com.baoyz.swipemenulistview.SwipeMenuItem
import com.baoyz.swipemenulistview.SwipeMenuListView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.alert_input_text.view.*
import kotlinx.android.synthetic.main.fragment_albums.*
import kotlinx.android.synthetic.main.fragment_scanner.*
import kotlinx.android.synthetic.main.fragment_scanner.fab_add
import java.util.*


class ScannerFragment : Fragment() {

    private lateinit var listOfScannerList: MutableList<ScannerList>
    private lateinit var refDbScannerLists: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.show()
        activity?.setTitle(resources.getString(R.string.scanner_database))

        refDbScannerLists  = (activity as MainActivity).refDatabase.child("ScannerLists")

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

                setupListView()
            }

        })

        fab_add.setOnClickListener {
            showAlertAddNewScannerList()
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
        LV_scanner_lists.setMenuCreator(creator)

        //Swipe to delete - listener

        LV_scanner_lists.setOnMenuItemClickListener(object : SwipeMenuListView.OnMenuItemClickListener {
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
                refDbScannerLists.child("${listOfScannerList[position].id}").removeValue()
            }
            .show()
    }

    private fun showAlertAddNewScannerList() {
        val dialogBuilder = AlertDialog.Builder(activity!!)
        dialogBuilder.setTitle(resources.getString(R.string.alert_add_new_scanner_list))
        val view = LayoutInflater.from(activity!!).inflate(R.layout.alert_input_text, null)

        dialogBuilder.setView(view)

        dialogBuilder.setPositiveButton(resources.getString(R.string.add)) { dialog, which ->
            if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                addNewScannerList(view.ET_inputText.text.toString())
            }
        }

        dialogBuilder.setNegativeButton(resources.getString(R.string.cancel)) { dialog, which ->

        }


        val alertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setOnShowListener(DialogInterface.OnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(activity!!, R.color.colorAccent))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(activity!!, R.color.danger_color))
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!view.ET_inputText.text.toString().trim().isEmpty()) {
                    addNewScannerList(view.ET_inputText.text.toString())
                    alertDialog.dismiss()
                }
            }
        })

        alertDialog.show()
    }

    private fun addNewScannerList(text: String) {
        val firebaseInput = ScannerList("${Date().time}",text)
        refDbScannerLists.child(firebaseInput.id).child("name").setValue(firebaseInput.name)
    }

    private fun setupListView() {

        val scannerListAdapter: ArrayAdapter<String> = ArrayAdapter(activity!!, R.layout.cell_one_title_item, R.id.TV_Title, listOfScannerList.map { it.name } )

        if (LV_scanner_lists != null) {
            LV_scanner_lists.adapter = scannerListAdapter

            LV_scanner_lists.setOnItemClickListener {
                    parent, view, position, id ->
                val intent = Intent(activity, ScannerListOfRecordsActivity::class.java)
                val item = listOfScannerList[id.toInt()]
                intent.putExtra("nameListScanner", item.name)
                intent.putExtra("idListScanner", item.id)
                startActivity(intent)
            }
        }

    }

}