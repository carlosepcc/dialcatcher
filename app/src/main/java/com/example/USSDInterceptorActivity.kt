package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.example.data.AppDatabase
import com.example.data.InterceptedCall
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder

class USSDInterceptorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intentAction = intent.action ?: "UNKNOWN"
        val intentData = intent.data

        Log.d("USSD_CAPTURED", "Received intent action: $intentAction, data: $intentData")

        if (intentData != null && intentData.scheme == "tel") {
            val rawNumber = intentData.schemeSpecificPart ?: ""
            val decodedUSSD = try {
                URLDecoder.decode(rawNumber, "UTF-8")
            } catch (e: Exception) {
                rawNumber
            }

            Log.d("USSD_CAPTURED", "Código detectado: $decodedUSSD")
            
            // Ultra-prominent logging for quick developer desktop copy/paste from Logcat
            Log.i("USSD_CAPTURED", "=================== DIAL INTERCEPTOR ===================")
            Log.i("USSD_CAPTURED", "[INTERCEPTED] >>> $decodedUSSD <<<")
            Log.i("USSD_CAPTURED", "Raw URI Specific: $rawNumber")
            Log.i("USSD_CAPTURED", "Action Type     : $intentAction")
            Log.i("USSD_CAPTURED", "========================================================")

            // Persist the intercepted dial into the Room database
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = (application as? InterceptorApplication)?.repository 
                ?: com.example.data.InterceptedCallRepository(database.interceptedCallDao())

            val actionLabel = if (intentAction == Intent.ACTION_CALL) "CALL" else "DIAL"

            CoroutineScope(Dispatchers.IO).launch {
                repository.insert(
                    InterceptedCall(
                        rawUri = intentData.toString(),
                        decodedCode = decodedUSSD,
                        actionType = actionLabel,
                        notes = "Intercepted via system intent ($actionLabel)"
                    )
                )
            }
        }

        // Return to MainActivity or start it if it is not open so user can see logs
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(launchIntent)
        finish()
    }
}
