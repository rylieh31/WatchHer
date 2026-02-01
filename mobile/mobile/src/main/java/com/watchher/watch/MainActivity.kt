package com.watchher.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.wearable.Wearable
import com.watchher.messages.PhoneToWatch
import java.util.Locale
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    companion object {
        private const val HELP_API_URL = "http://192.168.86.43:8080"
    }

    private lateinit var requestQueue: RequestQueue
    private var lastSafetyStatus: String? = null
    private lateinit var contactAdapter: ContactAdapter
    private val contacts = mutableListOf<EmergencyContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestQueue = Volley.newRequestQueue(this)

        val contactsList: RecyclerView = findViewById(R.id.rv_contacts)
        contactAdapter = ContactAdapter(contacts) { _, _ -> }
        contactsList.layoutManager = LinearLayoutManager(this)
        contactsList.adapter = contactAdapter

        val safetyStatusUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val safetyStatus = intent?.getStringExtra(WatchReceiverService.SAFETY_STATUS)
                Log.d("WatchHerMobile", "Received broadcast: $safetyStatus")
                val textView: TextView = findViewById(R.id.tv_status_value)
                if (safetyStatus == "safe") {
                    textView.text = "SAFE"
                    textView.setTextColor(resources.getColor(R.color.neon_green))
                } else if (safetyStatus == "unsafe") {
                    textView.text = "IN DANGER"
                    textView.setTextColor(Color.rgb(255, 0, 0))
                    if (lastSafetyStatus != "unsafe") {
                        sendHelpRequest()
                    }
                }
                if (safetyStatus == "safe" || safetyStatus == "unsafe") {
                    lastSafetyStatus = safetyStatus
                }
            }
        }
        val filter = IntentFilter(WatchReceiverService.ACTION_UPDATE_SAFETY_STATUS)

        registerReceiver(safetyStatusUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)

        val biometricsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val hrMean = intent?.getDoubleExtra(WatchReceiverService.HR_MEAN, 0.0) ?: 0.0
                val hrStd = intent?.getDoubleExtra(WatchReceiverService.HR_STD, 0.0) ?: 0.0
                val hrSlope = intent?.getDoubleExtra(WatchReceiverService.HR_SLOPE, 0.0) ?: 0.0
                val steps20s = intent?.getIntExtra(WatchReceiverService.STEPS_20S, 0) ?: 0
                val accelRms =
                    intent?.getDoubleExtra(WatchReceiverService.ACCEL_RMS, 0.0) ?: 0.0
                val accelPeak =
                    intent?.getDoubleExtra(WatchReceiverService.ACCEL_PEAK, 0.0) ?: 0.0
                val ppgStd = intent?.getDoubleExtra(WatchReceiverService.PPG_STD, 0.0) ?: 0.0
                val timeOfDay =
                    intent?.getDoubleExtra(WatchReceiverService.TIME_OF_DAY, 0.0) ?: 0.0

                val biometricsView: TextView = findViewById(R.id.tv_biometrics_values)
                biometricsView.text = String.format(
                    Locale.US,
                    "HR mean: %.1f\nHR std: %.2f\nHR slope: %.2f\nSteps(20s): %d\nAccel RMS: %.2f\nAccel peak: %.2f\nPPG std: %.2f\nTime of day: %.2f",
                    hrMean,
                    hrStd,
                    hrSlope,
                    steps20s,
                    accelRms,
                    accelPeak,
                    ppgStd,
                    timeOfDay
                )
            }
        }
        val biometricsFilter = IntentFilter(WatchReceiverService.ACTION_UPDATE_BIOMETRICS)
        registerReceiver(biometricsReceiver, biometricsFilter, RECEIVER_NOT_EXPORTED)

        val confidenceReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val confidence = intent?.getIntExtra(
                    WatchReceiverService.EXTRA_CONFIDENCE,
                    0
                ) ?: 0

                val progressBar: ProgressBar = findViewById(R.id.pb_danger)
                val percentText: TextView = findViewById(R.id.tv_danger_percent)

                progressBar.progress = confidence.coerceIn(0, 100)
                percentText.text = "${confidence.coerceIn(0, 100)}%"
            }
        }
        val confidenceFilter = IntentFilter(WatchReceiverService.ACTION_CONFIDENCE_UPDATE)
        registerReceiver(confidenceReceiver, confidenceFilter, RECEIVER_NOT_EXPORTED)


        startService(Intent(this, WatchReceiverService::class.java))

        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d("WatchHerMobile", "Found node: ${node.displayName}")

                val data = PhoneToWatch(0.52)

                Wearable.getMessageClient(this)
                    .sendMessage(
                        node.id,
                        "/watch_her/phone_to_watch",
                        data.encodeJson().toByteArray()
                    )
                    .addOnSuccessListener {
                        Log.d("WatchHerMobile", "Message Sent!!!")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WatchHerMobile", "Failed to send message", e)
                    }
            }
        }


        // Find the button we added in the XML
        val btnAddContact = findViewById<Button>(R.id.btn_open_add_contact)

        // Set what happens when you click it
        btnAddContact.setOnClickListener {
            showAddContactDialog()
        }
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Contact")
            .setPositiveButton("Save") { _, _ ->
                val name = dialogView.findViewById<EditText>(R.id.dialog_et_name).text.toString()
                val phone = dialogView.findViewById<EditText>(R.id.dialog_et_phone).text.toString()
                val relationship =
                    dialogView.findViewById<EditText>(R.id.dialog_et_relationship).text.toString()

                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    sendNewContactRequest(name) {
                        contactAdapter.addContact(
                            EmergencyContact(
                                name = name,
                                phone = phone,
                                relationship = relationship
                            )
                        )
                        Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please fill in Name and Phone", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        builder.show()
    }

    private fun sendHelpRequest() {
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            "$HELP_API_URL/im-in-danger",
            JSONObject(mapOf("username" to "Charlie")),
            { response ->
                Log.d("WatchHerMobile", "Received response: $response")
            },
            { e ->
                Log.e("WatchHerMobile", "Error sending help message", e)
            }
        )

        requestQueue.add(jsonRequest)
    }

    private fun sendNewContactRequest(name: String, onSuccess: () -> Unit) {
        val jsonRequest = JsonObjectRequest(
            Request.Method.POST,
            "$HELP_API_URL/add-contact",
            JSONObject(mapOf("username" to "Charlie", "contact_name" to name)),
            { response ->
                Log.d("WatchHerMobile", "Received response: $response")
                onSuccess()
            },
            { e ->
                Log.e("WatchHerMobile", "Error sending help message", e)
                Toast.makeText(this, "Failed to add contact", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonRequest)
    }
}