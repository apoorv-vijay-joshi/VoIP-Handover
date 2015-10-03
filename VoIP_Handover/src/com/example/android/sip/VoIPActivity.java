package com.example.android.sip;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.net.sip.*;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.ParseException;


public class VoIPActivity extends Activity implements View.OnTouchListener{

    public String sipAddress = null;
    public String old_sipaddress=null;

    public SipManager sipmanager = null;
    public SipProfile profile = null;
    public SipAudioCall call = null;
    public Calling caller;
   
    private static final int CALL_ADDRESS = 1;
    private static final int AUTHENTICATE = 2;
    private static final int SETTINGS = 3;
    private static final int END_CALL = 4;
  

  
	@Override
    public void onCreate(Bundle savedInstanceState) 
	{

        super.onCreate(savedInstanceState);
        setContentView(R.layout.voiphandover);

        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
 
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.SipDemo.INCOMING_CALL");
        caller = new Calling();
        this.registerReceiver(caller, filter);
       
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeManager();
    }

    @Override
    public void onStart() 
    {
       super.onStart();    
        initializeManager();

        if(sipAddress==old_sipaddress && old_sipaddress!=null ) 
        {
            initiateCall();

            updateStatus("Registering_again_OnStart");

        }
    }

   
    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        if (call != null) 
        {
            call.close();
        }

        closeLocalProfile();

        if (caller != null) 
        {
            this.unregisterReceiver(caller);
        }
    }

    public void initializeManager() 
    {
        if(sipmanager == null) 
        {
          sipmanager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }

   
    @SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public void initializeLocalProfile()
    {
        if (sipmanager == null)
        {
            return;
        }

        if (profile != null)
        {
            closeLocalProfile();
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = preferences.getString("namePref", "");
        String domain = preferences.getString("domainPref", "");
        String password = preferences.getString("passPref", "");
        String auth_name = preferences.getString("authnamePref","");
        String outbound_proxy = preferences.getString("proxyPref", "");

       if (username.length() == 0 || domain.length() == 0 || password.length() == 0 || auth_name.length()==0 || outbound_proxy.length()==0)
       {
            showDialog(SETTINGS);
            return;
       }

       try {
        	 SipProfile.Builder builder = new SipProfile.Builder(username, domain);
             builder.setPassword(password);
             builder.setAuthUserName(auth_name);
             builder.setOutboundProxy(outbound_proxy);
             profile = builder.build();

            Intent i = new Intent();
            i.setAction("android.SipDemo.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
            sipmanager.open(profile, pi, null);

            sipmanager.setRegistrationListener(profile.getUriString(), new SipRegistrationListener() 
            {
             @Override
			 public void onRegistering(String localProfileUri)
             {
               updateStatus("Trying to connect to SIP Server");
             }

             @Override
			 public void onRegistrationDone(String localProfileUri, long expiryTime) 
             {
               updateStatus("Registered");
             }

             @Override
			 public void onRegistrationFailed(String localProfileUri, int errorCode,String errorMessage) 
             {
               updateStatus("Registration failed.  Please check settings.");
             }
           });
        } catch (ParseException pe) {
            updateStatus("Connection Error.");
        } catch (SipException se) {
            updateStatus("Connection error.");
        }
    }
    
    public void closeLocalProfile() 
    {
        if (sipmanager == null)
        {
            return;
        }
        try {
            if (profile != null) {
                sipmanager.close(profile.getUriString());
            }
        } catch (Exception ee) {
            Log.d("VoIPActivity/onDestroy", "Failed to close local profile.", ee);
        }
    }

    public void initiateCall() 
    {
    	updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                
                @Override
                public void onCallEstablished(SipAudioCall call) 
                {
              	    call.startAudio();
                    call.setSpeakerMode(true);
                    updateStatus(call);
                    updateStatus("Ongoing Call");

                }

                @Override
                public void onCallEnded(SipAudioCall call)
                {
                    updateStatus("Ready.");
                }

                @Override
                public void onError(SipAudioCall call, int errorCode,String errorMessage) 
                {

                    if (errorCode==-10)
                    {
                    	 initiateCall();
                        updateStatus("handover");

                    }
                }

            };
            call = sipmanager.makeAudioCall(profile.getUriString(), sipAddress, listener, 50);

        }
        catch (Exception e) {
            Log.i("VoIPActivity/InitiateCall", "Manager not closed", e);
            if (profile != null) {
                try {
                    sipmanager.close(profile.getUriString());
                } catch (Exception ee) {
                    Log.i("VoIPActivity/InitiateCall", "Manager not closed", ee);
                    ee.printStackTrace();
                }
       }
       if (call != null)
       {
                call.close();
       }
     }
   }

   public void updateStatus(final String status)
   {
        
        this.runOnUiThread(new Runnable() {
            @Override
			public void run() 
            {
                TextView labelView = (TextView) findViewById(R.id.sipLabel);
                labelView.setText(status);
            }
       });
    }

    public void updateStatus(SipAudioCall call)
    {
        String user = call.getPeerProfile().getDisplayName();
        if(user == null) 
        {
          user = call.getPeerProfile().getUserName();
        }
        updateStatus(user+"@"+call.getPeerProfile().getSipDomain());
    }


    @Override
	public boolean onTouch(View v, MotionEvent event) 
    {
        if (call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call != null && call.isMuted()) {
            call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
            call.toggleMute();
        }
        return false;
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, CALL_ADDRESS, 0, "Call");
        menu.add(0, AUTHENTICATE, 0, "Account");
        menu.add(0, END_CALL, 0, "End");

        return true;
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) 
        {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case AUTHENTICATE:
                updatePreferences();
                break;
            case END_CALL:
                if(call != null) 
                {
                    try {
                      call.endCall();
                      sipAddress=null;
                      updateStatus("Call_ended");
                    } catch (SipException se) {
                        Log.d("VoIP/onOptionsItemSelected",
                                "Error", se);
                    }
                    call.close();
                }
                break;
        }
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id) 
        {
            case CALL_ADDRESS:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.makecall, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Make Call")
                        .setView(textBoxView)
                        .setPositiveButton(
                         android.R.string.ok, new DialogInterface.OnClickListener() {
                         @Override
						 public void onClick(DialogInterface dialog, int whichButton) {
                                EditText textField = (EditText)
                                (textBoxView.findViewById(R.id.calladdress_edit));
                                sipAddress = textField.getText().toString();
                                old_sipaddress=sipAddress;
                                initiateCall();

                                }
                        })
                        .setNegativeButton(
                               android.R.string.cancel, new DialogInterface.OnClickListener() {
                               @Override
							public void onClick(DialogInterface dialog, int whichButton) {
                                       
                            }
                        }).create();

            case SETTINGS:
                return new AlertDialog.Builder(this)
                        .setMessage("Update SIP Account Details")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                         @Override
						public void onClick(DialogInterface dialog, int whichButton) 
                        {
                              updatePreferences();
                        }
                        })
                        .setNegativeButton(
                        android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
						public void onClick(DialogInterface dialog, int whichButton) 
                        {
                                    
                        }
                        }).create();
        }
        return null;
    }

    public void updatePreferences()
    {
        Intent settingsActivity = new Intent(getBaseContext(),
                SipSettings.class);
        startActivity(settingsActivity);
    }
}
