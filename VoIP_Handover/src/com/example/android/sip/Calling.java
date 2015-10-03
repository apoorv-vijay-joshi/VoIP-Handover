package com.example.android.sip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.sip.*;

public class Calling extends BroadcastReceiver 
{
    
    @Override
    public void onReceive(Context context, Intent intent)
    {
        SipAudioCall caller = null;
        try {

            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onRinging(SipAudioCall call, SipProfile caller) {
                    try {
                        call.answerCall(30);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };

            VoIPActivity voip = (VoIPActivity) context;

            caller = voip.sipmanager.takeAudioCall(intent, listener);
            caller.answerCall(50);
            caller.startAudio();
            caller.setSpeakerMode(true);
            if(caller.isMuted()) 
            {
                caller.toggleMute();
            }

            voip.call = caller;

            voip.updateStatus(caller);

        } catch (Exception e) {
            if (caller != null) 
            {
                caller.close();
            }
    }
    }

}
