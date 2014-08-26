package com.stark.service;

import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.animation.AnimationUtils;

import com.stark.domain.AppConstant;
import com.stark.domain.LrcContent;
import com.stark.domain.LrcProcess;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.MainActivity;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.util.MediaUtil;

/**
 * * 2014/2/10
 * 
 * @author d 音乐播放服务
 */
@SuppressLint("NewApi")
public class PlayerService extends Service {
	private MediaPlayer mediaPlayer; // 媒体播放器对象;
	private String path; // 音乐文件路径
	private int msg; // 播放信息

	private static boolean isPause; // 暂停状态
	public static int current = 0; // 记录当前正在播放的音乐
	public static List<Mp3Info> mp3Infos; // 存放Mp3Info对象的集合

	private int status = 3; // 播放状态，默认为顺序播放
	private MyReceiver myReceiver; // 自定义广播接收器
	private HeadsetPlugReceiver headsetPlugReceiver;

	private LrcProcess mLrcProcess; // 歌词处理
	private List<LrcContent> lrcList = new ArrayList<LrcContent>(); // 存放歌词列表对象
	private int index = 0; // 歌词检索值
	private int playAcion = 0;// 播放动作前进还是后退
	private boolean havePaused = false;
	// 服务要发送的一些Action
	public static final String UPDATE_ACTION = "com.stark.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.stark.action.CTL_ACTION"; // 控制动作
	public static final String MUSIC_CURRENT = "com.stark.action.MUSIC_CURRENT"; // 当前音乐播放时间更新动作
	public static final String MUSIC_DURATION = "com.stark.action.MUSIC_DURATION";// 新音乐长度更新动作
	public static final String SHOW_LRC = "com.stark.action.SHOW_LRC"; // 通知显示歌词
	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";
	public static boolean isPlaying = false;

	public static MusicCompletionListener musicCompletionListener;

	public static int currentTime = -1; // 当前播放进度
	public static int duration; // 播放长度

	public static boolean isPause() {
		return isPause;
	}

	/**
	 * handler用来接收消息，来发送广播更新播放时间
	 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				if (mediaPlayer != null) {
					currentTime = mediaPlayer.getCurrentPosition(); // 获取当前音乐播放的位置
					Intent intent = new Intent();
					intent.setAction(MUSIC_CURRENT);
					intent.putExtra("currentTime", currentTime);
					sendBroadcast(intent); // 给PlayerActivity发送广播
					handler.sendEmptyMessageDelayed(1, 1000);// = =
				}
			}
		};
	};

	/**
	 * 发送是否正在播放的广播，包括暂停，来控制"播放中>"是否显示
	 */
	public void sendPlayingStat() {
		Intent intent = new Intent();
		intent.setAction(PLAYING_STAT);
		sendBroadcast(intent);
		isPlaying = true;
	}

	/**
	 * 发送是否正在播放的广播，包括暂停，来控制"播放中>"是否显示
	 */
	public void sendNotPlayingStat() {
		Intent intent = new Intent();
		intent.setAction(NOT_PLAYING_STAT);
		sendBroadcast(intent);
		isPlaying = false;
	}

	@Override
	public void onCreate() {
		Log.e("", "service oncreat");
		super.onCreate();
		mediaPlayer = new MediaPlayer();
		musicCompletionListener = new MusicCompletionListener();
		/**
		 * 设置音乐播放完成时的监听器
		 */
		mediaPlayer.setOnCompletionListener(musicCompletionListener);

		registerReceiver();
	}

	private void registerReceiver() {
		myReceiver = new MyReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(CTL_ACTION);
		filter.addAction(SHOW_LRC);
		getApplicationContext().registerReceiver(myReceiver, filter);

		headsetPlugReceiver = new HeadsetPlugReceiver();
		IntentFilter filter1 = new IntentFilter();
		filter1.addAction("android.intent.action.HEADSET_PLUG");
		getApplicationContext().registerReceiver(headsetPlugReceiver, filter1);
	}

	private void finishFormerPlayActivity() {
		Intent intent = new Intent();
		intent.setAction("FINISH");
		sendBroadcast(intent);
	}

	public class MusicCompletionListener implements OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			if (status == 1) { // 单曲循环
				mediaPlayer.start();
			} else if (status == 2) { // 全部循环
				current++;
				if (current > mp3Infos.size() - 1) { // 变为第一首的位置继续播放
					finishFormerPlayActivity();
				} else {
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("play_action", 1);// 为了显示专辑动画
					sendIntent.putExtra("current", current);

					// 发送广播，将被Activity组件中的BroadcastReceiver接收到
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				}

			} else if (status == 3) { // 顺序播放
				current++; // 下一首位置
				if (current <= mp3Infos.size() - 1) {
					Intent sendIntent = new Intent(UPDATE_ACTION);
					sendIntent.putExtra("current", current);
					sendIntent.putExtra("play_action", 1);// 为了显示专辑动画
					// 发送广播，将被Activity组件中的BroadcastReceiver接收到
					sendBroadcast(sendIntent);
					path = mp3Infos.get(current).getUrl();
					play(0);
				} else {
					/*
					 * mediaPlayer.seekTo(0); current = 0; Intent sendIntent =
					 * new Intent(UPDATE_ACTION); sendIntent.putExtra("current",
					 * current); sendIntent.putExtra("play_action", 1);//
					 * 为了显示专辑动画 // 发送广播，将被Activity组件中的BroadcastReceiver接收到
					 * sendBroadcast(sendIntent);
					 */
					finishFormerPlayActivity();
				}
			} else if (status == 4) { // 随机播放
				current = getRandomIndex(mp3Infos.size() - 1);
				Intent sendIntent = new Intent(UPDATE_ACTION);
				sendIntent.putExtra("current", current);
				// 发送广播，将被Activity组件中的BroadcastReceiver接收到
				sendIntent.putExtra("play_action", 1);// 为了显示专辑动画
				sendBroadcast(sendIntent);
				path = mp3Infos.get(current).getUrl();
				play(0);
			}
			Log.e("", "send update list broadcast");
			Intent updatelistIntent = new Intent();
			updatelistIntent.setAction("update_list");
			sendBroadcast(updatelistIntent);
		}

	}

	/**
	 * 获取随机位置
	 * 
	 * @param end
	 * @return
	 */
	protected int getRandomIndex(int end) {
		int index;
		do {
			index = (int) (Math.random() * end);
		} while (index == end);
		return index;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.e("", "service onStart");
		if(mp3Infos==null){
			mp3Infos = (ArrayList<Mp3Info>) intent.getSerializableExtra("mp3Infos");
		}
		
		path = intent.getStringExtra("url"); // 歌曲路径
		msg = intent.getIntExtra("MSG", 0); // 播放信息
		if (msg != AppConstant.PlayerMsg.CONTINUE_MSG) {
			current = intent.getIntExtra("listPosition", -1); // 当前播放歌曲的在mp3Infos的位置
		}
		Log.e("", "" + msg);
		if (msg == AppConstant.PlayerMsg.PLAY_MSG) { // 直接播放音乐
			play(0);
		} else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) { // 暂停
			havePaused = true;
			pause();
		} else if (msg == AppConstant.PlayerMsg.STOP_MSG) { // 停止
			this.stopSelf();
			stop();
		} else if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { // 继续播放
			resume();
		} else if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { // 上一首
			previous();
		} else if (msg == AppConstant.PlayerMsg.NEXT_MSG) { // 下一首

			next();
		} else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) { // 进度更新
			currentTime = intent.getIntExtra("progress", -1);
			play(currentTime);
		} else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
			handler.sendEmptyMessage(1);
		}

		return super.onStartCommand(intent, flags, startId);
	}

	/*
	 * @Override public void onStart(Intent intent, int startId) { Log.e("",
	 * "service onStart"); mp3Infos = (ArrayList<Mp3Info>)
	 * intent.getSerializableExtra("mp3Infos"); path =
	 * intent.getStringExtra("url"); // 歌曲路径 msg = intent.getIntExtra("MSG", 0);
	 * // 播放信息 if (msg != AppConstant.PlayerMsg.CONTINUE_MSG) { current =
	 * intent.getIntExtra("listPosition", -1); // 当前播放歌曲的在mp3Infos的位置 }
	 * Log.e("", ""+msg); if (msg == AppConstant.PlayerMsg.PLAY_MSG) { // 直接播放音乐
	 * play(0); } else if (msg == AppConstant.PlayerMsg.PAUSE_MSG) { // 暂停
	 * havePaused = true; pause(); } else if (msg ==
	 * AppConstant.PlayerMsg.STOP_MSG) { // 停止 this.stopSelf(); stop(); } else
	 * if (msg == AppConstant.PlayerMsg.CONTINUE_MSG) { // 继续播放 resume(); } else
	 * if (msg == AppConstant.PlayerMsg.PRIVIOUS_MSG) { // 上一首 previous(); }
	 * else if (msg == AppConstant.PlayerMsg.NEXT_MSG) { // 下一首
	 * 
	 * next(); } else if (msg == AppConstant.PlayerMsg.PROGRESS_CHANGE) { //
	 * 进度更新 currentTime = intent.getIntExtra("progress", -1); play(currentTime);
	 * } else if (msg == AppConstant.PlayerMsg.PLAYING_MSG) {
	 * handler.sendEmptyMessage(1); } super.onStart(intent, startId); }
	 */

	/**
	 * 初始化歌词配置
	 */
	/*
	 * public void initLrc(){ mLrcProcess = new LrcProcess(); //读取歌词文件
	 * mLrcProcess.readLRC(mp3Infos.get(current).getUrl()); //传回处理后的歌词文件 lrcList
	 * = mLrcProcess.getLrcList(); PlayerActivity.lrcView.setmLrcList(lrcList);
	 * //切换带动画显示歌词
	 * PlayerActivity.lrcView.setAnimation(AnimationUtils.loadAnimation
	 * (PlayerService.this,R.anim.alpha_z)); handler.post(mRunnable); } Runnable
	 * mRunnable = new Runnable() {
	 * 
	 * @Override public void run() {
	 * PlayerActivity.lrcView.setIndex(lrcIndex());
	 * PlayerActivity.lrcView.invalidate(); handler.postDelayed(mRunnable, 100);
	 * } };
	 */

	/**
	 * 根据时间获取歌词显示的索引值
	 * 
	 * @return
	 */
	public int lrcIndex() {
		if (mediaPlayer.isPlaying()) {
			currentTime = mediaPlayer.getCurrentPosition();
			duration = mediaPlayer.getDuration();
		}
		if (currentTime < duration) {
			for (int i = 0; i < lrcList.size(); i++) {
				if (i < lrcList.size() - 1) {
					if (currentTime < lrcList.get(i).getLrcTime() && i == 0) {
						index = i;
					}
					if (currentTime > lrcList.get(i).getLrcTime()
							&& currentTime < lrcList.get(i + 1).getLrcTime()) {
						index = i;
					}
				}
				if (i == lrcList.size() - 1
						&& currentTime > lrcList.get(i).getLrcTime()) {
					index = i;
				}
			}
		}
		return index;
	}

	/**
	 * 播放音乐
	 * 
	 * @param position
	 */
	private void play(int currentTime) {
		sendPlayingStat();
		try {
			// initLrc();
			mediaPlayer.reset();// 把各项参数恢复到初始状态
			mediaPlayer.setDataSource(path);
			mediaPlayer.prepare(); // 进行缓冲
			mediaPlayer
					.setOnPreparedListener(new PreparedListener(currentTime));// 注册一个监听器
			handler.sendEmptyMessage(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 暂停音乐
	 */
	private void pause() {
		sendPlayingStat();
		if (mediaPlayer != null && mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
			isPause = true;
		}
	}

	private void resume() {
		sendPlayingStat();
		if (isPause) {
			mediaPlayer.start();
			isPause = false;
		}
	}

	/**
	 * 上一首
	 */
	private void previous() {
		sendPlayingStat();
		Intent sendIntent = new Intent(UPDATE_ACTION);
		if (!havePaused) {
			sendIntent.putExtra("play_action", -1);// 为了显示专辑动画
		}
		havePaused = false;
		sendIntent.putExtra("current", current);
		// 发送广播，将被Activity组件中的BroadcastReceiver接收到
		sendBroadcast(sendIntent);
		play(0);
	}

	/**
	 * 下一首
	 */
	private void next() {
		sendPlayingStat();
		Intent sendIntent = new Intent(UPDATE_ACTION);
		if (!havePaused) {
			sendIntent.putExtra("play_action", 1);// 为了显示专辑动画
		}
		havePaused = false;
		sendIntent.putExtra("current", current);
		// 发送广播，将被Activity组件中的BroadcastReceiver接收到
		sendBroadcast(sendIntent);

		play(0);
	}

	/**
	 * 停止音乐
	 */
	private void stop() {
		sendNotPlayingStat();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			try {
				mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onDestroy() {
		Log.e("", "service ondestory");
		sendNotPlayingStat();
		if (mediaPlayer != null) {
			mediaPlayer.stop();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		// handler.removeCallbacks(mRunnable);
		/*
		 * this.unregisterReceiver(headsetPlugReceiver);
		 * unregisterReceiver(myReceiver);
		 */
	}

	/**
	 * 
	 * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
	 * 
	 */
	private final class PreparedListener implements OnPreparedListener {
		private int currentTime;

		public PreparedListener(int currentTime) {
			this.currentTime = currentTime;
		}

		@Override
		public void onPrepared(MediaPlayer mp) {
			mediaPlayer.start(); // 开始播放
			if (currentTime > 0) { // 如果音乐不是从头播放
				mediaPlayer.seekTo(currentTime);
			}
			Intent intent = new Intent();
			intent.setAction(MUSIC_DURATION);
			duration = mediaPlayer.getDuration();
			intent.putExtra("duration", duration); // 通过Intent来传递歌曲的总长度
			sendBroadcast(intent);
		}
	}

	public class MyReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			int control = intent.getIntExtra("control", -1);
			Log.e("", "" + control);
			switch (control) {
			case 1:
				status = 1; // 将播放状态置为1表示：单曲循环
				break;
			case 2:
				status = 2; // 将播放状态置为2表示：全部循环
				break;
			case 3:
				status = 3; // 将播放状态置为3表示：顺序播放
				break;
			case 4:
				status = 4; // 将播放状态置为4表示：随机播放
				break;
			}

			String action = intent.getAction();
			if (action.equals(SHOW_LRC)) {
				current = intent.getIntExtra("listPosition", -1);
				// initLrc();
			}
		}
	}

	public class HeadsetPlugReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra("state")) {
				if (intent.getIntExtra("state", 0) == 0) {
					Log.e("", "headset");

					Intent headsetIntent = new Intent();
					headsetIntent.setAction("HEADSET");
					sendBroadcast(headsetIntent);
				}
			}

		}

	}

}
