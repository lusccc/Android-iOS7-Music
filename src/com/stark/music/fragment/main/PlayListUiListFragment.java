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
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.AlbumUiListViewAdapter;
import com.stark.database.DBHelperofList;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.service.PlayerService;
import com.stark.util.Dip2Px;
import com.stark.view.ElasticListView;

public class PlayListUiListFragment extends Fragment {

	private static Bundle bundle;

	private TextView artistTitleTV;
	private TextView editTV;
	private TextView cleanTV;
	private TextView deleteTV;
	private RelativeLayout backRL;
	
	public RelativeLayout isPlayingRL;
	
	private View view;
	private String _listName = null;
	private ElasticListView elasticListView;
	private Cursor cursor;
	private AlbumUiListViewAdapter auvAdapter;
	private List<Mp3Info> mp3Infos;
	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	private SQLiteDatabase db2;
	private Cursor dbCursor;
	private DBHelperofList dbHelperofList;
	private Context context;

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isShuffle = true; // 随机播放
	
	private Cursor c;
	
	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";

	static PlayListUiListFragment newInstance(Bundle b) {
		PlayListUiListFragment abmUiListFragment = new PlayListUiListFragment();
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
		view = inflater.inflate(R.layout.fragment_inner_ui_playlist, null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		context = getActivity();
		getDataFromBundle();
		query();
		initView();
		setupView();
		setViewClickListener();
		setPlayStatReceiver();
		mp3Infos = getMp3Infos();
	}

	public void initView() {
		artistTitleTV = (TextView) view
				.findViewById(R.id.textView_fragment_title_uilist);
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_albumui_list);
		auvAdapter = new AlbumUiListViewAdapter(getActivity(), cursor);
		cleanTV = (TextView)view.findViewById(R.id.textView_clean_title_uilist);
		deleteTV = (TextView)view.findViewById(R.id.textView_title_uilist_delete);
		isPlayingRL = (RelativeLayout)view.findViewById(R.id.RelativeLayout_isPlaying_playlistui);
		backRL = (RelativeLayout)view.findViewById(R.id.RelativeLayout_back_playlist);
	}
	
	public void setViewClickListener() {
		ViewOnClickListener listener = new ViewOnClickListener();
		isPlayingRL.setOnClickListener(listener);
		backRL.setOnClickListener(listener);
		deleteTV.setOnClickListener(listener);
		cleanTV.setOnClickListener(listener);
	}
	
	public void setPlayStatReceiver() {
		PlayingReceiver receiver = new PlayingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOT_PLAYING_STAT);
		filter.addAction(PLAYING_STAT);
		getActivity().getApplicationContext()
				.registerReceiver(receiver, filter);
	}
	public void getDataFromBundle() {
		_listName = bundle.getString("list_table_name");
		
	}

	public void query() {
		dbHelperofList = new DBHelperofList(getActivity());
		db2 = dbHelperofList.getWritableDatabase();
		cursor = db2.query(_listName, null, null, null, null, null,
				"_id asc");
	}

	public void setupView() {
		View emptyView = View.inflate(getActivity(), R.layout.listview_empty_rowx2, null);
		String listName = _listName.substring(1,_listName.length());//去除表名前的"_"
		/**add两个empty HeaderView 会出现奇怪的闪屏问题I do not know why**/
		elasticListView.addHeaderView(emptyView);
		elasticListView.addHeaderView(getFirstHeaderView());
		elasticListView.addFooterView(emptyView);
		artistTitleTV.setText(listName);
		elasticListView.setAdapter(auvAdapter);
		elasticListView.setOnItemClickListener(new PlayListUilListClickListener());
		if (!PlayerService.isPlaying) {
			isPlayingRL.setVisibility(View.GONE);
		} else {
			isPlayingRL.setVisibility(View.VISIBLE);
		}
	}

	public View getFirstHeaderView() {
		View headerView = LayoutInflater.from(context).inflate(
				R.layout.random_playrow_rl, null);
		return headerView;
	}

	/***
	 * 构造专辑内歌曲的集合
	 */
	public List<Mp3Info> getMp3Infos() {
		List<Mp3Info> mp3Infos = new ArrayList<Mp3Info>();
		while (cursor.moveToNext()) {
			Mp3Info mp3Info = new Mp3Info();
				long albumId = cursor.getLong(cursor.getColumnIndex("album_id"));
				long songId = cursor.getLong(cursor.getColumnIndex("id"));
				String title = cursor.getString(cursor.getColumnIndex("title"));
				String artist = cursor.getString(cursor.getColumnIndex("artist"));
				String album = cursor.getString(cursor.getColumnIndex("album"));
				long duration = cursor.getLong(cursor.getColumnIndex("duration"));
				String url = cursor.getString(cursor.getColumnIndex("url"));
				mp3Info.setAlbumId(albumId);
				mp3Info.setId(songId);
				mp3Info.setTitle(title);
				mp3Info.setArtist(artist);
				mp3Info.setAlbum(album);
				mp3Info.setDuration(duration);
				mp3Info.setUrl(url);
				mp3Infos.add(mp3Info);
		}
		db2.close();
		return mp3Infos;
	}

	private class PlayListUilListClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position <1)
				return;
			if(position == 1){
				startRandomPlay();
				return;
			}
			Mp3Info mp3Info = mp3Infos.get(position - 2);
			// 添加一系列要传递的数据
			Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
			intent.putExtra("title", mp3Info.getTitle());
			intent.putExtra("url", mp3Info.getUrl());
			intent.putExtra("artist", mp3Info.getArtist());
			intent.putExtra("listPosition", position - 2);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			intent.putExtra("repeatState", repeatState);//
			intent.putExtra("shuffleState", isShuffle);//
			intent.putExtra("mp3Infos", (Serializable) mp3Infos);
			startActivity(intent);
		}

	}
	protected int getRandomIndex(int end) {
		int index = (int) (Math.random() * end);
		return index;
	}
	public void startRandomPlay() {
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
		int position = getRandomIndex(mp3Infos.size() - 1);
		Mp3Info mp3Info = mp3Infos.get(position);
		// 添加一系列要传递的数据
		Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
		intent.putExtra("title", mp3Info.getTitle());
		intent.putExtra("url", mp3Info.getUrl());
		intent.putExtra("artist", mp3Info.getArtist());
		intent.putExtra("listPosition", position);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
		intent.putExtra("repeatState", repeatState);//
		intent.putExtra("shuffleState", isShuffle);//
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		startActivity(intent);
	}
	private class ViewOnClickListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.textView_clean_title_uilist:
				dbHelperofList.cleanPlayList(_listName);
				query();
				auvAdapter = new AlbumUiListViewAdapter(getActivity(), cursor);
				auvAdapter.notifyDataSetChanged();
				elasticListView.setAdapter(auvAdapter);
				break;
			case R.id.textView_title_uilist_delete:
				deleteTV.setPressed(true);
				getActivity().getFragmentManager().popBackStack();
				//异步有一个问题 弾栈的时候fragment列表会有残留
				dbHelperofList.deletePlayList(_listName);
				break;
			case R.id.RelativeLayout_isPlaying_playlistui:
				enterPlayActivity();
				break;
			case R.id.RelativeLayout_back_playlist:
				backRL.setPressed(true);
				getActivity().getFragmentManager().popBackStack();
				break;
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
	
	
}
