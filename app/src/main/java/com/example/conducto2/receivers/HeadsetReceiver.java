package com.example.conducto2.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class HeadsetReceiver extends BroadcastReceiver {
    private int state;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            state = intent.getIntExtra("state", -1);
            int microphone = intent.getIntExtra("microphone",0);

            switch (state) {
                case 0:
                    Toast.makeText(context, "headset unplugged",Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    Toast.makeText(context, "headset plugged , with microphone: " + (microphone==1) ,Toast.LENGTH_SHORT).show();
                    break;

                default:
                    Toast.makeText(context, "unknown state ",Toast.LENGTH_SHORT).show();
            }
        }

    }
    public int getState() {
        return state;
    }
}
