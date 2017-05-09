package ba.ima.hepek;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import ba.ima.hepek.effects.SOSFlash;
import ba.ima.hepek.effects.SOSVibrate;
import ba.ima.hepek.utils.SoundThread;

/**
 * Main application activity with all UI
 * 
 * @author ZeKoU - amerzec@gmail.com
 * @author Gondzo - gondzo@gmail.com
 * @author NarDev - valajbeg@gmail.com
 */

public class MainActivity extends Activity implements SurfaceHolder.Callback {

	/* Used as logging ID */
	private static final String ACTIVITY = MainActivity.class.getSimpleName();

	/* UI elements */
	private Button hepekButton;

	private Button ledWhite;

	private Button ledBlue;

	private Button ledGreen;

	private Button ledRed;

	// FlashThread flashThread = null;
	private Camera mCamera;
	public static SurfaceView preview;
	public static SurfaceHolder mHolder;
	private MqttAndroidClient mqttAndroidClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		init();

	}

	/* Load UI components */
	private void init() {

		preview = (SurfaceView) findViewById(R.id.camSurface);
		mHolder = preview.getHolder();
		mHolder.addCallback(this);

		this.hepekButton = (Button) this.findViewById(R.id.hepekBtn);
		this.hepekButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				// Play sound
				Log.d(ACTIVITY, "Hepek sound effect invoked...");
				new SoundThread(MainActivity.this, R.raw.hepek_sound_effect,
						MainActivity.this).start();

				// Vibrate
				Log.d(ACTIVITY, "Hepek vibrate effect invoked...");
				new SOSVibrate().execute(new Activity[] { MainActivity.this });

				// Flash SOS
				Log.d(ACTIVITY, "Hepek flash effect invoked...");
				new SOSFlash(mCamera).execute(getApplicationContext());
                doPublish();

			}

		});

	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d("MAIN", "onPause()");
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		try {
			Log.d("FLASH", "Starting camera preview");
			mCamera = Camera.open();
			mCamera.startPreview();
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {
			Log.e("FLASH", "Error starting camera preview!");
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// Not interested in this because we hide cam preview surface anyways.
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mHolder = holder;
		try {
			mCamera.setPreviewDisplay(mHolder);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// Not interested in this because we hide cam preview surface anyways.
	}

	private void doPublish() {

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);


		String username="mosquitto";
		String password="Sarajevo!1984%";
		String serverUri = "tcp://hepek.ba:1883";

		final String publishTopic = "wccontrol";
		final String publishMessage="pustiVodu";


		String clientId = "clientId";
		clientId = clientId + System.currentTimeMillis();

		mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
		mqttAndroidClient.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {

                //Toast.makeText(MainActivity.this,"Connected to mqtt server. Sending message.",Toast.LENGTH_SHORT).show();
				try {
					MqttMessage message = new MqttMessage();
					message.setPayload(publishMessage.getBytes());
					mqttAndroidClient.publish(publishTopic, message);
				} catch (MqttException e) {
					Log.d("",("Error Publishing: " + e.getMessage()));
                    //Toast.makeText(MainActivity.this,"Error publishing message",Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void connectionLost(Throwable cause) {
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				try {
                    //Toast.makeText(MainActivity.this,"Message published.",Toast.LENGTH_SHORT).show();
					mqttAndroidClient.disconnect();
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		});

		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		mqttConnectOptions.setCleanSession(false);
		if (username.length()>0)
			mqttConnectOptions.setUserName(username);
		if (password.length()>0)
			mqttConnectOptions.setPassword(password.toCharArray());







		try {
			//addToHistory("Connecting to " + serverUri);
			mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
                    //Toast.makeText(MainActivity.this,"Connected to mqtt server.",Toast.LENGTH_SHORT).show();
					DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
					disconnectedBufferOptions.setBufferEnabled(true);
					disconnectedBufferOptions.setBufferSize(100);
					disconnectedBufferOptions.setPersistBuffer(false);
					disconnectedBufferOptions.setDeleteOldestMessages(false);
					mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Log.d("","Failed to connect");
                    //Toast.makeText(MainActivity.this,"Failed to connect to mqtt server",Toast.LENGTH_SHORT).show();
				}
			});


		} catch (MqttException ex){
			ex.printStackTrace();
            //Toast.makeText(MainActivity.this,"Failed to connect to mqtt server",Toast.LENGTH_SHORT).show();
		}
	}


}
