package com.example.android.sip;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

public class SipSettings extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sipsetting);
        Preference button = (Preference)getPreferenceManager().findPreference("Save");      
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    finish();   
                    return true;
                }
            });
    }
}
}
