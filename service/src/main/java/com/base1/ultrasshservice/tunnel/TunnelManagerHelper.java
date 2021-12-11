package com.base1.ultrasshservice.tunnel;

import android.content.Intent;
import android.os.Build;
import android.content.Context;
import com.base1.ultrasshservice.BSE1VPNService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class TunnelManagerHelper
{
	public static void startBSE1VPN(Context context) {
        Intent startVPN = new Intent(context, BSE1VPNService.class);
		
		if (startVPN != null) {
			TunnelUtils.restartRotateAndRandom();
			
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
			//noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);
        }
    }
	
	public static void stopBSE1VPN(Context context) {
		Intent stopTunnel = new Intent(BSE1VPNService.TUNNEL_SSH_STOP_SERVICE);
		LocalBroadcastManager.getInstance(context)
			.sendBroadcast(stopTunnel);
	}
}
