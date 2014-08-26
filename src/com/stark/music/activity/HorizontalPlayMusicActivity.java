package com.stark.music.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.HorizontalPlayActivityListViewAdapter;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.service.PlayerService;
import com.stark.util.MediaUtil;
import com.stark.view.ElasticListView;

public class HorizontalPlayMusicActivity extends Activity implements
		SensorEventListener {

	private SensorManager mSensorManager;
	private Sensor mOrientationSensor;
	private TextView musicTitle = null;
	private TextView musicArtist = null;
	private ImageView previousBtn; // 上一首
	private ImageView playBtn; // 播放（播放、暂停）
	private ImageView nextBtn; // 下一首
	private ImageView artwork_front;
	private ImageView artwork_behind;
	private RelativeLayout artworkRL;
	private ElasticListView elasticListView;
	private HorizontalPlayActivityListViewAdapter adapter;

	private String title; // 歌曲标题
	private String artist; // 歌曲艺术家
	private String album;
	private String url; // 歌曲路径
	private int listPosition; // 播放歌曲在mp3Infos的位置
	private int currentTime; // 当前歌曲播放时间
	private int duration; // 歌曲长度
	private int flag; // 播放标识
	private int artworklayoutHeight;
	private int artworkLayoutWidth;

	private int playAction = 0;
	private int repeatState;
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isPlaying = false; // 正在播放
	private boolean isPause = false; // 暂停
	private boolean isNoneShuffle; // 顺序播放
	private boolean isShuffle; // 随机播放

	private static List<Mp3Info> mp3Infos, mp3InfosForList;

	private PlayerReceiver playerReceiver;
	private UpdateListReceiver updateListReceiver;
	public static final String UPDATE_ACTION = "com.stark.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.stark.action.CTL_ACTION"; // 控制动作
	public static final String MUSIC_CURRENT = "com.stark.action.MUSIC_CURRENT"; // 音乐当前时间改变动作
	public static final String MUSIC_DURATION = "com.stark.action.MUSIC_DURATION";// 音乐播放长度改变动作
	public static final String MUSIC_PLAYING = "com.stark.action.MUSIC_PLAYING"; // 音乐正在播放动作
	public static final String REPEAT_ACTION = "com.stark.action.REPEAT_ACTION"; // 音乐重复播放动作
	public static final String SHUFFLE_ACTION = "com.stark.action.SHUFFLE_ACTION";// 音乐随机播放动作
	public static final String SHOW_LRC = "com.stark.action.SHOW_LRC"; // 通知显示歌词

	private AudioManager am; // 音频管理引用，提供对音频的控制
	private int currentVolume; // 当前音量
	private int maxVolume; // 最大音量
	private Cursor cursor, dbCursor;

	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	// 音量面板显示和隐藏动画

	/** 暂停状态下换歌的回调类 **/
	private PauseCaller pauseCaller;
	/** 换歌时的Intent **/
	private Intent actionIntent;
	/** 进度条改变时的Intent **/
	private Intent progressIntent;
	/** 暂停状态下换歌后等待播放 **/
	private boolean isWaitForPlay = false;
	private boolean isProgressChangeByUser = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if(MainActivity.zAxis > 0){
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}else{
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
		
		setContentView(R.layout.horizontal_play_activity_layout);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mOrientationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		registerReceiver();
		findViewById();
		getDataFromBundle();
		queryForList();
		getMp3InfosForList();
		setPhoneListener();
		setSystemAudioMgr();
		initView(); // 初始化视图
		setupView();
		setViewOnclickListener();

	}

	/**
	 * 添加来电监听事件
	 */
	public void setPhoneListener() {
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); // 获取系统服务
		telManager.listen(new MobliePhoneStateListener(),
				PhoneStateListener.LISTEN_CALL_STATE);
	}

	/**
	 * 获得系统音频管理服务对象
	 */
	public void setSystemAudioMgr() {
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		am.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
	}

	/**
	 * 定义和注册广播接收器
	 */
	private void registerReceiver() {
		playerReceiver = new PlayerReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_ACTION);
		filter.addAction(MUSIC_CURRENT);
		filter.addAction(MUSIC_DURATION);
		getApplicationContext().registerReceiver(playerReceiver, filter);

		updateListReceiver = new UpdateListReceiver();
		IntentFilter filter2 = new IntentFilter();
		filter2.addAction("update_list");
		getApplicationContext().registerReceiver(updateListReceiver, filter2);
	}

	private void findViewById() {
		previousBtn = (ImageView) findViewById(R.id.imageView_playbtn_previous);
		playBtn = (ImageView) findViewById(R.id.imageView_playbtn_pause);
		nextBtn = (ImageView) findViewById(R.id.imageView_playbtn_next);
		artwork_front = (ImageView) findViewById(R.id.imageView_artwork_front);
		artwork_behind = (ImageView) findViewById(R.id.imageView_artwork_behind);
		elasticListView = (ElasticListView) findViewById(R.id.elasticListView_horizontal);

	}

	/**
	 * 初始化界面
	 */
	public void initView() {
		isPlaying = true;
		isPause = false;
		Mp3Info mp3Info = mp3Infos.get(listPosition);
		adapter = new HorizontalPlayActivityListViewAdapter(this, cursor);
		switch (repeatState) {
		case isCurrentRepeat: // 单曲循环
			break;
		case isAllRepeat: // 全部循环
			break;
		case isNoneRepeat: // 无重复
			break;
		}
		if (isShuffle) {// 随机播放状态
			isNoneShuffle = false;// 顺序播放为false
		} else {
			isNoneShuffle = true;
		}
		if (flag == AppConstant.PlayerMsg.PLAYING_MSG) { // 如果播放信息是正在播放
			Intent intent = new Intent();
			intent.setAction(SHOW_LRC);
			intent.putExtra("listPosition", listPosition);
			sendBroadcast(intent);
		} else if (flag == AppConstant.PlayerMsg.PLAY_MSG) { // 如果是点击列表播放歌曲的话
			play();
		} else if (flag == AppConstant.PlayerMsg.CONTINUE_MSG) {
			Intent intent = new Intent(HorizontalPlayMusicActivity.this,
					PlayerService.class);
			intent.setAction("com.stark.media.MUSIC_SERVICE");
			intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG); // 继续播放音乐
			intent.putExtra("mp3Infos", (Serializable) mp3Infos);
			startService(intent);
		}
	}

	public void setupView() {
		elasticListView.addHeaderView(getHeaderView());
		elasticListView.setAdapter(adapter);
		elasticListView.setOnItemClickListener(new ListClickListener());
	}

	public void queryForList() {
		// 获取歌曲详细信息
		String[] colums = { MediaStore.Audio.Media.DATA,// 歌曲文件的路径
				MediaStore.Audio.Media._ID,// 歌曲ID
				MediaStore.Audio.Media.TITLE,// 歌曲标题
				MediaStore.Audio.Media.ARTIST,// 歌曲的歌手名
				MediaStore.Audio.Media.ALBUM,// 歌曲的唱片集
				MediaStore.Audio.Media.DURATION, // 歌曲的总播放时长
				MediaStore.Audio.Media.ALBUM_ID };
		String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";
		String whereVal[] = { album };
		cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colums, where,
				whereVal, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

	}

	/***
	 * 构造专辑内歌曲的集合
	 */
	public void getMp3InfosForList() {
		mp3InfosForList = new ArrayList<Mp3Info>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToPosition(i);
			Mp3Info mp3Info = new Mp3Info();
			long id = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID)); // 音乐id
			String title = cursor.getString((cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题
			String artist = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 艺术家
			String album = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM)); // 专辑
			long albumId = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			long duration = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长
			String url = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径

			mp3Info.setId(id);
			mp3Info.setTitle(title);
			mp3Info.setArtist(artist);
			mp3Info.setAlbum(album);
			mp3Info.setAlbumId(albumId);
			mp3Info.setDuration(duration);
			mp3Info.setUrl(url);
			mp3InfosForList.add(mp3Info);
		}
	}

	public View getHeaderView() {
		View headerView = LayoutInflater.from(this).inflate(
				R.layout.horizontal_play_activity_list_headerview, null);
		TextView albumTitle = (TextView) headerView
				.findViewById(R.id.textView_title);
		TextView albumDetail = (TextView) headerView
				.findViewById(R.id.textView_detail);
		albumTitle.setText(album);
		albumDetail.setText(cursor.getCount() + " 首歌");
		return headerView;
	}

	/**
	 * 给每一个按钮设置监听器
	 */
	private void setViewOnclickListener() {
		ViewOnclickListener viewOnClickListener = new ViewOnclickListener();
		previousBtn.setOnClickListener(viewOnClickListener);
		playBtn.setOnClickListener(viewOnClickListener);
		nextBtn.setOnClickListener(viewOnClickListener);
		artwork_behind.setOnClickListener(viewOnClickListener);
		artwork_front.setOnClickListener(viewOnClickListener);
	}

	private class ListClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0)
				return;

			dbHelperofPlayState = new DBHelperofPlayState(
					HorizontalPlayMusicActivity.this);
			db = dbHelperofPlayState.getWritableDatabase();
			dbCursor = db.query("play_state", null, null, null, null, null,
					null);
			dbCursor.moveToNext();
			repeatState = dbCursor.getInt(dbCursor
					.getColumnIndex("repeat_state"));
			int isShuffleINT = dbCursor.getInt(dbCursor
					.getColumnIndex("is_shuffle"));
			if (isShuffleINT == 0) {
				isShuffle = false;
			} else {
				isShuffle = true;
			}
			cursor.moveToPosition(position);
			Mp3Info mp3Info = mp3InfosForList.get(position - 1);

			Intent intent = new Intent();
			intent.setAction("com.stark.media.MUSIC_SERVICE");
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("listPosition", listPosition);
			intent.putExtra("MSG", flag);
			intent.putExtra("mp3Infos", (Serializable) mp3Infos);
			startService(intent);
		}

	}

	/**
	 * 控件点击事件
	 */
	private class ViewOnclickListener implements OnClickListener {
		Intent intent = new Intent();

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.imageView_playbtn_pause:
				if (isPlaying) {
					playBtn.setImageResource(R.drawable.play_play_white);
					intent.setAction("com.stark.media.MUSIC_SERVICE");
					intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
					intent.putExtra("mp3Infos", (Serializable) mp3Infos);
					intent.putExtra("listPosition", listPosition);
					startService(intent);
					isPlaying = false;
					isPause = true;
					isWaitForPlay = true;
				} else if (isPause) {
					playBtn.setImageResource(R.drawable.play_pause_white_selector);
					if (isWaitForPlay) {
						if (pauseCaller != null) {
							pauseCaller.pauseCall();
						}
						isWaitForPlay = false;
						isPause = false;
						isPlaying = true;
						intent.setAction("com.stark.media.MUSIC_SERVICE");
						intent.putExtra("MSG",
								AppConstant.PlayerMsg.CONTINUE_MSG);
						intent.putExtra("mp3Infos", (Serializable) mp3Infos);
						startService(intent);
						return;
					}
					if (isProgressChangeByUser) {
						stopService(progressIntent);
						startService(progressIntent);
						isProgressChangeByUser = false;
						isPause = false;
						isPlaying = true;
						return;
					}
					intent.setAction("com.stark.media.MUSIC_SERVICE");
					intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
					intent.putExtra("mp3Infos", (Serializable) mp3Infos);
					startService(intent);
				}
				break;
			case R.id.imageView_playbtn_previous: // 上一首歌曲
				previous_music();
				break;
			case R.id.imageView_playbtn_next: // 下一首歌曲
				next_music();
				break;
			case R.id.imageView_artwork_front:
				HorizontalPlayMusicActivity.this.finish();
				break;
			case R.id.imageView_artwork_behind:
				HorizontalPlayMusicActivity.this.finish();
				break;

			}

		}

	}

	/**
	 * 单曲循环
	 */
	public void repeat_one() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 1);
		sendBroadcast(intent);
	}

	/**
	 * 全部循环
	 */
	public void repeat_all() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 2);
		sendBroadcast(intent);
	}

	/**
	 * 顺序播放
	 */
	public void repeat_none() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 3);
		sendBroadcast(intent);
	}

	/**
	 * 随机播放
	 */
	public void shuffleMusic() {
		Intent intent = new Intent(CTL_ACTION);
		intent.putExtra("control", 4);
		sendBroadcast(intent);
	}

	/**
	 * 暂停状态下换歌后点击播放回调接口
	 * 
	 * @author Administrator
	 * 
	 */
	public interface OnPauseStatClick {
		public void onPauseStatClick();
	}

	/**
	 * 暂停 换歌后的回调类
	 * 
	 * @author Administrator
	 * 
	 */
	public class PauseCaller {
		public OnPauseStatClick op;

		public void setOnPauseStatClick(OnPauseStatClick op) {
			PauseCaller.this.op = op;
		}

		public void pauseCall() {
			PauseCaller.this.op.onPauseStatClick();
		}
	}

	/**
	 * 上一首
	 */

	public void previous_music() {
		if (isShuffle) {
			listPosition = getRandomIndex(mp3Infos.size() - 1);
		} else {
			listPosition = listPosition - 1;
		}

		if (listPosition >= 0) {
			Mp3Info mp3Info = mp3Infos.get(listPosition); // 上一首MP3
			if (isWaitForPlay) {
				showArtwork(listPosition, -1);
			}
			String title = mp3Info.getTitle();
			url = mp3Info.getUrl();
			actionIntent = new Intent();
			actionIntent.setAction("com.stark.media.MUSIC_SERVICE");
			actionIntent.putExtra("url", mp3Info.getUrl());
			actionIntent.putExtra("listPosition", listPosition);
			actionIntent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
			if (isPlaying) {
				actionIntent.putExtra("mp3Infos", (Serializable) mp3Infos);
				startService(actionIntent);
			} else {
				isWaitForPlay = true;
				pauseCaller = new PauseCaller();
				pauseCaller.setOnPauseStatClick(new OnPauseStatClick() {

					@Override
					public void onPauseStatClick() {
						actionIntent.putExtra("mp3Infos",
								(Serializable) mp3Infos);
						actionIntent.putExtra("previous_pause", true);// 暂停情况下换歌，防止重复播放动画
						startService(actionIntent);

					}
				});
			}

		} else {// 过头了
			listPosition = listPosition + 1;
			Mp3Info mp3Info = mp3Infos.get(listPosition); // 上一首MP3
			String title = mp3Info.getTitle();
			url = mp3Info.getUrl();
			actionIntent = new Intent();
			actionIntent.setAction("com.stark.media.MUSIC_SERVICE");
			actionIntent.putExtra("url", mp3Info.getUrl());
			actionIntent.putExtra("listPosition", listPosition);
			actionIntent.putExtra("MSG", AppConstant.PlayerMsg.PRIVIOUS_MSG);
			if (isPlaying) {
				actionIntent.putExtra("mp3Infos", (Serializable) mp3Infos);
				startService(actionIntent);
			} else {
				isWaitForPlay = true;
				pauseCaller = new PauseCaller();
				pauseCaller.setOnPauseStatClick(new OnPauseStatClick() {

					@Override
					public void onPauseStatClick() {
						actionIntent.putExtra("mp3Infos",
								(Serializable) mp3Infos);
						actionIntent.putExtra("previous_pause", true);// 暂停情况下换歌，防止重复播放动画
						startService(actionIntent);

					}
				});
			}
		}

		updateList();// 更新列表

	}

	/**
	 * 获取随机位置
	 * 
	 * @param end
	 * @return
	 */
	protected int getRandomIndex(int end) {
		int index = (int) (Math.random() * end);
		return index;
	}

	/**
	 * 下一首
	 */
	public void next_music() {
		if (isShuffle) {
			listPosition = getRandomIndex(mp3Infos.size() - 1);
		} else {
			listPosition = listPosition + 1;
		}

		if (listPosition <= mp3Infos.size() - 1) {
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			if (isWaitForPlay) {
				showArtwork(listPosition, 1);
			}
			url = mp3Info.getUrl();
			actionIntent = new Intent();
			actionIntent.setAction("com.stark.media.MUSIC_SERVICE");
			actionIntent.putExtra("url", mp3Info.getUrl());
			actionIntent.putExtra("listPosition", listPosition);
			actionIntent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
			if (isPlaying) {
				stopService(actionIntent);
				actionIntent.putExtra("mp3Infos", (Serializable) mp3Infos);
				startService(actionIntent);
			} else {
				isWaitForPlay = true;
				pauseCaller = new PauseCaller();
				pauseCaller.setOnPauseStatClick(new OnPauseStatClick() {

					@Override
					public void onPauseStatClick() {
						actionIntent.putExtra("mp3Infos",
								(Serializable) mp3Infos);
						actionIntent.putExtra("next_pause", true);// 暂停情况下换歌，防止重复播放动画
						startService(actionIntent);

					}
				});
			}
		} else {// 到底了
			listPosition = listPosition - 1;
			Mp3Info mp3Info = mp3Infos.get(listPosition);
			url = mp3Info.getUrl();
			actionIntent = new Intent();
			actionIntent.setAction("com.stark.media.MUSIC_SERVICE");
			actionIntent.putExtra("url", mp3Info.getUrl());
			actionIntent.putExtra("listPosition", listPosition);
			actionIntent.putExtra("MSG", AppConstant.PlayerMsg.NEXT_MSG);
			if (isPlaying) {
				stopService(actionIntent);
				actionIntent.putExtra("mp3Infos", (Serializable) mp3Infos);
				startService(actionIntent);
			} else {
				isWaitForPlay = true;
				pauseCaller = new PauseCaller();
				pauseCaller.setOnPauseStatClick(new OnPauseStatClick() {

					@Override
					public void onPauseStatClick() {
						actionIntent.putExtra("mp3Infos",
								(Serializable) mp3Infos);
						actionIntent.putExtra("next_pause", true);// 暂停情况下换歌，防止重复播放动画
						startService(actionIntent);

					}
				});
			}
		}

		updateList();
	}

	/**
	 * 
	 * @author 电话监听器类
	 */
	private class MobliePhoneStateListener extends PhoneStateListener {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE: // 挂机状态 其实没事情况下就触发 坑死我了Device
													// call state: No activity.

				Intent intent = new Intent(HorizontalPlayMusicActivity.this,
						PlayerService.class);
				intent.setAction("com.stark.media.MUSIC_SERVICE");
				intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG); // 继续播放音乐
				intent.putExtra("mp3Infos", (Serializable) mp3Infos);
				// Log.e("", "挂机状态");
				startService(intent);
				isPlaying = true;
				isPause = false;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: // 通话状态
			case TelephonyManager.CALL_STATE_RINGING: // 响铃状态
				Intent intent2 = new Intent(HorizontalPlayMusicActivity.this,
						PlayerService.class);
				intent2.setAction("com.stark.media.MUSIC_SERVICE");
				intent2.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
				intent2.putExtra("mp3Infos", (Serializable) mp3Infos);
				startService(intent2);
				isPlaying = true;
				isPause = false;
				break;
			default:
				break;
			}
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	/**
	 * 从Bundle中获取来自Fragment中传过来的数据
	 */
	@SuppressWarnings("unchecked")
	private void getDataFromBundle() {
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		title = bundle.getString("title");
		artist = bundle.getString("artist");
		url = bundle.getString("url");
		listPosition = bundle.getInt("listPosition");
		repeatState = bundle.getInt("repeatState");
		isShuffle = bundle.getBoolean("shuffleState");
		flag = bundle.getInt("MSG");
		currentTime = bundle.getInt("currentTime");
		duration = bundle.getInt("duration");
		album = bundle.getString("album");
		mp3Infos = (ArrayList) bundle.getSerializable("mp3Infos");
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver();
		mSensorManager.registerListener(this, mOrientationSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}

	/**
	 * 反注册广播
	 */
	@Override
	protected void onStop() {
		super.onStop();
		getApplicationContext().unregisterReceiver(playerReceiver);
		System.out.println("PlayerActivity has stoped");
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

		// exit();
		//android.os.Process.killProcess(android.os.Process.myPid());

		Log.e("", "ondestory");
		super.onDestroy();
	}

	/**
	 * 播放音乐
	 */
	public void play() {
		// 开始播放的时候为顺序播放
		// repeat_none();
		Intent intent = new Intent();
		intent.setAction("com.stark.media.MUSIC_SERVICE");
		intent.putExtra("url", url);
		intent.putExtra("listPosition", listPosition);
		intent.putExtra("MSG", flag);
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		startService(intent);
	}

	/**
	 * 用来接收从service传回来的广播的内部类
	 */
	public class PlayerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MUSIC_CURRENT)) {
				currentTime = intent.getIntExtra("currentTime", -1);
				if (!isWaitForPlay && !isPause) {
				}
			} else if (action.equals(MUSIC_DURATION)) {
				int duration = intent.getIntExtra("duration", -1);
			} else if (action.equals(UPDATE_ACTION)) {
				// Log.e("", "updateList");
				// 获取Intent中的current消息，current代表当前正在播放的歌曲
				// updateList();
				listPosition = intent.getIntExtra("current", -1);
				int play_action = intent.getIntExtra("play_action", 0);
				url = mp3Infos.get(listPosition).getUrl();
				if (!isWaitForPlay) {
					showArtwork(listPosition, play_action);
				}
				if (listPosition >= 0) {
				}
				if (listPosition == 0) {
					isPause = true;
				}
			}
		}
	}

	public void updateList() {
		album = mp3Infos.get(listPosition).getAlbum();
		queryForList();
		getMp3InfosForList();
		adapter = new HorizontalPlayActivityListViewAdapter(this, cursor);
		elasticListView.removeHeaderView(elasticListView.getChildAt(0));
		elasticListView.addHeaderView(getHeaderView());
		elasticListView.setAdapter(adapter);
	}
	/**
	 * 获取bitmap
	 * 
	 * @param position
	 * @return
	 */
	private Bitmap getBmpInList(int position) {
		Mp3Info mp3Info = mp3Infos.get(position);
		return MediaUtil.getArtwork(this, mp3Info.getId(),
				mp3Info.getAlbumId(), true, false);
	}

	private static Bitmap front;
	private Bitmap behind;;

	public static Bitmap getArtworkOut() {
		return front;
	}

	/**
	 * 显示专辑封面
	 * 
	 * @param position
	 *            mp3info在MP3Infos中的位置
	 * @param action
	 *            -1：前一首 0：没有 1：下一首
	 */
	private void showArtwork(final int position, int action) {
		if (action != 0) {
			if (position - 1 >= 0 && position + 1 <= mp3Infos.size()) {
				showArtworkAnim(position, action);
			} else {
				// 后期退回到MainActivity
			}
			return;
		} else {

			front = getBmpInList(position);
			if (front == null) {// 设置文字封面
				front = getTextArtwork(mp3Infos.get(position).getAlbum(),
						mp3Infos.get(position).getArtist());
			}
			artwork_front.setImageBitmap(front);
		}

	}

	private Animation albumanim1;
	private Animation albumanim2;

	private void showArtworkAnim(int position, int action) {
		switch (action) {
		case -1:
			albumanim1 = AnimationUtils.loadAnimation(
					HorizontalPlayMusicActivity.this,
					R.anim.album_slide_out_to_right);
			// Log.e("start","前一首");
			artwork_front.startAnimation(albumanim1);
			albumanim1.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					artwork_front.setImageBitmap(behind);
				}
			});

			behind = getBmpInList(position);
			if (behind == null) {
				behind = getTextArtwork(mp3Infos.get(position).getAlbum(),
						mp3Infos.get(position).getArtist());
			}
			artwork_behind.setImageBitmap(behind);

			albumanim2 = AnimationUtils.loadAnimation(
					HorizontalPlayMusicActivity.this,
					R.anim.album_slide_in_from_left);
			// Log.e("end","前一首");
			artwork_behind.startAnimation(albumanim2);
			break;
		case 1:
			// Log.e("", "下一首");
			albumanim1 = AnimationUtils.loadAnimation(
					HorizontalPlayMusicActivity.this,
					R.anim.album_slide_out_to_left);
			artwork_front.startAnimation(albumanim1);
			albumanim1.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationStart(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationRepeat(Animation animation) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					// TODO Auto-generated method stub
					artwork_front.setImageBitmap(behind);
				}
			});

			behind = getBmpInList(position);
			if (behind == null) {
				behind = getTextArtwork(mp3Infos.get(position).getAlbum(),
						mp3Infos.get(position).getArtist());
			}
			artwork_behind.setImageBitmap(behind);

			albumanim2 = AnimationUtils.loadAnimation(
					HorizontalPlayMusicActivity.this,
					R.anim.album_slide_in_from_right);
			artwork_behind.startAnimation(albumanim2);
			break;
		default:
			break;
		}

	}

	/**
	 * 创建文字的专辑封面
	 * 
	 * @param albumName
	 * @param artistName
	 * @return
	 */
	public Bitmap getTextArtwork(String albumName, String artistName) {

		TextView albumNameArtwork = (TextView) findViewById(R.id.textView_artwork_album_name);
		TextView artistNameArtwork = (TextView) findViewById(R.id.textView_artwork_artist);
		albumNameArtwork.setVisibility(View.VISIBLE);
		artistNameArtwork.setVisibility(View.VISIBLE);
		albumNameArtwork.setText(albumName);
		artistNameArtwork.setText(artistName);
		Bitmap bitmap = Bitmap.createBitmap(artworkLayoutWidth,
				artworklayoutHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.rotate(-35, artworkLayoutWidth / 2, artworklayoutHeight / 2);
		artworkRL.draw(canvas);
		albumNameArtwork.setVisibility(View.GONE);
		artistNameArtwork.setVisibility(View.GONE);
		return bitmap;
	}

	/**
	 * onCreate()后渲染专辑封面
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus == true) {
			artworkRL = (RelativeLayout) findViewById(R.id.RelativeLayout_text_artwork);
			artworkLayoutWidth = artworkRL.getWidth();
			artworklayoutHeight = artworkRL.getHeight();
			showArtwork(listPosition, 0);
		}
	}

	/**
	 * 回调音量控制函数
	 */
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP: // 按音量减键
			if (action == KeyEvent.ACTION_UP) {
				if (currentVolume < maxVolume) {
					currentVolume = currentVolume + 1;
					am.setStreamVolume(AudioManager.STREAM_MUSIC,
							currentVolume, 0);
				} else {
					am.setStreamVolume(AudioManager.STREAM_MUSIC,
							currentVolume, 0);
				}
			}
			return false;
		case KeyEvent.KEYCODE_VOLUME_DOWN: // 按音量加键
			if (action == KeyEvent.ACTION_UP) {
				if (currentVolume > 0) {
					currentVolume = currentVolume - 1;
					am.setStreamVolume(AudioManager.STREAM_MUSIC,
							currentVolume, 0);
				} else {
					am.setStreamVolume(AudioManager.STREAM_MUSIC,
							currentVolume, 0);
				}
			}
			return false;
		default:
			return super.dispatchKeyEvent(event);
		}
	}

	public static List<Mp3Info> getMp3InfosForOut() {
		return mp3Infos;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float roll = event.values[1];
		float z = event.values[2];
		if (roll < -30 && Math.abs(z) < 30) {
			this.finish();
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	private static int listFlag = 0;
	public class UpdateListReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			listFlag++;
		//	Log.e(""+listFlag, "update list receiver");
			if(listFlag%4==0)
			updateList();

		}

	}
}
