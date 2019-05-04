package com.example.pti;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.pti.adapter.ContactListAdapter;
import com.example.pti.dialog.DialogAddContact;
import com.example.pti.model.Contact;
import com.example.pti.service.FloatingWidgetService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContactActivity extends AppCompatActivity implements DialogAddContact.DialogAddContactListener{

    private Toolbar toolbar;
    private Button btn_add_contact;
    private ListView listView_contact;
    private ContactListAdapter contactListAdapter;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private boolean appInTop = false;
    List<Contact> contactList;

    private AlertDialog.Builder builder;
    private AlertDialog dialogError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        btn_add_contact = findViewById(R.id.btn_add_contact);
        listView_contact = findViewById(R.id.listView_contact);

        // init toolbar
        init_toolbar("Contact d'urgence");

        //set up listener to btn
        btn_add_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogAddContact dialogAddContact = new DialogAddContact();
                dialogAddContact.show(getSupportFragmentManager(),"Add Contact");

            }
        });

        // this dialog shows when user want to delete last contact and the alert mode is active
        build_create_dialog_last_contact();

        this.preferences = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_contact),getApplicationContext().MODE_PRIVATE);
        this.editor = preferences.edit();
        contactList = new ArrayList<Contact>();
        String serializedContact = preferences.getString(getApplicationContext().getResources().getString(R.string.key_contact),null);
        if (serializedContact != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<Contact>>(){}.getType();
            contactList = gson.fromJson(serializedContact,type);
        }
        this.contactListAdapter = new ContactListAdapter(this,contactList);
        listView_contact.setAdapter(this.contactListAdapter);
        final List<Contact> finalContactList = contactList;
        this.contactListAdapter.setOnItemClickListener(new ContactListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                SharedPreferences sharedPref =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
                if (sharedPref.getInt(getApplicationContext().getResources().getString(R.string.pref_key_active_mode),-1)==1 && contactList.size() == 1){
                    dialogError.show();
                }else {
                    finalContactList.remove(finalContactList.get(position));
                    Gson gson2 = new Gson();
                    String json = gson2.toJson(finalContactList);
                    editor.putString(getApplicationContext().getResources().getString(R.string.key_contact),json);
                    editor.commit();
                    contactListAdapter.notifyDataSetChanged();
                }

            }
        });


    }

    private void build_create_dialog_last_contact() {

        builder = new AlertDialog.Builder(this);
        builder.setMessage("Veuillez Desactiver le mode d'alerte pour pouvoir supprimer le dernier contact d'urgence");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        dialogError = builder.create();
    }

    private void init_toolbar(String titletoolbar) {

        toolbar = findViewById(R.id.toolbar_layout);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(titletoolbar);

        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        stopServiceFloatingWidgetService();
        appInTop = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!appInTop) startServiceFloatingWidgetService();
    }

    private void stopServiceFloatingWidgetService(){
        stopService(new Intent(getApplicationContext(), FloatingWidgetService.class));
    }

    private void startServiceFloatingWidgetService(){
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);

        SharedPreferences preferences =  getApplicationContext().getSharedPreferences(getApplicationContext().getResources().getString(R.string.pref_key_mode_alerte),getApplicationContext().MODE_PRIVATE);
        if (preferences.getInt(getString(R.string.pref_key_active_mode),-1)==1){
            if (sharedPref.getInt(getString(R.string.key_manuelle_alarme),1)==1){
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ContactActivity.this)){

                    Intent intent = new Intent(getApplicationContext(), FloatingWidgetService.class);
                    intent.putExtra("activity_background", true);
                    startService(intent);

                }else {
                    Toast.makeText(ContactActivity.this, "Draw over other app permission not available. Can't start the application without the permission.", Toast.LENGTH_LONG).show();
                }
            }

        }
    }


    @Override
    public void applyText(Contact contact) {
        List<Contact> contactList = new ArrayList<Contact>();
        String serializedContact = this.preferences.getString(getApplicationContext().getResources().getString(R.string.key_contact),null);
        if (serializedContact != null){
            Gson gson = new Gson();
            Type type = new TypeToken<List<Contact>>(){}.getType();
            contactList = gson.fromJson(serializedContact,type);
        }
        Log.i("TAG", "applyText: "+contact.getNumber());
        contactList.add(contact);
        Gson gson = new Gson();
        String json = gson.toJson(contactList);
        this.editor.putString(getApplicationContext().getResources().getString(R.string.key_contact),json);
        this.editor.commit();
        this.contactListAdapter.add(contact);
        //this.contactListAdapter.notifyDataSetChanged();

    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        appInTop = true;
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        appInTop = true;
    }
}
