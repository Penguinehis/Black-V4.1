package com.base1.net;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;

import com.base1.ultrasshservice.BSE1VPNCore;
import com.base1.ultrasshservice.config.Settings;
import com.google.android.gms.ads.MobileAds;
import android.content.res.Configuration;

/**
* App
*/
public class BSE1VPNApp extends Application
{
	private static final String TAG = BSE1VPNApp.class.getSimpleName();
	public static final String PREFS_GERAL = "BSE1VPNGERAL";
	
	public static final String ADS_UNITID_INTERSTITIAL_MAIN = "noads";
	public static final String ADS_UNITID_BANNER_MAIN = "noads";
	public static final String ADS_UNITID_BANNER_SOBRE = "noads";
	public static final String ADS_UNITID_BANNER_TEST = "noads";
	public static final String APP_FLURRY_KEY = "RQQ8J9Q2N4RH827G32X9";
	
	private static BSE1VPNApp mApp;
	
	@Override
	public void onCreate()
	{
		super.onCreate();
		
		mApp = this;
		
		// captura dados para análise
		/*new FlurryAgent.Builder()
			.withCaptureUncaughtExceptions(true)
            .withIncludeBackgroundSessionsInMetrics(true)
            .withLogLevel(Log.VERBOSE)
            .withPerformanceMetrics(FlurryPerformance.ALL)
			.build(this, APP_FLURRY_KEY);*/
			
		// inicia
		BSE1VPNCore.init(this);
		
		// protege o app
		//SkProtect.init(this);
		
		// Initialize the Mobile Ads SDK.
        MobileAds.initialize(this);
		
		// modo noturno
	}
	
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		//LocaleHelper.setLocale(this);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//LocaleHelper.setLocale(this);
	}

	
	public static BSE1VPNApp getApp() {
		return mApp;
	}
}
