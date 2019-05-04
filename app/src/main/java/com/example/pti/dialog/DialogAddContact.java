package com.example.pti.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.pti.R;
import com.example.pti.model.Contact;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class DialogAddContact extends DialogFragment {

    EditText contact_number;
    ImageButton btn_search_contact;
    public final int PICK_NUMBER = 0;
    Contact contact;
    private DialogAddContactListener dialogAddContactListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_pick_contact,null);
        builder.setView(view);
        contact_number = view.findViewById(R.id.contact_number);
        btn_search_contact = view.findViewById(R.id.btn_search_contact);

        btn_search_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(i, PICK_NUMBER);

            }
        });
        contact = new Contact(null,null);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<Contact> contactList = new ArrayList<Contact>();
                SharedPreferences preferences =  getContext().getSharedPreferences(getContext().getResources().getString(R.string.pref_key_contact),getContext().MODE_PRIVATE);;
                String serializedContact = preferences.getString(getContext().getResources().getString(R.string.key_contact),null);
                if (serializedContact != null){
                    Gson gson = new Gson();
                    Type type = new TypeToken<List<Contact>>(){}.getType();
                    contactList = gson.fromJson(serializedContact,type);
                }
                contact.setNumber(contact_number.getText().toString());
                //Log.i("TAG", "onClick: "+existIn(contact,contactList)+","+contactList.size());
                if (existIn(contact,contactList)){
                    Toast.makeText(getContext(), "Ce contact existe déja dans la liste des contacts d'urgence!", Toast.LENGTH_SHORT).show();
                }else{
                    dialogAddContactListener.applyText(contact);
                }



            }
        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        return builder.create();
    }

    private boolean existIn(Contact contact,List<Contact> contactList ){
        for (int i=0;i<contactList.size();i++){
            if (contact.getNumber().equals(contactList.get(i).getNumber())){
                if (contact.getName()==null){
                    return true;
                }else {
                    if (contact.getName().equals(contactList.get(i).getName()))
                        return true;                }

            }
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_NUMBER && resultCode == RESULT_OK){
            Uri contactUri = data.getData();
            Cursor cursor = getContext().getContentResolver().query(contactUri,null,null,null,null);
            cursor.moveToFirst();
            int name = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int number = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            contact.setName(cursor.getString(name));
            contact_number.setText(cursor.getString(number));



        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            dialogAddContactListener = (DialogAddContactListener) context;
        }catch (ClassCastException e){
            throw new ClassCastException(context.toString()+
                    "must implement DialogAddContactListener");

        }

    }

    public  interface DialogAddContactListener{
        void applyText(Contact contact);
    }


}
