package com.base1.net;

import androidx.appcompat.app.AppCompatActivity;

import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import android.content.Context;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.CompoundButton;

import com.base1.net.util.KillThis;
import com.base1.net.util.Utils;

import android.widget.TextView;
import androidx.core.view.GravityCompat;
import android.widget.EditText;
import com.google.android.material.textfield.TextInputEditText;

import androidx.drawerlayout.widget.DrawerLayout;
import android.net.Uri;
import android.widget.Button;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import com.base1.net.activities.ConfigGeralActivity;

import androidx.appcompat.app.AlertDialog;

import com.base1.ultrasshservice.tunnel.TunnelUtils;
import com.base1.ultrasshservice.util.CustomNativeLoader;
import com.base1.ultrasshservice.logger.SkStatus;

import android.widget.LinearLayout;
import android.os.Build;
import android.app.Activity;
import com.base1.ultrasshservice.logger.ConnectionStatus;
import android.os.Handler;
import androidx.core.content.ContextCompat;
import com.base1.net.fragments.ClearConfigDialogFragment;
import com.base1.ultrasshservice.config.Settings;

import android.os.PersistableBundle;
import android.content.res.Configuration;
import android.widget.RadioGroup;
import com.base1.ultrasshservice.config.ConfigParser;

import android.content.DialogInterface;
import com.base1.ultrasshservice.tunnel.TunnelManagerHelper;
import com.base1.ultrasshservice.LaunchVpn;

import android.text.InputType;
import android.widget.ImageButton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.os.CountDownTimer;
import android.view.inputmethod.InputMethodManager;
import java.util.Locale;
import com.base1.ultrasshservice.BSE1VPNService;


import com.google.android.gms.ads.AdView;
import com.base1.net.activities.BaseActivity;

import androidx.annotation.Nullable;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Activity Principal
 * @author SlipkHunter
 */

public class BSE1VPNMainActivity extends BaseActivity
		implements DrawerLayout.DrawerListener,
		View.OnClickListener, RadioGroup.OnCheckedChangeListener,
		CompoundButton.OnCheckedChangeListener, SkStatus.StateListener,NetworkStateReceiver.NetworkStateReceiverListener
{
	private static final String TAG = BSE1VPNMainActivity.class.getSimpleName();
	private static final String UPDATE_VIEWS = "MainUpdate";
	public static final String OPEN_LOGS = "com.base1.net:openLogs";
	private static final String DNS_BIN = "libstartdns";

	private DrawerLog mDrawer;
	private DrawerPanelMain mDrawerPanel;

	private Settings mConfig;
	private Toolbar toolbar_main;
	private Handler mHandler;
	private LinearLayout mainLayout;
	private LinearLayout loginLayout;
	private LinearLayout proxyInputLayout;
	private TextView proxyText;
	private Button starterButton;

	private ImageButton inputPwShowPass;
	private TextInputEditText inputPwUser;
	private TextInputEditText inputPwPass;

	private LinearLayout configMsgLayout;
	private TextView configMsgText;

	private AdView adsBannerView;
	private Spinner servidores;
	
	private TextView mTextViewCountDown;
	private Button mButtonSet;
	private Button mButtonStartPause;
	private Button mButtonReset;
	private CountDownTimer mCountDownTimer;
	private boolean mTimerRunning;
	private long mStartTimeInMillis;
	private long mTimeLeftInMillis;
	private long mEndTime;
	private EditText mEditTextInput;

	private Process dnsProcess;
	private File filedns;


	private SwitchCompat AutoReconnectSwitch;
	private LinearLayout AutoReconnectLayout;
	private AppCompatActivity mActivity;
	
	private FloatingActionButton logs;

	private ImageView contato;
	private ConnectivityManager connMgr;

	private long mStartRX = 0;
	private long mStartTX = 0;
	private long mStartYX = 0;
	private long mStartUX = 0;
	private NetworkStateReceiver networkStateReceiver;

	// Timer Reconexão 1
	private void pauseTimer() {
		mCountDownTimer.cancel();
		mTimerRunning = false;
		updateWatchInterface();
	}
	private void resetTimer() {
		mTimeLeftInMillis = mStartTimeInMillis;
		updateCountDownText();
		updateWatchInterface();
	}

	private void updateWatchInterface() {
		if (mTimerRunning) {
			mEditTextInput.setVisibility(View.INVISIBLE);
			mButtonSet.setVisibility(View.INVISIBLE);
			mButtonReset.setVisibility(View.INVISIBLE);
			mButtonStartPause.setText("PAUSAR");
		} else {
			mTextViewCountDown.setVisibility(View.VISIBLE);
			mEditTextInput.setVisibility(View.VISIBLE);
			mButtonSet.setVisibility(View.VISIBLE);
			mButtonStartPause.setText("INICIAR");
			mButtonReset.setText("RESETAR");
			if (mTimeLeftInMillis < 1000) {
				mButtonStartPause.setVisibility(View.INVISIBLE);
				mButtonSet.setVisibility(View.INVISIBLE);
				mEditTextInput.setVisibility(View.INVISIBLE);
				mTextViewCountDown.setVisibility(View.INVISIBLE);
				mButtonReset.setText("PARAR");
			} else {
				mButtonStartPause.setVisibility(View.VISIBLE);
			}
			if (mTimeLeftInMillis < mStartTimeInMillis) {
				mButtonReset.setVisibility(View.VISIBLE);
			} else {
				mButtonReset.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void closeKeyboard() {
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("startTimeInMillis", mStartTimeInMillis);
		editor.putLong("millisLeft", mTimeLeftInMillis);
		editor.putBoolean("timerRunning", mTimerRunning);
		editor.putLong("endTime", mEndTime);
		editor.apply();

	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
		mStartTimeInMillis = prefs.getLong("startTimeInMillis", 3600000);
		mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
		mTimerRunning = prefs.getBoolean("timerRunning", false);
		updateCountDownText();
		updateWatchInterface();
		if (mTimerRunning) {
			mEndTime = prefs.getLong("endTime", 0);
			mTimeLeftInMillis = mEndTime - System.currentTimeMillis();
			if (mTimeLeftInMillis < 0) {
				mTimeLeftInMillis = 0;
				mTimerRunning = false;
				updateCountDownText();
				updateWatchInterface();
			} else {
				startTimer();
			}
		}
	}

	private void updateCountDownText() {
		int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
		int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
		int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
		String timeLeftFormatted;
		if (hours > 0) {
			timeLeftFormatted = String.format(Locale.getDefault(),
											  "%d:%02d:%02d", hours, minutes, seconds);
		} else {
			timeLeftFormatted = String.format(Locale.getDefault(),
											  "%02d:%02d", minutes, seconds);
		}
		mTextViewCountDown.setText(timeLeftFormatted);
	}

	private void setTime(long milliseconds) {
		mStartTimeInMillis = milliseconds;
		resetTimer();
		closeKeyboard();
	}

	private void startTimer() {
		mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
		mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {


			@Override
			public void onTick(long millisUntilFinished) {
				mTimeLeftInMillis = millisUntilFinished;
				updateCountDownText();
			}
			@Override
			public void onFinish() {
				mTimerRunning = false;
				updateWatchInterface();
				resetTimer();
				startTimer();

				Intent reconTunnel = new Intent(BSE1VPNService.TUNNEL_SSH_RESTART_SERVICE);
				LocalBroadcastManager.getInstance(BSE1VPNMainActivity.this).sendBroadcast(reconTunnel);

			}
		}.start();
		mTimerRunning = true;
		updateWatchInterface();
	}
 // #####################	

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mHandler = new Handler();
		mConfig = new Settings(this);
		mDrawer = new DrawerLog(this);
		mDrawerPanel = new DrawerPanelMain(this);
		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		SharedPreferences prefs = getSharedPreferences(BSE1VPNApp.PREFS_GERAL, Context.MODE_PRIVATE);

		boolean showFirstTime = prefs.getBoolean("connect_first_time", true);
		int lastVersion = prefs.getInt("last_version", 0);


		// se primeira vez
		if (showFirstTime)
		{
			SharedPreferences.Editor pEdit = prefs.edit();
			pEdit.putBoolean("connect_first_time", false);
			pEdit.apply();

			Settings.setDefaultConfig(this);

			showBoasVindas();
		}

		try {
			int idAtual = ConfigParser.getBuildId(this);

			if (lastVersion < idAtual) {
				SharedPreferences.Editor pEdit = prefs.edit();
				pEdit.putInt("last_version", idAtual);
				pEdit.apply();

				// se estiver atualizando
				if (!showFirstTime) {
					if (lastVersion <= 12) {
						Settings.setDefaultConfig(this);
						Settings.clearSettings(this);

						Toast.makeText(this, "As configurações foram limpas para evitar bugs",
								Toast.LENGTH_LONG).show();
					}
				}

			}
		} catch(IOException e) {}


		// set layout
		doLayout();

		// verifica se existe algum problema
		//SkProtect.CharlieProtect();

		// recebe local dados
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_VIEWS);
		filter.addAction(OPEN_LOGS);

		LocalBroadcastManager.getInstance(this)
				.registerReceiver(mActivityReceiver, filter);

		doUpdateLayout();

		mStartRX = TrafficStats.getTotalRxBytes();
		mStartTX = TrafficStats.getTotalTxBytes();
		mStartYX = TrafficStats.getTotalRxBytes();
		mStartUX = TrafficStats.getTotalTxBytes();
		if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Uh Oh!");
			alert.setMessage("Your device does not support traffic stat monitoring.");
			alert.show();
		} else {
			mHandler.postDelayed(mRunnable, 1000);
		}

		startNetworkBroadcastReceiver(this);
	}


	/**
	 * Layout
	 */

	private void doLayout() {
		setContentView(R.layout.activity_main_drawer);

		toolbar_main = (Toolbar) findViewById(R.id.toolbar_main);
		mDrawerPanel.setDrawer(toolbar_main);
		setSupportActionBar(toolbar_main);

		mDrawer.setDrawer(this);


		// set ADS
		adsBannerView = (AdView) findViewById(R.id.adBannerMainView);

		if (!BuildConfig.DEBUG) {
			//adsBannerView.setAdUnitId(BSE1VPNApp.ADS_UNITID_BANNER_MAIN);
		}

        mEditTextInput=(EditText)findViewById(R.id.time);
		mTextViewCountDown = (TextView) findViewById(R.id.duration);
		mButtonSet = (Button) findViewById(R.id.set);

		mButtonSet.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
				@Override
				public void onClick(View v) {
					String input = mEditTextInput.getText().toString();
					if (input.length() == 0) {
						Toast.makeText((BSE1VPNMainActivity.this), "INSIRA UM TEMPO", Toast.LENGTH_SHORT).show();
					
						return;
					}
					long millisInput = Long.parseLong(input) * 60000;
					if (millisInput == 0) {
						Toast.makeText((BSE1VPNMainActivity.this), "INSIRA UM TEMPO VALIDO", Toast.LENGTH_SHORT).show();
					
						return;
					}
					setTime(millisInput);
					mEditTextInput.setText("");
				
					startTimer();
				}
			});


		mButtonStartPause = (Button) findViewById(R.id.start);
		mButtonStartPause.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
				@Override
				public void onClick(View v) {
					if (mTimerRunning) {
						pauseTimer();
					} else {
						startTimer();
						Toast.makeText(BSE1VPNMainActivity.this, "RECONEXÃO ATIVADA", Toast.LENGTH_SHORT).show();
					}
				
				}
			});

		mButtonReset = (Button) findViewById(R.id.reset);
		mButtonReset.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);

				@Override
				public void onClick(View v) {
					resetTimer();
				
				}
			});

		mainLayout = (LinearLayout) findViewById(R.id.activity_mainLinearLayout);
		loginLayout = (LinearLayout) findViewById(R.id.activity_mainInputPasswordLayout);
		starterButton = (Button) findViewById(R.id.activity_starterButtonMain);

		inputPwUser = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordUserEdit);
		inputPwPass = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordPassEdit);

		inputPwShowPass = (ImageButton) findViewById(R.id.activity_mainInputShowPassImageButton);


		contato = (ImageView) findViewById(R.id.contato);
		contato.setOnClickListener(this);


		AutoReconnectLayout = (LinearLayout) findViewById(R.id.activity_AutoReconnectLayout);
		
		// Atual estado do Layout
		AutoReconnectLayout.setVisibility(View.INVISIBLE);

		AutoReconnectSwitch = (SwitchCompat) findViewById(R.id.activity_AutoReconnectSwitch);	

         // Atual estado do Switch
        AutoReconnectSwitch.setChecked(false);		
		AutoReconnectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                       // Quando está marcado
					   
					    AutoReconnectLayout.setVisibility(View.VISIBLE);
					    showAvisoUseTimer();
					  
                     } else {
                        // Quando estiver desmarcado

						if (mTimerRunning) {
							pauseTimer();
							resetTimer();
						}
						//updateWatchInterface();
						AutoReconnectLayout.setVisibility(View.INVISIBLE);
                     }
                }
            });
		


		
		logs = (FloatingActionButton) findViewById(R.id.logs);

		starterButton.setOnClickListener(this);

        logs.setOnClickListener(this);



		inputPwShowPass.setOnClickListener(this);

        //Onde fica salvo as coisas no aparelho
		final SharedPreferences sPrefs = mConfig.getPrefsPrivate();

        //Declarando o xml do spinner
		servidores = (Spinner) findViewById(R.id.serverSpin);

		// Aqui sao os nomes que vcs quiserem pro servidor, pode ser oq vc quiser(nao importa)
		List<String> ListaServidores = new ArrayList<String>();
		ListaServidores.add("DNSTT");
		ListaServidores.add("SSL");
		ListaServidores.add("DIRECT");


		// Criando adaptador para receber os servidores
		ArrayAdapter<String> AdptadorServidores = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, ListaServidores);

		// Definindo layout para o Adaptador
		AdptadorServidores.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Preenche o Spinner com a lista de servidores
		servidores.setAdapter(AdptadorServidores);


		//Carrega a posição salva do servidor selecionado
		servidores.setSelection(sPrefs.getInt("Servidor", 0));

		//Função ao clicar em 1 dos servidores da lista

		servidores.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> p1, View p2, int position, long p4)
			{

				try
				{
					//Salva a posição do servidor para quando entrar no app ele continuar selecionado
					sPrefs.edit().putInt("Servidor", position).apply();


					//Define as configurações do servidor de acordo com o servidor selecionado
                    if(position == 0) {
						//SERVIDOR 1 VIVO ###########################################################################################################

						//Usar payload costumizada
						sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true).apply();

						//INFO SSH
						sPrefs.edit().putString(Settings.SERVIDOR_KEY, "127.0.0.1").apply();

						//PORTA SSH
						sPrefs.edit().putString(Settings.SERVIDOR_PORTA_KEY, "2222").apply();

						//MODO DE CONEXÃO OPERADORA (DIRECT)
						sPrefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT).apply();


						//SETA SLOWDNS PREFERENCES
						sPrefs.edit().putString(Settings.SLOW_CHAVE_KEY, "dff6a0194db86396ea0e0bada400a821ce756a7546597067d2592de29e7f8116").apply();
						sPrefs.edit().putString(Settings.SLOW_NAMESERVER_KEY, "pony.anasor.com").apply();
						sPrefs.edit().putString(Settings.SLOW_DNSKEY, "8.8.8.8").apply();

						//###########################################################################################################################

					}else if(position == 1){
						//SERVIDOR 1 CLARO 3 ###########################################################################################################

						//USAR PAYLOAD PADRÃO
						sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();

						//PAYLOAD
						sPrefs.edit().putString(Settings.CUSTOM_SNI, "m.waze.com").apply();

						//PAYLOAD PRA NAO FICAR VAZIO
						sPrefs.edit().putString(Settings.CUSTOM_PAYLOAD_KEY, "m.waze.com").apply();

						//MODO DE CONEXÃO OPERADORA (SSL SNI)
						sPrefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_SSL).apply();

						//SERVIDOR
						sPrefs.edit().putString(Settings.SERVIDOR_KEY, "dc2.anasor.com").apply();

						//PORTA SSH
						sPrefs.edit().putString(Settings.SERVIDOR_PORTA_KEY, "7897").apply();
					}
					else if(position == 2){
						//SERVIDOR 1 TIM 1 ###########################################################################################################
						//USAR PAYLOAD PADRÃO
						sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();

						//PAYLOAD
						sPrefs.edit().putString(Settings.CUSTOM_PAYLOAD_KEY, "HTTP/1.1").apply();

						//SERVIDOR
						sPrefs.edit().putString(Settings.SERVIDOR_KEY, "socks.bigbolgames.com").apply();

						//PORTA SSH
						sPrefs.edit().putString(Settings.SERVIDOR_PORTA_KEY, "80").apply();

						//MODO DE CONEXÃO OPERADORA (DIRETO)
						sPrefs.edit().putInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT).apply();


					}
                    //Atualiza informações
                    doUpdateLayout();
				}
				catch (Exception e)
				{}
			}

			@Override
			public void onNothingSelected(AdapterView<?> p1)
			{

			}
		});

		//Sistema de usuario e senha na tela inicial by @BSE1VPN
		final SharedPreferences prefsTxt = mConfig.getPrefsPrivate();
		inputPwUser.setText(prefsTxt.getString(Settings.USUARIO_KEY, ""));
		inputPwPass.setText(prefsTxt.getString(Settings.SENHA_KEY, ""));
		inputPwUser.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				if(!s.toString().isEmpty()) {
					prefsTxt.edit().putString(Settings.USUARIO_KEY, s.toString()).apply();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		inputPwPass.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				if(!s.toString().isEmpty()) {
					prefsTxt.edit().putString(Settings.SENHA_KEY, s.toString()).apply();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
	}

	private void doUpdateLayout() {
		final SharedPreferences prefs = mConfig.getPrefsPrivate();

		int up = View.VISIBLE;
		boolean isRunning = SkStatus.isTunnelActive();
		setStarterButton(starterButton, this);

		boolean enabled_radio = !isRunning;

		setStarterButton(starterButton, this);
	}


	private synchronized void doSaveData() {
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		if (mainLayout != null && !isFinishing())
			mainLayout.requestFocus();


		edit.apply();
	}


	/**
	 * Tunnel SSH
	 */

	public void startOrStopTunnel(Activity activity) {
		if (SkStatus.isTunnelActive()) {
			TunnelManagerHelper.stopBSE1VPN(activity);
		}
		else {
			try {
				startdnsvoid();
			} catch (IOException e) {
				e.printStackTrace();
			}

			Settings config = new Settings(activity);

			if (config.getPrefsPrivate()
					.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
				if (inputPwUser.getText().toString().isEmpty() ||
						inputPwPass.getText().toString().isEmpty()) {
					Toast.makeText(this, R.string.error_userpass_empty, Toast.LENGTH_SHORT)
							.show();
					return;
				}
			}

			Intent intent = new Intent(activity, LaunchVpn.class);
			intent.setAction(Intent.ACTION_MAIN);

			if (config.getHideLog()) {
				intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
			}

			activity.startActivity(intent);
		}
	}



	public void setStarterButton(Button starterButton, Activity activity) {
		String state = SkStatus.getLastState();
		boolean isRunning = SkStatus.isTunnelActive();

		if (starterButton != null) {
			int resId;

			SharedPreferences prefsPrivate = new Settings(activity).getPrefsPrivate();

			if (SkStatus.SSH_INICIANDO.equals(state)) {
				resId = R.string.stop;

				starterButton.setEnabled(true);
				inputPwUser.setEnabled(false);
				inputPwPass.setEnabled(false);
				servidores.setEnabled(false);
				mDrawer.clearLogs();
			}
			else if (SkStatus.SSH_CONECTADO.equals(state)) {
				resId = R.string.stop;
				starterButton.setEnabled(true);
				inputPwUser.setEnabled(false);
				inputPwPass.setEnabled(false);
				servidores.setEnabled(false);
			 

			}
			else if (SkStatus.SSH_PARANDO.equals(state)) {
				resId = R.string.state_stopping;
				starterButton.setEnabled(true);
			}
			else if (SkStatus.SSH_DESCONECTADO.equals(state))
			{
				
				resId = R.string.start;
				starterButton.setEnabled(true);
				inputPwUser.setEnabled(true);
				inputPwPass.setEnabled(true);
				servidores.setEnabled(true);
				stopdns();
			}
			else {
				resId = isRunning ? R.string.stop : R.string.start;
				starterButton.setEnabled(true);
			}

			starterButton.setText(resId);
		}
	}



	@Override
	public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
		super.onPostCreate(savedInstanceState, persistentState);
		if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().onConfigurationChanged(newConfig);
	}

	private boolean isMostrarSenha = false;

	@Override
	public void onClick(View p1)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		switch (p1.getId()) {
			case R.id.activity_starterButtonMain:
				doSaveData();
				startOrStopTunnel(this);
				break;



			case R.id.activity_mainInputShowPassImageButton:
				isMostrarSenha = !isMostrarSenha;
				if (isMostrarSenha) {
					inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					inputPwShowPass.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_black_24dp));
				}
				else {
					inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					inputPwShowPass.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_visibility_off_black_24dp));
				}
				break;

				
			case R.id.logs:
				showLogWindow();
				break;

			case R.id.contato:
				openlink();
				break;

		}
	}

	private void openlink() {
			String link = "https://t.me/penguinehis";
			Intent intentlink = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
		     intentlink .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(Intent.createChooser(intentlink, this.getText(R.string.open_with)));

	}

	private void startdnsvoid() throws IOException {


		//Onde fica salvo as coisas no aparelho
		final SharedPreferences slowprefs = mConfig.getPrefsPrivate();

		StringBuilder cmd1 = new StringBuilder();
		filedns = CustomNativeLoader.loadNativeBinary(this, DNS_BIN, new File(this.getFilesDir(),DNS_BIN));

		if (filedns == null){
			throw new IOException("Bin DNS não encontrado");
		}



		final String chave = slowprefs.getString(Settings.SLOW_CHAVE_KEY, "slowchave");
		final String nameserver = slowprefs.getString(Settings.SLOW_NAMESERVER_KEY, "slowns");
		final String dns = slowprefs.getString(Settings.SLOW_DNSKEY, "slowdns");
		// executa comando
		cmd1.append(filedns.getCanonicalPath());
		cmd1.append(" -udp "+dns+":53   -pubkey "+chave+" "+nameserver+" 127.0.0.1:2222");

		dnsProcess = Runtime.getRuntime().exec(cmd1.toString());

		try {
			//dnsProcess.waitFor();

		} catch (Exception e) {
			//	SkStatus.logDebug("BIN Error: " + e);
		}


	}

	private void stopdns(){

		if (dnsProcess != null)
			dnsProcess.destroy();

		try {
			if (filedns != null)
				KillThis.killProcess(filedns);
		} catch (Exception e) {}

		dnsProcess = null;
		filedns = null;


	}

	@Override
	public void onCheckedChanged(RadioGroup p1, int p2)
	{
		SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();


		edit.apply();

		doSaveData();
		doUpdateLayout();
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		edit.apply();

		doSaveData();
	}

	protected void showBoasVindas() {
		new AlertDialog.Builder(this)
				. setTitle(R.string.attention)
				. setMessage(R.string.first_start_msg)
				. setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int p) {
						// ok
					}
				})
				. setCancelable(false)
				. show();
	}

	protected void showAvisoUseTimer() {
		new AlertDialog.Builder(this)
				. setTitle("Use apenas se souber!")
				. setMessage("Isto define um tempo para o app reconectar.\n\n1 - Clique em TEMPO e coloque um valor,por exemplo 5 para 5 minutos,30 para 30 minutos,etc\n2 - Clique em INSERIR\n\n4 - Agora o app vai ficar reconectando no tempo definido\n\n5 - Para parar clique em PAUSAR,ou desative o Auto Reconexão\n6 - Para limpar clique em RESETAR\n\nObs.:Isto é útil se sua internet cai ou fica lenta após um tempo(De 5 em 5 minutos,por exemplo),você pode colocar para reconectar neste tempo de 5 minutos")
				. setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int p) {
						// ok
					}
				})
				. setCancelable(false)
				. show();
	}
	
	private void showLogWindow() {
		Intent updateView = new Intent("com.base1.net:openLogs");
		LocalBroadcastManager.getInstance(this)
				.sendBroadcast(updateView);
	}

	@Override
	public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				doUpdateLayout();
			}
		});

		switch (state) {
			case SkStatus.SSH_CONECTADO:
				// carrega ads banner
				break;
		}
	}


	/**
	 * Recebe locais Broadcast
	 */

	private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null)
				return;

			if (action.equals(UPDATE_VIEWS) && !isFinishing()) {
				doUpdateLayout();
			}
			else if (action.equals(OPEN_LOGS)) {
				if (mDrawer != null && !isFinishing()) {
					DrawerLayout drawerLayout = mDrawer.getDrawerLayout();

					if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
						drawerLayout.openDrawer(GravityCompat.END);
					}
				}
			}
		}
	};


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerPanel.getToogle() != null && mDrawerPanel.getToogle().onOptionsItemSelected(item)) {
			return true;
		}

		// Menu Itens
		switch (item.getItemId()) {



			case R.id.miSettings:
				Intent intentSettings = new Intent(this, ConfigGeralActivity.class);
				//intentSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intentSettings);
				break;


			// logs opções
			case R.id.miLimparLogs:
				mDrawer.clearLogs();
				break;

			case R.id.miExit:
				if (Build.VERSION.SDK_INT >= 16) {
					finishAffinity();
				}

				System.exit(0);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout layout = mDrawer.getDrawerLayout();

		if (mDrawerPanel.getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
			mDrawerPanel.getDrawerLayout().closeDrawers();
		}
		else if (layout.isDrawerOpen(GravityCompat.END)) {
			// fecha drawer
			layout.closeDrawers();
		}
		else {
			// mostra opção para sair
			showExitDialog();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		mDrawer.onResume();

		//doSaveData();
		//doUpdateLayout();

		SkStatus.addStateListener(this);

		registerNetworkBroadcastReceiver(this);
		super.onResume();

	}

	@Override
	protected void onPause()
	{
		unregisterNetworkBroadcastReceiver(this);

		super.onPause();

		doSaveData();

		SkStatus.removeStateListener(this);

		if (adsBannerView != null) {
			adsBannerView.pause();
		}


	}


	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mDrawer.onDestroy();

		LocalBroadcastManager.getInstance(this)
				.unregisterReceiver(mActivityReceiver);

		if (adsBannerView != null) {
			adsBannerView.destroy();
		}
	}


	/**
	 * DrawerLayout Listener
	 */

	@Override
	public void onDrawerOpened(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.logs_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerClosed(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.main_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerStateChanged(int stateId) {}
	@Override
	public void onDrawerSlide(View view, float p2) {}


	/**
	 * Utils
	 */

	public static void updateMainViews(Context context) {
		Intent updateView = new Intent(UPDATE_VIEWS);
		LocalBroadcastManager.getInstance(context)
				.sendBroadcast(updateView);
	}

	public void showExitDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).
				create();
		dialog.setTitle(getString(R.string.attention));
		dialog.setMessage(getString(R.string.alert_exit));

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.
						string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Utils.exitAll(BSE1VPNMainActivity.this);
					}
				}
		);

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.
						string.minimize),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// minimiza app
						Intent startMain = new Intent(Intent.ACTION_MAIN);
						startMain.addCategory(Intent.CATEGORY_HOME);
						startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(startMain);
					}
				}
		);

		dialog.show();
	}
	private final Runnable mRunnable = new Runnable() {

		public void run() {
			TextView RX = (TextView) findViewById(R.id.RX);
			TextView TX = (TextView) findViewById(R.id.TX);
			long resetdownload=TrafficStats.getTotalRxBytes();
			long rxBytes = TrafficStats.getTotalRxBytes() - mStartRX;
			RX.setText(Long.toString(rxBytes) + " bytes");
			if(rxBytes>=1024){
				//KB or more
				long rxKb = rxBytes/1024;
				RX.setText(Long.toString(rxKb) + " KBs");
				if(rxKb>=1024){
					//MB or more
					long rxMB = rxKb/1024;
					RX.setText(Long.toString(rxMB) + " MBs");
					if(rxMB>=1024){
						//GB or more
						long rxGB = rxMB/1024;
						RX.setText(Long.toString(rxGB) + " GBs");
					}//rxMB>1024
				}//rxKb > 1024
			}//rxBytes>=1024
			mStartRX=resetdownload;

			long resetupload=TrafficStats.getTotalTxBytes();
			long txBytes = TrafficStats.getTotalTxBytes() - mStartTX;
			TX.setText(Long.toString(txBytes) + " bytes");
			if(txBytes>=1024){
				//KB or more
				long txKb = txBytes/1024;
				TX.setText(Long.toString(txKb) + " KBs");
				if(txKb>=1024){
					//MB or more
					long txMB = txKb/1024;
					TX.setText(Long.toString(txMB) + " MBs");
					if(txMB>=1024){
						//GB or more
						long txGB = txMB/1024;
						TX.setText(Long.toString(txGB) + " GBs");
					}//txMB>1024
				}//txKb > 1024
			}//txBytes>=1024
			mStartTX=resetupload;

			TextView YX = (TextView) findViewById(R.id.YX);
			TextView UX = (TextView) findViewById(R.id.UX);
			long yxBytes = TrafficStats.getTotalRxBytes() - mStartYX;
			YX.setText(Long.toString(yxBytes) + " bytes");
			if(yxBytes>=1024){
				//KB or more
				long yxKb = yxBytes/1024;
				YX.setText(Long.toString(yxKb) + " KB");
				if(yxKb>=1024){
					//MB or more
					long yxMB = yxKb/1024;
					YX.setText(Long.toString(yxMB) + " MB");
					if(yxMB>=1024){
						//GB or more
						long yxGB = yxMB/1024;
						YX.setText(Long.toString(yxGB) + " GB");
					}//yxMB>1024
				}//yxKb > 1024
			}//yxBytes>=1024

			long uxBytes = TrafficStats.getTotalTxBytes() - mStartUX;
			UX.setText(Long.toString(uxBytes) + " bytes");
			if(uxBytes>=1024){
				//KB or more
				long uxKb = uxBytes/1024;
				UX.setText(Long.toString(uxKb) + " KB");
				if(uxKb>=1024){
					//MB or more
					long uxMB = uxKb/1024;
					UX.setText(Long.toString(uxMB) + " MB");
					if(uxMB>=1024){
						//GB or more
						long uxGB = uxMB/1024;
						UX.setText(Long.toString(uxGB) + " GB");
					}//uxMB>1024
				}//uxKb > 1024
			}//uxBytes>=1024
			mHandler.postDelayed(mRunnable, 1000);
		}
	};

	protected String getIpPublic() {

		final android.net.NetworkInfo network = connMgr
				.getActiveNetworkInfo();

		if (network != null && network.isConnectedOrConnecting()) {
			return TunnelUtils.getLocalIpAddress();
		}
		else {
			return "Indisponivel";
		}
	}

	public void startNetworkBroadcastReceiver(Context currentContext) {
		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener((NetworkStateReceiver.NetworkStateReceiverListener) currentContext);
		registerNetworkBroadcastReceiver(currentContext);
	}

	/**
	 * Register the NetworkStateReceiver with your activity
	 * @param currentContext
	 */
	public void registerNetworkBroadcastReceiver(Context currentContext) {
		currentContext.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	/**
	 Unregister the NetworkStateReceiver with your activity
	 * @param currentContext
	 */
	public void unregisterNetworkBroadcastReceiver(Context currentContext) {
		currentContext.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public void networkAvailable() {
		TextView iplocal = (TextView) findViewById(R.id.iplocal);
		TextView networktype = (TextView) findViewById(R.id.networktype);
		Log.i(TAG, "networkAvailable()");
		TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String carrierName = manager.getNetworkOperatorName();
		String ipatual = (String.valueOf(getIpPublic()));
		iplocal.setText(ipatual);
		networktype.setText(carrierName);


	}

	@Override
	public void networkUnavailable() {
		TextView iplocal = (TextView) findViewById(R.id.iplocal);
		TextView networktype = (TextView) findViewById(R.id.networktype);
		Log.i(TAG, "networkUnavailable()");
		iplocal.setText("Desconectado!");
		networktype.setText("Sem Rede!");

	}
}


