package com.stark.music.fragment.addmusic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.SongListViewAdapterForAddActivity;
import com.stark.database.DBHelperofList;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.fragment.main.NewPlayListDialogFragment;
import com.stark.service.PlayerService;
import com.stark.util.Dip2Px;
import com.stark.util.SortCursor;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

public class SongsFragmentInAddActivity extends Fragment implements
		LoaderCallbacks<Cursor> {
	private View view;
	private LinearLayout.LayoutParams layoutParams;
	private ElasticListView elasticListView;
	private LinearLayout headView;
	private LinearLayout footView;
	private SideBar sideBar;
	private SongListViewAdapterForAddActivity slvaAdapter;
	private List<Mp3Info> mp3Infos = null;
	public static Collection<Integer> choosedSongs = new HashSet<Integer>();
	
	private TextView titleTextView;
	private TextView doneBtnTV;

	private boolean isTurnOnNav; // 是否开启字母导航
	private int listPosition = 0; // 标识列表位置
	private int itemNum;

	private DBHelperofPlayState dbHelperofPlayState;
	private DBHelperofList dbHelperofList;
	private SQLiteDatabase db;
	private Cursor c;
	private Cursor mCursor;
	private Context context;


	public static int mSongIdIndex, mSongTitleIndex, mArtistNameIndex,
			mAlbumNameIndex, mAlbumIdIndex, mDurationIndex, mUrlIndex;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_view_frame_add_rl, null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		context = getActivity();
		// Important!
		getLoaderManager().initLoader(0, null, this);
		initView();
		setUpView();
		
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
		return new CursorLoader(getActivity(), uri, null, null, null, sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (data == null) {
			return;
		}
		itemNum = data.getCount();
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
		Log.e(mSongTitleIndex+"", mArtistNameIndex+"");
		mCursor = sc;
		slvaAdapter.changeCursor(sc);
		getMp3Infos();
		initSideBar();
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (slvaAdapter != null)
			slvaAdapter.changeCursor(null);
	}

	public void initView() {

		titleTextView = (TextView) view
				.findViewById(R.id.textView_fragment_title);
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_list);
		sideBar = (SideBar) view.findViewById(R.id.sideBar);
		doneBtnTV = (TextView) view.findViewById(R.id.textView_title_done);

	}

	public void setUpView() {
		titleTextView.setText(R.string.add_music);
		//elasticListView.addHeaderView(getFirstHeaderView());
		elasticListView.addFooterView(View.inflate(getActivity(),
				R.layout.footer_view, null));
		slvaAdapter = new SongListViewAdapterForAddActivity(getActivity(),
				R.layout.songs_add_list_item_layout, null, new String[] {},
				new int[] {}, 0);
		doneBtnTV.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addMusic2DB();
				getActivity().finish();

			}
		});
	}

	public View getFirstHeaderView() {
		View headView = LayoutInflater.from(context).inflate(
				R.layout.search_ll, null);
		headView.setPadding(Dip2Px.px2dip(context, 40),
				Dip2Px.px2dip(context, 480), Dip2Px.px2dip(context, 40),
				Dip2Px.px2dip(context, 30));
		return headView;
	}

	public void initSideBar() {
		if (itemNum >= 50) { // 歌曲数目大于50开启导航
			isTurnOnNav = true;
			slvaAdapter.setTurnOnNav(isTurnOnNav);
			elasticListView.setPadding(0, 0, Dip2Px.px2dip(getActivity(), 60),
					0);
			elasticListView.setVerticalScrollBarEnabled(false);// 隐藏滚动条啦，，，天哪我要抑郁症了
		} else {
			isTurnOnNav = false;
			slvaAdapter.setTurnOnNav(isTurnOnNav);
			elasticListView.setVerticalScrollBarEnabled(true);
		}
		
		elasticListView.setAdapter(slvaAdapter);
		elasticListView.setOnScrollListener(mScrollListener);
		elasticListView
				.setOnItemClickListener(new MusicListItemClickListener());
		sideBar.setListView(elasticListView);
	}

	/***
	 * 构造专辑内歌曲的集合
	 */
	public void getMp3Infos() {
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
	}


	/**
	 * 滚动时监听器
	 */
	OnScrollListener mScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_FLING:
				long startTime = System.currentTimeMillis(); // 获取开始时间
				long endTime = System.currentTimeMillis(); // 获取结束时间
				slvaAdapter.setFlagBusy(true);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				slvaAdapter.setFlagBusy(false);
				slvaAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				slvaAdapter.setFlagBusy(true);
				break;
			default:
				break;
			}

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// TODO Auto-generated method stub

		}
	};

	/**
	 * 点击列表
	 */
	private class MusicListItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			
				choosedSongs.add(position );
				view.setClickable(false);
				ImageView add_circle = (ImageView) view
						.findViewById(R.id.imageView_add_circle);
				RelativeLayout rl = (RelativeLayout) view
						.findViewById(R.id.RelativeLayout_song_list_add_root);
				rl.setBackgroundDrawable(null);
				add_circle.setImageResource(R.drawable.add_circle_pressed);

		}

	}



	/**
	 * 往数据库添加每首歌的信息
	 * 
	 * @param position
	 */
	private void addMusic2DB() {
		dbHelperofList = new DBHelperofList(getActivity());
		Iterator<Integer> it = choosedSongs.iterator();
		for (int i = 0; i < choosedSongs.size()&&it.hasNext(); i++) {
			Mp3Info mp3Info = mp3Infos.get(it.next());
			dbHelperofList.insertMusicInfo(getActivity().getIntent()
					.getStringExtra("listName"), mp3Info.getId(), mp3Info
					.getTitle(), mp3Info.getArtist(), mp3Info.getAlbum(),
					mp3Info.getAlbumId(), mp3Info.getDuration(), mp3Info
							.getUrl());
		}

	}


}
