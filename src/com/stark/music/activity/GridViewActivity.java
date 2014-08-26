package com.stark.music.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.stark.adapter.GridViewAdapter;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.service.PlayerService;
import com.stark.util.SortCursor;
import com.stark.view.MyGridView;

public class GridViewActivity extends Activity implements SensorEventListener,
		LoaderCallbacks<Cursor> {

	private SensorManager mSensorManager;
	private Sensor mOrientationSensor;
	private List<Mp3Info> mp3Infos;
	private MyGridView gridView;
	private GridViewAdapter gridViewAdapter;
	private Cursor mCursor;
	private int listPosition;
	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	public static int mSongIdIndex, mSongTitleIndex, mArtistNameIndex,
			mAlbumNameIndex, mAlbumIdIndex, mDurationIndex, mUrlIndex;

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isShuffle = true; // 随机播放
	private boolean isNoneShuffle = true; // 顺序播放

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.gridview_activity);
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mOrientationSensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		getLoaderManager().initLoader(0, null, this);
		initView();

	}

	public void initView() {
		gridView = (MyGridView) findViewById(R.id.data_gridview);
		gridViewAdapter = new GridViewAdapter(getApplicationContext(),
				R.layout.gridview_item, mCursor, new String[] {}, new int[] {},
				0);
		gridView.setAdapter(gridViewAdapter);
	}

	public void setGridView() {
		/** 横屏时宽高对换...... **/
		int a = MainActivity.getScreenHeight();
		int b = MainActivity.getScreenWidth();
		int screenHeight = Math.max(a, b); // 横屏时高度为竖屏时宽度
		int screenWidth = Math.min(b, a);
		gridViewAdapter.setGridView(gridView);
		gridView.setColumnWidth(screenWidth / 3);
		gridView.setOnItemClickListener(new GridViewOnItemClickListener());
		if(MainActivity.zAxis>0){
			gridView.setStackFromBottom(false);
		}else{
			gridView.setStackFromBottom(true);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		/**
		 * 强制设置为竖屏
		 */

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		mSensorManager.registerListener(this, mOrientationSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	private float formerZ ;
	@Override
	public void onSensorChanged(SensorEvent event) {
		float roll = event.values[1];
		float z = event.values[2];
		/*if(Math.abs(formerZ - z )>70){
			this.finish();
		}*/
		formerZ = z;
		if (roll < -30 && Math.abs(z) < 30) {
			this.finish();
			/*Intent intent = new Intent(this,MainActivity.class);
			startActivity(intent);*/
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

		return new CursorLoader(this, uri, null, null, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data == null) {
			return;
		}
		mSongIdIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
		mSongTitleIndex = data
				.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
		mArtistNameIndex = data
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
		mAlbumNameIndex = data
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
		mAlbumIdIndex = data
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
		mDurationIndex = data
				.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
		mUrlIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
		// mAlbumNameIndex = data.getColumnIndexOrThrow(Audio.Media.ALBUM);
		SortCursor sc = new SortCursor(data, MediaStore.Audio.Media.TITLE);
		mCursor = sc;
		setGridView();
		gridViewAdapter.changeCursor(sc);

		getMp3Infos();

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (gridViewAdapter != null)
			gridViewAdapter.changeCursor(null);
	}

	class GridViewOnItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			listPosition = position;
			playMusic(position);
		}

	}

	/***
	 * 构造专辑内歌曲的集合
	 */
	public List<Mp3Info> getMp3Infos() {
		mp3Infos = new ArrayList<Mp3Info>();
		for (int i = 0; i < mCursor.getCount(); i++) {
			mCursor.moveToPosition(i);
			Mp3Info mp3Info = new Mp3Info();
			long id = mCursor.getLong(mSongIdIndex); // 音乐id
			String title = mCursor.getString(mSongTitleIndex); // 音乐标题
			String artist = mCursor.getString(mArtistNameIndex); // 艺术家
			String album = mCursor.getString(mAlbumNameIndex); // 专辑
			long albumId = mCursor.getInt(mAlbumIdIndex);
			long duration = mCursor.getLong(mDurationIndex); // 时长
			String url = mCursor.getString(mUrlIndex); // 文件路径

			mp3Info.setId(id);
			mp3Info.setTitle(title);
			mp3Info.setArtist(artist);
			mp3Info.setAlbum(album);
			mp3Info.setAlbumId(albumId);
			mp3Info.setDuration(duration);
			mp3Info.setUrl(url);
			mp3Infos.add(mp3Info);
		}
		return mp3Infos;
	}

	/**
	 * 
	 * @param listPosition
	 */
	private void playMusic(int listPosition) {

		dbHelperofPlayState = new DBHelperofPlayState(this);
		db = dbHelperofPlayState.getWritableDatabase();
		Cursor c = db.query("play_state", null, null, null, null, null, null);
		c.moveToNext();
		repeatState = c.getInt(c.getColumnIndex("repeat_state"));
		int isShuffleINT = c.getInt(c.getColumnIndex("is_shuffle"));
		if (isShuffleINT == 0) {
			isShuffle = false;
		} else {
			isShuffle = true;
		}
		Mp3Info mp3Info = mp3Infos.get(listPosition );
		// 添加一系列要传递的数据
		Intent intent = new Intent(this, HorizontalPlayMusicActivity.class);
		intent.putExtra("title", mp3Info.getTitle());
		intent.putExtra("url", mp3Info.getUrl());
		intent.putExtra("artist", mp3Info.getArtist());
		intent.putExtra("listPosition", listPosition );
		intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
		intent.putExtra("repeatState", repeatState);//
		intent.putExtra("shuffleState", isShuffle);//
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		intent.putExtra("album", mp3Info.getAlbum());
		startActivity(intent);
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
}
