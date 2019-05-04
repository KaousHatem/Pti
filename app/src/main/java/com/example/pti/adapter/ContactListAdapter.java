package com.example.pti.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.pti.R;
import com.example.pti.model.Contact;

import java.util.List;

public class ContactListAdapter extends ArrayAdapter<Contact> {
    private Context context;
    private OnItemClickListener mItemClickListener;
    List<Contact> contacts;

    public ContactListAdapter(Context context, List<Contact> contacts) {
        super(context, R.layout.item_list_contact,contacts);
        this.context = context;
        this.contacts = contacts;
    }

    private class ViewHolder{
        TextView textViewName;
        TextView textViewNumber;
        TextView textViewNumber2;
        ImageButton btn_delete_contact;


    }

    @Override
    public int getCount() {
        return contacts.size();
    }


    @Override
    public Contact getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }


    @SuppressLint("WrongConstant")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

            convertView = inflater.inflate(R.layout.item_list_contact,null);
            holder = new ViewHolder();
            holder.textViewName = convertView.findViewById(R.id.textViewName);
            holder.textViewNumber = convertView.findViewById(R.id.textViewNumber);
            holder.textViewNumber2 = convertView.findViewById(R.id.textViewNumber2);
            holder.btn_delete_contact = convertView.findViewById(R.id.btn_delete_contact);

            convertView.setTag(holder);
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        Contact contact = getItem(position);
        if (contact.getName() != null){
            holder.textViewNumber.setVisibility(View.VISIBLE);
            holder.textViewName.setVisibility(View.VISIBLE);
            holder.textViewNumber2.setVisibility(View.INVISIBLE);
            holder.textViewName.setText(contact.getName());
            holder.textViewNumber.setText(contact.getNumber());
            Log.i("TAG", "getView: "+contact.getNumber());

        }else {
            holder.textViewNumber.setVisibility(View.INVISIBLE);
            holder.textViewName.setVisibility(View.INVISIBLE);
            holder.textViewNumber2.setVisibility(View.VISIBLE);
            holder.textViewNumber2.setText(contact.getNumber());

        }

        holder.btn_delete_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mItemClickListener != null){
                    mItemClickListener.onItemClick(view,position);
                }
            }
        });


        return convertView;
    }

    @Override
    public void add(Contact contact) {
        super.add(contact);
        Log.i("TAG", "add: added"+contact.getNumber());
        //notifyDataSetChanged();
    }

    @Override
    public void remove(Contact contact) {
        super.remove(contact);
        notifyDataSetChanged();
    }

    public interface OnItemClickListener{
        void onItemClick(View view, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener){
        this.mItemClickListener = mItemClickListener;
    }
}
