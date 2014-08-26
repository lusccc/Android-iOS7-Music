package com.stark.music.fragment.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.AlbumUiListViewAdapter;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.service.PlayerService;
import com.stark.util.MediaUtil;
import com.stark.view.ElasticListView;

public class AlbumUiListFragment extends Fragment {

	private static Bundle bundle;

	private TextView artistTitleTV;
	private View view;
	private String albumName;
	private String songNum;
	private ElasticListView elasticListView;
	private Cursor cursor;
	private AlbumUiListViewAdapter auvAdapter;
	private long albumId;
	private long songId;
	private List<Mp3Info> mp3Infos;
	private Cursor dbCursor;
	private RelativeLayout isPlayingRL;
	private RelativeLayout backRL;

	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isNoneShuffle = true; // 顺序播放

	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	private Cursor c;

	private int repeatState; // 循环标识
	private boolean isShuffle = false; // 随机播放

	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";

	static AlbumUiListFragment newInstance(Bundle b) {
		AlbumUiListFragment abmUiListFragment = new AlbumUiListFragment();
		bundle = b;
		return abmUiListFragment;

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_inner_ui_album, null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getDataFromBundle();
		query();
		initView();
		setupView();
		getMp3Infos();
		setReceiver();
		setViewClickListener();
	}

	public void initView() {
		artistTitleTV = (TextView) view
				.findViewById(R.id.textView_fragment_title_uilist);
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_albumui_list);
		auvAdapter = new AlbumUiListViewAdapter(getActivity(), cursor);
		isPlayingRL = (RelativeLayout) view
				.findViewById(R.id.RelativeLayout_isPlaying_title_albumui);
		backRL = (RelativeLayout)view.findViewById(R.id.RelativeLayout_back_album);

	}

	public void setViewClickListener() {
		ViewOnClickListener listener = new ViewOnClickListener();
		isPlayingRL.setOnClickListener(listener);
		backRL.setOnClickListener(listener);
	}

	private class ViewOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.RelativeLayout_isPlaying_title_albumui:
				enterPlayActivity();
				break;
			case R.id.RelativeLayout_back_album:
				backRL.setPressed(true);
				getActivity().getFragmentManager().popBackStack();
			default:
				break;
			}
		}

	}

	public void enterPlayActivity() {
		dbHelperofPlayState = new DBHelperofPlayState(getActivity());
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
		Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
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

	public class PlayingReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(NOT_PLAYING_STAT)) {
				isPlayingRL.setVisibility(View.GONE);
			} else {
				isPlayingRL.setVisibility(View.VISIBLE);
			}
		}

	}

	public void setReceiver() {
		PlayingReceiver receiver = new PlayingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOT_PLAYING_STAT);
		filter.addAction(PLAYING_STAT);
		getActivity().getApplicationContext()
				.registerReceiver(receiver, filter);
	}

	public void getDataFromBundle() {
		albumName = bundle.getString("album_name");
		songNum = bundle.getString("song_num");
		albumId = bundle.getLong("album_id");
		songId = bundle.getLong("song_id");
	}

	public void query() {
		// 获取歌曲详细信息
		String[] colums = { MediaStore.Audio.Media.DATA,// 歌曲文件的路径
				MediaStore.Audio.Media._ID,// 歌曲ID
				MediaStore.Audio.Media.TITLE,// 歌曲标题
				MediaStore.Audio.Media.ARTIST,// 歌曲的歌手名
				MediaStore.Audio.Media.ALBUM,// 歌曲的唱片集
				MediaStore.Audio.Media.DURATION, // 歌曲的总播放时长
				MediaStore.Audio.Media.ALBUM_ID };
		String where = android.provider.MediaStore.Audio.Media.ALBUM + "=?";
		String whereVal[] = { albumName };
		cursor = getActivity().getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, colums, where,
				whereVal, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

	}

	public void setupView() {

		elasticListView.addHeaderView(getHeaderView());
		artistTitleTV.setText(albumName);
		elasticListView.setAdapter(auvAdapter);
		elasticListView.setOnItemClickListener(new AlbumUilListClickListener());
		if (!PlayerService.isPlaying) {
			isPlayingRL.setVisibility(View.GONE);
		} else {
			isPlayingRL.setVisibility(View.VISIBLE);
		}
	}

	public View getHeaderView() {
		View headerView = LayoutInflater.from(getActivity()).inflate(
				R.layout.ui_list_firstrow, null);
		ImageView albumArt = (ImageView) headerView
				.findViewById(R.id.albumImage_in_uilist);
		TextView albumTitle = (TextView) headerView
				.findViewById(R.id.textView_title_in_uilist);
		TextView albumDetail = (TextView) headerView
				.findViewById(R.id.textView_detail_uilist);
		Bitmap bitmap = MediaUtil.getArtwork(getActivity(), songId, albumId,
				true, false);
		albumTitle.setText(albumName);
		albumArt.setImageBitmap(bitmap);
		albumDetail.setText(songNum + " 首歌");
		return headerView;

	}

	/***
	 * 构造专辑内歌曲的集合
	 */
	public void getMp3Infos() {
		mp3Infos = new ArrayList<Mp3Info>();
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
			mp3Infos.add(mp3Info);
		}
	}

	private class AlbumUilListClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == 0)
				return;

			dbHelperofPlayState = new DBHelperofPlayState(getActivity());
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
			Mp3Info mp3Info = mp3Infos.get(position - 1);
			// 添加一系列要传递的数据
			Intent intent = new Intent(getActivity(), PlayMusicActivity.class);

			intent.putExtra("title", mp3Info.getTitle());
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("artist", mp3Info.getArtist());
			intent.putExtra("listPosition", position - 1);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			intent.putExtra("repeatState", repeatState);//
			intent.putExtra("shuffleState", isShuffle);//
			intent.putExtra("mp3Infos", (Serializable) mp3Infos);
			startActivity(intent);
			// getActivity().startService(intent);
		}

	}
}
