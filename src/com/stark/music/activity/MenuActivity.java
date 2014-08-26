package com.stark.music.activity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.LauncherActivity.ListItem;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.stark.adapter.MenuListViewAdapter;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.service.PlayerService;
import com.stark.view.ElasticListView;

public class MenuActivity extends Activity {
	private List<Mp3Info> mp3Infos;
	private ElasticListView elasticListView;
	private MenuListViewAdapter menuListViewAdapter;
	private String song_name;
	private String album;
	private Cursor cursor;
	private Cursor listCursor;
	private String artist;
	private long song_id, album_id;
	private TextView doneTV;

	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	private Cursor c;
	private Context context;
	private int repeatState; // 循环标识
	private boolean isShuffle = true; // 随机播放

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu_activity_rl);
		getDataFrombundle();
		query();
		listCursor = getListCursor();
		initView();
		setupView();
		mp3Infos = PlayMusicActivity.getMp3InfosForOut(); // 获取playActivity中MP3info集合
	}

	public void initView() {
		elasticListView = (ElasticListView) findViewById(R.id.elasticListView_menu);
		doneTV = (TextView) findViewById(R.id.textView_title_done);
	}

	public void setupView() {
		menuListViewAdapter = new MenuListViewAdapter(this, listCursor);
		elasticListView.addHeaderView(getHeaderView());
		elasticListView.setAdapter(menuListViewAdapter);
		elasticListView.setOnItemClickListener(new ListItemListener());
		doneTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				doneTV.setPressed(true);
				MenuActivity.this.finish();
				MenuActivity.this.onDestroy();
				finishFormerPlayActivity();
				enterPlayActivity();
			}
		});

	}

	private void finishFormerPlayActivity() {
		Intent intent = new Intent();
		intent.setAction("FINISH");
		sendBroadcast(intent);
	}

	@Override
	protected void onDestroy() {
	//	enterPlayActivity();
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	public void enterPlayActivity() {
		dbHelperofPlayState = new DBHelperofPlayState(this);
		db = dbHelperofPlayState.getWritableDatabase();
		c = db.query("play_state", null, null, null, null, null, null);
		c.moveToNext();
		repeatState = c.getInt(c.getColumnIndex("repeat_state"));
		int isShuffleINT = c.getInt(c.getColumnIndex("is_shuffle"));
		if (isShuffleINT == 0) {
			isShuffle = false;
		} else {
			isShuffle = true;
		}

		List<Mp3Info> mp3Infos = PlayerService.mp3Infos;
		Mp3Info mp3Info = mp3Infos.get(PlayerService.current);
		// 添加一系列要传递的数据
		Intent intent = new Intent(this, PlayMusicActivity.class);
		intent.putExtra("title", mp3Info.getTitle());
		intent.putExtra("url", mp3Info.getUrl());
		intent.putExtra("artist", mp3Info.getArtist());
		intent.putExtra("listPosition", PlayerService.current);
		if (PlayerService.isPause()) {
			intent.putExtra("MSG", AppConstant.PlayerMsg.PAUSE_MSG);
		} else {
			intent.putExtra("MSG", AppConstant.PlayerMsg.CONTINUE_MSG);
		}
		intent.putExtra("repeatState", repeatState);//
		intent.putExtra("shuffleState", isShuffle);//
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		startActivity(intent);
	}

	public Cursor getListCursor() {
		String[] colums = { MediaStore.Audio.Media.DATA,// 歌曲文件的路径
				MediaStore.Audio.Media._ID,// 歌曲ID
				MediaStore.Audio.Media.TITLE,// 歌曲标题
				MediaStore.Audio.Media.ARTIST,// 歌曲的歌手名
				MediaStore.Audio.Media.ALBUM,// 歌曲的唱片集
				MediaStore.Audio.Media.DURATION, // 歌曲的总播放时长
				MediaStore.Audio.Media.ALBUM_ID };
		String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";
		String whereVal[] = { album };
		return getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colums, where,
				whereVal, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
	}

	/** 设置出去动画 ***/
	@Override
	protected void onPause() {
		overridePendingTransition(R.anim.empty, R.anim.popup_exit);
		super.onPause();
	}

	/** 设置进入动画 ***/
	@Override
	protected void onResume() {
		overridePendingTransition(R.anim.popup_enter, R.anim.empty);
		super.onResume();
	}

	public void getDataFrombundle() {
		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();
		song_name = bundle.getString("song_name");
		album = bundle.getString("album");
		artist = bundle.getString("artist");
	}

	public void query() {
		// 获取歌曲详细信息
		String[] colums = { BaseColumns._ID, AlbumColumns.ALBUM,
				AlbumColumns.ARTIST, AlbumColumns.NUMBER_OF_SONGS };
		String where = AlbumColumns.ALBUM + "=?";
		String whereVal[] = { album };
		cursor = getContentResolver().query(Audio.Albums.EXTERNAL_CONTENT_URI,
				colums, where, whereVal, Audio.Albums.ALBUM);
	}

	public class ListItemListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0) {
				return;
			}
			playMusic(getPositionInList(position - 1));

		}

	}

	/**
	 * 
	 * @param ListItemPosition
	 *            list中的位置然后转化到activity中MP3info集合中的位置
	 * @return -1没找到
	 */
	public int getPositionInList(int ListItemPosition) {
		listCursor.moveToPosition(ListItemPosition);
		String toPlayTitle = listCursor.getString(listCursor
				.getColumnIndex(MediaStore.Audio.Media.TITLE));
		for (int i = 0; i < mp3Infos.size(); i++) {
			if (toPlayTitle.equals(mp3Infos.get(i).getTitle())) {

				return i;
			}
		}

		return -1;
	}

	public void playMusic(int positionInMp3Infos) {
		Intent intent = new Intent();
		intent.setAction("com.stark.media.MUSIC_SERVICE");
		intent.putExtra("url", mp3Infos.get(positionInMp3Infos).getUrl());
		intent.putExtra("listPosition", positionInMp3Infos);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		startService(intent);
	}

	public View getHeaderView() {
		View v = View.inflate(this, R.layout.menu_list_firstrow, null);
		TextView artistTV = (TextView) v
				.findViewById(R.id.textView_artist_in_menulist);
		TextView albumTV = (TextView) v
				.findViewById(R.id.textView_album_menulist);
		TextView numofSongTV = (TextView) v
				.findViewById(R.id.textView_numofsong_menulist);
		ImageView artworkIV = (ImageView) v
				.findViewById(R.id.albumImage_in_menulist);
		artistTV.setText(artist);
		albumTV.setText(album);
		String string = this.getString(R.string.num_of_song);
		cursor.moveToNext();
		numofSongTV.setText(cursor.getLong(cursor
				.getColumnIndex(AlbumColumns.NUMBER_OF_SONGS)) + string);
		artworkIV.setImageBitmap(PlayMusicActivity.getArtworkOut());
		artworkIV.setBackgroundColor(Color.parseColor("#F2F2F2"));
		return v;
	}
}
