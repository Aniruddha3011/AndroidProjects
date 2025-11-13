package com.example.emergencysos.Adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Models.EmergencyContact;
import com.example.emergencysos.R;

import java.util.*;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    private List<EmergencyContact> contactList;
    private Context context;

    public EmergencyContactAdapter(List<EmergencyContact> contactList, Context context) {
        this.contactList = contactList;
        this.context = context;
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView name, phone, relation;
        ImageView deleteBtn;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_contact_name);
            phone = itemView.findViewById(R.id.tv_contact_phone);
            relation = itemView.findViewById(R.id.tv_contact_relation);
            deleteBtn = itemView.findViewById(R.id.btn_delete_contact);
        }
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sample_layout_emergencycontact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contactList.get(position);
        holder.name.setText(contact.getName());
        holder.phone.setText(contact.getPhone());
        holder.relation.setText(contact.getRelation());

        holder.name.setTextColor(Color.BLACK);
        holder.phone.setTextColor(Color.BLACK);
        holder.relation.setTextColor(Color.BLACK);

        holder.deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        contactList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, contactList.size());


                        saveContactListToPrefs();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    private void saveContactListToPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE);
        Set<String> saveSet = new HashSet<>();
        for (EmergencyContact c : contactList) {
            saveSet.add(c.getName() + "," + c.getPhone() + "," + c.getRelation());
        }
        prefs.edit().putStringSet("contacts", saveSet).apply();
    }
}
