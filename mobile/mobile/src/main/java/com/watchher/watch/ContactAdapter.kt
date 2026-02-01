package com.watchher.watch

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contacts: MutableList<EmergencyContact>,
    private val onContactClickListener: (EmergencyContact, Int) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_contact_name)
        val relationship: TextView = itemView.findViewById(R.id.tv_contact_relationship)
        val phone: TextView = itemView.findViewById(R.id.tv_contact_phone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.name.text = contact.name
        holder.relationship.text = contact.relationship
        holder.phone.text = contact.phone

        holder.itemView.setOnClickListener {
            onContactClickListener(contact, position)
        }
    }

    override fun getItemCount() = contacts.size

    fun addContact(contact: EmergencyContact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun updateContact(contact: EmergencyContact, position: Int) {
        contacts[position] = contact
        notifyItemChanged(position)
    }

    fun removeContact(position: Int) {
        contacts.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, contacts.size)
    }
}