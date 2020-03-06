package com.example.mypatternlockapp

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        val PREFERENCE:String = "my_preference"
        val PERMISSIONS_REQUEST_READ_LOCATION = 1
        val PERMISSIONS_REQUEST_SEND_SMS = 2
    }

    lateinit var sharedPreferences: SharedPreferences;

    private enum class STATE {
        NO_STATE, LOGIN, SIGNUP
    }

    private var currentState = STATE.NO_STATE;
    private var counter = 0
    private var failCounter = 0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val subscriptionManager =
//            SubscriptionManager.from(applicationContext)
//        val subscriptionInfoList =
//            subscriptionManager.activeSubscriptionInfoList
//        for (subscriptionInfo in subscriptionInfoList) {
//            val subscriptionId = subscriptionInfo.
//            Log.d("apipas", "subscriptionId:$subscriptionId")
//        }

        requestForPermission()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        var tempPattern = ""

        sharedPreferences = getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val existingPattern = sharedPreferences.getString("pattern", "")
        if (existingPattern.equals("")){
            clear.isEnabled = false
            currentState = STATE.SIGNUP
            textView.text = "Please create a pattern sign up"
        }else{
            currentState = STATE.LOGIN;
            textView.text = "Please draw pattern to login"
        }

        val existingPhone = sharedPreferences.getString("phone","")
        if (!existingPhone.equals(""))
            phone.text = existingPhone

        val mPatternLockViewListener: PatternLockViewListener = object : PatternLockViewListener {
            override fun onStarted() {
                Log.d(javaClass.name, "Pattern drawing started")
            }

            override fun onProgress(progressPattern: List<PatternLockView.Dot>) {
                Log.d(
                    javaClass.name, "Pattern progress: " +
                            PatternLockUtils.patternToString(pattern_lock_view, progressPattern)
                )
            }

            override fun onComplete(pattern: List<PatternLockView.Dot>) {
                val _pattern = PatternLockUtils.patternToString(pattern_lock_view, pattern)
                Log.d(javaClass.name, "Pattern complete: " + _pattern)
                var needRecreate = false;

                if (currentState == STATE.LOGIN){
                    if (existingPattern.equals(_pattern)){
                        textView.text = "Successful"
                        //Toast.makeText(this@MainActivity, "", Toast.LENGTH_SHORT).show()
                    }else{
                        textView.text = "Fail"
                        failCounter++
                        if (failCounter==3) {
                            //resetFailCouter
                            failCounter=0
                            //notify publisher
                            onWrongPatternDraw3Times()
                        }
                        //Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
                    }
                }else if (currentState == STATE.SIGNUP){
                    if (counter==0) {
                        tempPattern = _pattern
                        counter++
                        textView.text = "Please draw one more time"
                        //Toast.makeText(this@MainActivity, "", Toast.LENGTH_LONG).show()
                    }
                    else if (counter==1){
                        if (tempPattern.equals(_pattern)) {
                            sharedPreferences.edit().putString("pattern", _pattern).apply()
                            textView.text = "Successful"
                            //Toast.makeText(this@MainActivity, "", Toast.LENGTH_SHORT)
                             //   .show()
                            needRecreate = true
                        }else{
                            textView.text = "Wrong Pattern, Try Again"
                            //Toast.makeText(this@MainActivity, "", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                Handler().postDelayed({
                    pattern_lock_view.clearPattern()
                    if (needRecreate) recreate()
                }, 1000)
            }

            override fun onCleared() {
                Log.d(javaClass.name, "Pattern has been cleared")
            }
        }
        pattern_lock_view.addPatternLockListener(mPatternLockViewListener);
    }

    private fun requestForPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.SEND_SMS),
                    PERMISSIONS_REQUEST_READ_LOCATION)
            }
        }
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.SEND_SMS)
//            != PackageManager.PERMISSION_GRANTED) {
//
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.SEND_SMS)) {
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//            } else {
//                // No explanation needed, we can request the permission.
//                ActivityCompat.requestPermissions(this,
//                    arrayOf(Manifest.permission.SEND_SMS),
//                    PERMISSIONS_REQUEST_SEND_SMS)
//            }
//        }
    }

    private fun onWrongPatternDraw3Times() {
        //get location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(sharedPreferences.getString("phone","+919935409177"), null,
                    "${location?.latitude} ${location?.longitude}", null, null)
                Toast.makeText(
                    applicationContext, "SMS sent.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun onPatternClear(v: View){
        pattern_lock_view.clearPattern()
        sharedPreferences.edit().clear().apply();
        recreate()
    }

    fun onPhoneNumber(v: View){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Phone Number")
        builder.setMessage("Enter the phone number with country code")

        // Set up the input
        val input = EditText(this)
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.inputType = InputType.TYPE_CLASS_PHONE
        builder.setView(input)

        // Set up the buttons
        builder.setPositiveButton("SAVE",
            DialogInterface.OnClickListener { dialog, which ->
                val m_Text = input.text.toString()
                sharedPreferences.edit().putString("phone", m_Text).apply()
                phone.text = m_Text
            })
        builder.setNegativeButton("CANCEL",null)

        builder.show()
    }
}
