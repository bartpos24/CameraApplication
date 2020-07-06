package com.example.myphotostock

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.myphotostock.login.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {

    internal lateinit var auth: FirebaseAuth
    internal lateinit var refDatabase: DatabaseReference
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d("test","onCreate MainActivity")
        //display albums fragment
        loadFragment(AlbumsFragment())
        menu_bottom.setItemSelected(R.id.albmus)

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

        menu_bottom.setOnItemSelectedListener { id->
            when(id){

                R.id.camera->loadFragment(CameraFragment())
                R.id.albmus->loadFragment(AlbumsFragment())
                R.id.scanner->loadFragment(ScannerFragment())
                R.id.logout-> {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(resources.getString(R.string.logout_alert))
                        .setNegativeButton(resources.getString(R.string.no)) { dialog, which ->
                            menu_bottom.setItemSelected(0)
                        }
                        .setPositiveButton(resources.getString(R.string.yes)) { dialog, which ->
                            auth.signOut()
                            startActivity(Intent(this, LoginActivity::class.java))
                        }
                        .show()

                }
                else->{return@setOnItemSelectedListener}
            }
        }
    }


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().also { fragmentTransaction ->
            var tag = ""
            when(fragment){
                is AlbumsFragment -> tag = "frag_alb"
                is CameraFragment -> tag = "frag_cam"
                is ScannerFragment -> tag = "frag_sca"
                else -> tag = ""
            }
            val mFragment: Fragment? = supportFragmentManager.findFragmentByTag(tag)
            mFragment?.let {
                Log.d("test", "fragment present")
                fragmentTransaction.replace(R.id.fragmentContainer, mFragment, tag).addToBackStack(null)
            } ?: run {
                Log.d("test", "fragment not present")
                fragmentTransaction.replace(R.id.fragmentContainer, fragment, tag).addToBackStack(null)
            }

            fragmentTransaction.commit()
        }
    }
}