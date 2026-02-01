package com.watchher.watch

import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<EmergencyContact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup RecyclerView
        val rvContacts = findViewById<RecyclerView>(R.id.rv_contacts)
        contactAdapter = ContactAdapter(contactList) { contact, position ->
            showContactDialog(contact, position)
        }
        rvContacts.adapter = contactAdapter
        rvContacts.layoutManager = LinearLayoutManager(this)

        val btnOpenAddContact = findViewById<Button>(R.id.btn_open_add_contact)
        btnOpenAddContact.setOnClickListener {
            showContactDialog()
        }
    }

    private fun showContactDialog(contact: EmergencyContact? = null, position: Int = -1) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null)
        val etName = dialogView.findViewById<EditText>(R.id.dialog_et_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.dialog_et_phone)
        val etRelationship = dialogView.findViewById<EditText>(R.id.dialog_et_relationship)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val btnDelete = dialogView.findViewById<Button>(R.id.btn_dialog_delete)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_dialog_cancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_dialog_save)

        val isEditing = contact != null
        if (isEditing) {
            tvTitle.text = "Edit Contact"
            etName.setText(contact?.name)
            etPhone.setText(contact?.phone)
            etRelationship.setText(contact?.relationship)
            btnDelete.visibility = View.VISIBLE
            btnSave.text = "Update"
        } else {
            tvTitle.text = "Add to Circle"
            btnDelete.visibility = View.GONE
            btnSave.text = "Save"
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Remove the default white background of the dialog container
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        btnDelete.setOnClickListener {
            contactAdapter.removeContact(position)
            Toast.makeText(this, "Deleted: ${contact?.name}", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
        }

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val phone = etPhone.text.toString()
            val relationship = etRelationship.text.toString()
            
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                val updatedContact = EmergencyContact(name, phone, relationship)
                if (isEditing) {
                    contactAdapter.updateContact(updatedContact, position)
                    Toast.makeText(this, "Updated: $name", Toast.LENGTH_SHORT).show()
                } else {
                    contactAdapter.addContact(updatedContact)
                    Toast.makeText(this, "Added: $name", Toast.LENGTH_SHORT).show()
                }
                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Name and Phone are required", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }
}