package com.watchher.watch

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
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
                    // For now, just show a message to prove it works
                    Toast.makeText(this, "Saved: $name", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please fill in Name and Phone", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)

        builder.show()
    }
}