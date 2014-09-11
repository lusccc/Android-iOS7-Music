package com.stark.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.stark.domain.AppConstant;
import com.stark.domain.LrcContent;
import com.stark.domain.LrcProcess;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.MainActivity;
import com.stark.util.MediaUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * * 2014/2/10
 *
 * @author d 音乐播放服务
 */
public class PlayerService extends Service {
    private MediaPlayer mediaPlayer; // 媒体播放器对象;
    private String path; // 音乐文件路径
    private int msg; // 播放信息

    private static boolean isPause = false; // 暂停状态
    public static int current = 0; // 记录当前正在播放的音乐
    public static List<Mp3Info> mp3Infos; // 存放Mp3Info对象的集合

    private int status = 3; // 播放状态，默认为顺序播放
    private MyReceiver myReceiver; // 自定义广播接收器
    private HeadsetPlugReceiver headsetPlugReceiver;
    private NotificationReceiver notificationReceiver;

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
    public static final String NOTIFICATION_PRE = "com.stark.action.NOTIFICATION_PRE";
    public static final String NOTIFICATION_PAUSE = "com.stark.action.NOTIFICATION_PAUSE";
    public static final String NOTIFICATION_NEXT = "com.stark.action.NOTIFICATION_NEXt";
    public static final String NOTIFICATION_ACTION = "com.stark.action.NOTIFICATION_ACTION";
    public static final String SERVICE_PLAY_CONTROL = "com.stark.action.SERVICE_PLAY_CONTROL";
    public static boolean isPlaying = false;

    public static MusicCompletionListener musicCompletionListener;

    public static int currentTime = -1; // 当前播放进度
    public static int duration; // 播放长度

    private static Intent lastIntent;//上次传入的Intent 携带音乐信息

    private NotificationManager mNotificationManager;
    private Notification notification;
    private RemoteViews mRemoteViews;
    private AppWidgetManager appWidgetManager;

    private int headsetFirstStat = isHeadsetExists();

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
        }

        ;
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

        notificationReceiver = new NotificationReceiver();
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(NOTIFICATION_ACTION);
        getApplicationContext().registerReceiver(notificationReceiver, filter2);

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

    private void setupNotification() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mRemoteViews = new RemoteViews(getPackageName(), R.layout.notification_layout);
        appWidgetManager = AppWidgetManager.getInstance(this);
        Intent intent1 = new Intent();
        intent1.setAction(NOTIFICATION_ACTION);
        intent1.putExtra("PLAY_ACTION", NOTIFICATION_PRE);
        /* 上一首按钮 */
        //这里加了广播，所及INTENT的必须用getBroadcast方法
        PendingIntent intent_prev = PendingIntent.getBroadcast(this, 1, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.iv_notification_pre, intent_prev);
        appWidgetManager.updateAppWidget(R.id.iv_notification_pre, mRemoteViews);
         /* 播放/暂停  按钮 */
        Intent intent2 = new Intent();
        intent2.setAction(NOTIFICATION_ACTION);
        intent2.putExtra("PLAY_ACTION", NOTIFICATION_PAUSE);
        PendingIntent intent_play = PendingIntent.getBroadcast(this, 2, intent2, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.iv_notification_pause, intent_play);
        appWidgetManager.updateAppWidget(R.id.iv_notification_pause, mRemoteViews);
        /* 下一首 按钮  */
        Intent intent3 = new Intent();
        intent3.setAction(NOTIFICATION_ACTION);
        intent3.putExtra("PLAY_ACTION", NOTIFICATION_NEXT);
        PendingIntent intent_next = PendingIntent.getBroadcast(this, 3, intent3, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteViews.setOnClickPendingIntent(R.id.iv_notification_next, intent_next);
        appWidgetManager.updateAppWidget(R.id.iv_notification_next, mRemoteViews);
        mRemoteViews.setTextViewText(R.id.tv_notification_songinfo, mp3Infos.get(current).getArtist() + "-" + mp3Infos.get(current).getTitle());
        mRemoteViews.setImageViewBitmap(R.id.iv_notification_album, MediaUtil.getArtwork(this, mp3Infos.get(current).getId(),
                mp3Infos.get(current).getAlbumId(), true, false));
        if(isPause()){
            mRemoteViews.setImageViewResource(R.id.iv_notification_pause, R.drawable.play_play_white);
        }else{
            mRemoteViews.setImageViewResource(R.id.iv_notification_pause,R.drawable.play_pause_white_selector);

        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, new Intent(), Notification.FLAG_ONGOING_EVENT);
        Intent jumpIntent = new Intent();
        jumpIntent.setClass(this, MainActivity.class);
        mBuilder
                .setContent(mRemoteViews)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setTicker("正在播放 "+mp3Infos.get(current).getTitle())
                .setPriority(Notification.PRIORITY_DEFAULT)// 设置该通知优先级
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(this,0,jumpIntent,0))
                .setSmallIcon(R.drawable.ic_launcher)

                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        notification = mBuilder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("", "service onStart" + intent);

        if (mp3Infos == null && intent != null) {
            mp3Infos = (ArrayList<Mp3Info>) intent.getSerializableExtra("mp3Infos");
        }

        path = intent.getStringExtra("url"); // 歌曲路径
        msg = intent.getIntExtra("MSG", 0); // 播放信息
        if (msg != AppConstant.PlayerMsg.CONTINUE_MSG) {
            current = intent.getIntExtra("listPosition", -1); // 当前播放歌曲的在mp3Infos的位置
        }
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
        setupNotification();
        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }


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

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
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

    //检测是否插入耳机
    private int isHeadsetExists() {
        String HEADSET_STATE_PATH = "/sys/class/switch/h2w/state";
        char[] buffer = new char[1024];

        int newState = 0;

        try {
            FileReader file = new FileReader(HEADSET_STATE_PATH);
            int len = file.read(buffer, 0, 1024);
            newState = Integer.valueOf((new String(buffer, 0, len)).trim());
        } catch (FileNotFoundException e) {
            Log.e("FMTest", "This kernel does not have wired headset support");
        } catch (Exception e) {
            Log.e("FMTest", "", e);
        }

        if (newState != 0) {
            return 1;
        } else {
            return 0;
        }
    }

    public class HeadsetPlugReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("state")) {
                int stat = intent.getIntExtra("state", 0);
                //0 not connected,1 connected
                Log.e("headsetStat:", "" + intent.getIntExtra("state", 0));
                if (headsetFirstStat == stat) {//耳机状态没有改变
                    Log.e("", "耳机状态没有改变");
                    return;
                } else {//耳机状态改变
                    headsetFirstStat = isHeadsetExists();//更新耳机遇上一次对比的状态
                    if (!isPause && stat == 1) {//正在播放耳机插入不作为

                    } else if (!isPause && stat == 0) {//正在播放耳机拔出暂停播放
                        Log.e("", "pause()");
                        pause();
                        Intent headsetIntent = new Intent();
                        headsetIntent.setAction(SERVICE_PLAY_CONTROL);
                        headsetIntent.putExtra("PLAY_ACTION", "pause"); // 通过Intent来传递歌曲的总长度
                        sendBroadcast(headsetIntent);
                    } else if (isPause && stat == 1) {//暂停耳机插入继续播放
                        Log.e("", "resume()");
                        resume();
                        Intent headsetIntent = new Intent();
                        headsetIntent.setAction(SERVICE_PLAY_CONTROL);
                        headsetIntent.putExtra("PLAY_ACTION", "resume"); // 通过Intent来传递歌曲的总长度
                        sendBroadcast(headsetIntent);
                    } else if (isPause && stat == 0) {//暂停耳机拔出不作为

                    }
                }

            }

        }

    }

    public class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String playAction = intent.getStringExtra("PLAY_ACTION");
            if (playAction.equals(NOTIFICATION_PAUSE)) {
                Log.e("", "pause()");
                if (!isPause()) {//正在播放
                    pause();
                    Intent notificationIntent = new Intent();
                    notificationIntent.setAction(SERVICE_PLAY_CONTROL);
                    notificationIntent.putExtra("PLAY_ACTION", "pause"); // 通过Intent来传递歌曲的总长度
                    sendBroadcast(notificationIntent);
                } else {
                    resume();
                    Intent notificationIntent = new Intent();
                    notificationIntent.setAction(SERVICE_PLAY_CONTROL);
                    notificationIntent.putExtra("PLAY_ACTION", "resume"); // 通过Intent来传递歌曲的总长度
                    sendBroadcast(notificationIntent);

                }

            } else if (playAction.equals(NOTIFICATION_PRE)) {
                if (status == 1) { // 单曲循环
                    mediaPlayer.start();
                } else if (status == 2) { // 全部循环
                    current--;
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
                    current--; // 下一首位置
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


            } else if (playAction.equals(NOTIFICATION_NEXT)) {
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

            setupNotification();
        }

    }


}
