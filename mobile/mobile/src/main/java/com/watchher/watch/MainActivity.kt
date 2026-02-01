package com.watchher.watch

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.wearable.Wearable
import com.watchher.messages.PhoneToWatch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
                }
            }
        }
        val filter = IntentFilter(WatchReceiverService.ACTION_UPDATE_SAFETY_STATUS)

        registerReceiver(safetyStatusUpdateReceiver, filter, RECEIVER_NOT_EXPORTED)


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
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_dialog_delete)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_dialog_save)

        tvTitle.text = "Add to Circle"
        btnDelete.visibility = View.GONE
        btnSave.text = "Save"

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = dialogView.findViewById<EditText>(R.id.dialog_et_name).text.toString().trim()
            val phone = dialogView.findViewById<EditText>(R.id.dialog_et_phone).text.toString().trim()
            val relationship = dialogView.findViewById<EditText>(R.id.dialog_et_relationship).text.toString().trim()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill in Name and Phone", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }
}