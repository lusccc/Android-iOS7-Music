package com.stark.music.activity;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.stark.database.DBHelperofPlayState;
import com.stark.music.R;
import com.stark.music.fragment.main.FragmentFactory;
import com.stark.service.PlayerService;

public class MainActivity extends Activity implements SensorEventListener {
	private FragmentManager fragmentManager;
	private RadioGroup radioGroup;
	private RadioButton radioButton;
	private static int screenWidth, screenHeight;

	private SensorManager mSensorManager;
	private Sensor mOrientationSensor;

	private boolean hasResumed = false;

	public static float zAxis;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_frame);
		new DBHelperofPlayState(this).getWritableDatabase();
		fragmentManager = getFragmentManager();
		radioGroup = (RadioGroup) findViewById(R.id.rg_tab);
		resetRadioButtonID();
		radioGroup.getBackground().setAlpha(192);
		radioButton = (RadioButton) radioGroup.getChildAt(0);
		radioButton.setChecked(true);
		init();
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mOrientationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		radioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId != 1) {
							radioButton.setChecked(false);
						}
						FragmentTransaction transaction = fragmentManager
								.beginTransaction();
						Fragment fragment = FragmentFactory
								.getInstanceByIndex(checkedId);
						transaction.replace(R.id.content, fragment);
						transaction.commitAllowingStateLoss();
					}
				});
	}

	@Override
	protected void onResume() {
		super.onResume();
		hasResumed = true;
		/**
		 * 强制设置为竖屏
		 */
		mSensorManager.registerListener(MainActivity.this,
				mOrientationSensor, SensorManager.SENSOR_DELAY_NORMAL);
		/*new Thread(new Runnable() {
			public void run() {
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				mSensorManager.registerListener(MainActivity.this,
						mOrientationSensor, SensorManager.SENSOR_DELAY_NORMAL);

			}
		}).start();*/
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	/**
	 * 重置radiobutton的ID...，这坑爹的Bug
	 */
	private void resetRadioButtonID() {
		for (int i = 1; i <= 4; i++) {
			radioGroup.getChildAt(i - 1).setId(i);
		}
	}

	/**
	 * 初始化到第一个fragment
	 */
	public void init() {
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		Fragment fragment = FragmentFactory.getInstanceByIndex(1);
		transaction.replace(R.id.content, fragment);
		transaction.commit();

	}

	/**
	 * 退出程序
	 */
	private void exit() {
		
		  Intent intent = new Intent(this, PlayerService.class);
		  this.stopService(intent);
		 
		this.finish();
	}

	@Override
	protected void onDestroy() {

		 //exit();
		//android.os.Process.killProcess(android.os.Process.myPid());

		Log.e("", " mainactivity ondestory");
		super.onDestroy();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		WindowManager wm = getWindowManager();
		screenWidth = wm.getDefaultDisplay().getWidth();
		screenHeight = wm.getDefaultDisplay().getHeight();
		

	}

	public static int getScreenWidth() {
		return screenWidth;
	}

	public static int getScreenHeight() {
		return screenHeight;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// 自动旋转。0为关闭 1为开启
		// 得到是否开启
		int flag = 0;
		flag = Settings.System.getInt(getContentResolver(),
				Settings.System.ACCELEROMETER_ROTATION, 0);
		if (flag == 0) {
			return;
		}

		float roll = event.values[2];
		float y = event.values[1];

		
		/*  Log.e("", event.values[0] + " " + event.values[1] + " " +
		  event.values[2])*/;
		 

		zAxis = event.values[2]; // 控制旋转图片方向
		if (y > -30 && Math.abs(roll) > 30) {
			Intent intent = new Intent(MainActivity.this,
					GridViewActivity.class);
			if (hasResumed && screenHeight != 0 && screenWidth != 0)
			startActivity(intent);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

}
