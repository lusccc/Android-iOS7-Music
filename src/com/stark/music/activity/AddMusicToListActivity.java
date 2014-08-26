package com.stark.music.activity;

import com.stark.music.R;
import com.stark.music.fragment.addmusic.FragmentFactoryInAddActivity;
import com.stark.music.fragment.main.FragmentFactory;
import com.stark.service.PlayerService;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class AddMusicToListActivity extends Activity {
	private FragmentManager fragmentManager;
	private RadioGroup radioGroup;
	private RadioButton radioButton;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_add_frame);
		/** 设置进入动画***/
		overridePendingTransition(R.anim.popup_enter, R.anim.empty);
		
		fragmentManager = getFragmentManager();
		radioGroup = (RadioGroup) findViewById(R.id.rg_tab);
		radioGroup.setVisibility(View.GONE);
		resetRadioButtonID();
		radioButton = (RadioButton) radioGroup.getChildAt(0);
		radioButton.setChecked(true);
		
		init();
		radioGroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId != 1) {
							radioButton.setChecked(false);
						}
						FragmentTransaction transaction = fragmentManager
								.beginTransaction();
						Fragment fragment = FragmentFactoryInAddActivity
								.getInstanceByIndex(checkedId);
						transaction.replace(R.id.content, fragment);
						transaction.commit();
					}
				});
	}

	/**
	 * 初始化到第一个fragment
	 */
	public void init() {
		
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		Fragment fragment = FragmentFactoryInAddActivity.getInstanceByIndex(2);
		transaction.replace(R.id.content, fragment);
		transaction.commit();
	}
	/**
	 * 重置radiobutton的ID...，这坑爹的Bug
	 */
	private void resetRadioButtonID(){
		for(int i =1;i<=4;i++){
			radioGroup.getChildAt(i - 1).setId(i);
		}
	}
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		/** 设置出去动画***/
		overridePendingTransition(R.anim.empty, R.anim.popup_exit);
		super.onPause();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		/** 设置进入动画***/
		overridePendingTransition(R.anim.popup_enter, R.anim.empty);
		super.onResume();
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

		 exit();
		//android.os.Process.killProcess(android.os.Process.myPid());

		Log.e("", "ondestory");
		super.onDestroy();
	}
}
