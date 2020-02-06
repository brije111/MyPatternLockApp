package com.example.mypatternlockapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.andrognito.patternlockview.PatternLockView
import com.andrognito.patternlockview.listener.PatternLockViewListener
import com.andrognito.patternlockview.utils.PatternLockUtils
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        val PREFERENCE:String = "my_preference"
    }

    lateinit var sharedPreferences: SharedPreferences;

    private enum class STATE {
        NO_STATE, LOGIN, SIGNUP
    }

    private var currentState = STATE.NO_STATE;

    private var counter = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    fun onPatternClear(v: View){
        pattern_lock_view.clearPattern()
        sharedPreferences.edit().clear().apply();
        recreate()
    }
}
