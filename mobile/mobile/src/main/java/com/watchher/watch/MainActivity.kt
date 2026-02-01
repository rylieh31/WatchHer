package com.watchher.watch

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
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.Wearable

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

        var channelClient: ChannelClient?

        val nodeClient = Wearable.getNodeClient(this)
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                Log.d("WEAR", "Found node: ${node.displayName} (${node.id})")

                val nodeId = node.id
                val path = "/watchher"

                channelClient = Wearable.getChannelClient(this)
                channelClient.openChannel(nodeId, path)
                    .addOnSuccessListener { channel ->
                        Log.d("WEAR", "Channel opened: ${channel.path}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WEAR", "Failed to open channel", e)
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
                val relationship = dialogView.findViewById<EditText>(R.id.dialog_et_relationship).text.toString()

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