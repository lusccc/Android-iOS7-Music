package com.stark.music.fragment.main;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.SearchListViewSongAdapter;
import com.stark.adapter.SongListViewAdapter;
import com.stark.blur.Blur;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.MainActivity;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.service.PlayerService;
import com.stark.util.Dip2Px;
import com.stark.util.SortCursor;
import com.stark.view.ClearEditText;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SongsFragment extends Fragment implements LoaderCallbacks<Cursor> {
	private View viewRoot;
	private View firstHeaderView;
	private View secondHeaderView;

	private LinearLayout.LayoutParams layoutParams;
	private ElasticListView elasticListView;
	private RelativeLayout listTitleRL;
	private RelativeLayout isPlayingRL;
	private RelativeLayout searchTextRL;
	private RelativeLayout titleBgRL;
	private SideBar sideBar;
	private/* Scrollable */ImageView mBlurredImageHeader;
	private SongListViewAdapter songListViewAdapter;
	private List<Mp3Info> mp3Infos = null;
	private TextView titleTextView;
	private TextView footerTV;

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isShuffle = true; // 随机播放
	private boolean isNoneShuffle = true; // 顺序播放

	private boolean isTurnOnNav = false; // 是否开启字母导航
	private int listPosition = 0; // 标识列表位置
	private HomeReceiver homeReceiver; // 自定义的广播接收器
	private int itemNum;

	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	private Cursor c;
	private Context context;

	// 一系列动作
	public static final String UPDATE_ACTION = "com.stark.action.UPDATE_ACTION"; // 更新动作
	public static final String CTL_ACTION = "com.stark.action.CTL_ACTION"; // 控制动作
	public static final String MUSIC_CURRENT = "com.stark.action.MUSIC_CURRENT"; // 当前音乐改变动作
	public static final String MUSIC_DURATION = "com.stark.action.MUSIC_DURATION"; // 音乐时长改变动作
	public static final String REPEAT_ACTION = "com.stark.action.REPEAT_ACTION"; // 音乐重复改变动作
	public static final String SHUFFLE_ACTION = "com.stark.action.SHUFFLE_ACTION"; // 音乐随机播放动作

	public static int mSongIdIndex, mSongTitleIndex, mArtistNameIndex,
			mAlbumNameIndex, mAlbumIdIndex, mDurationIndex, mUrlIndex;
	/** 可以提供给GridView **/
	public static Cursor mCursor;

	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";

	private int currentTime; // 当前时间
	private int duration; // 时长

	private int padding = 40;

	private int screenWidth, screenHeight;
	private int titleHegiht;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		viewRoot = inflater.inflate(R.layout.fragment_view_frame_rl, null);
		return viewRoot;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		context = getActivity();
		// Important!
		getLoaderManager().initLoader(0, null, this);
		initView();
		setUpView();
		setUpReceiver();
		setPlayStatReceiver();
		setViewClickListener();

	}

	public static Cursor getCursorForOut() {
		return mCursor;
	}

	public void setViewClickListener() {
		ViewOnClickListener listener = new ViewOnClickListener();
		isPlayingRL.setOnClickListener(listener);
	}

	public void setPlayStatReceiver() {
		PlayingReceiver receiver = new PlayingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOT_PLAYING_STAT);
		filter.addAction(PLAYING_STAT);
		getActivity().getApplicationContext()
				.registerReceiver(receiver, filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(homeReceiver);
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
		initSideBar();
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
		songListViewAdapter.changeCursor(sc);
		mCursor = sc;
		getMp3Infos();
		String string = this.getString(R.string.num_of_song);
		footerTV.setText(itemNum + string);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (songListViewAdapter != null)
			songListViewAdapter.changeCursor(null);
	}

	public void initView() {
		titleTextView = (TextView) viewRoot
				.findViewById(R.id.textView_fragment_title);
		elasticListView = (ElasticListView) viewRoot
				.findViewById(R.id.elasticListView_list);
		sideBar = (SideBar) viewRoot.findViewById(R.id.sideBar);
		isPlayingRL = (RelativeLayout) viewRoot
				.findViewById(R.id.RelativeLayout_frame_isPlaying);
		listTitleRL = (RelativeLayout) viewRoot
				.findViewById(R.id.list_title_RelativeLayout);
		mBlurredImageHeader = (/* Scrollable */ImageView) viewRoot
				.findViewById(R.id.blurred_image_header);
		titleBgRL = (RelativeLayout) viewRoot
				.findViewById(R.id.RelativeLayout_titlebg);

	}

	public void setUpView() {
		firstHeaderView = getFirstHeaderView();
		secondHeaderView = getSecondHeaderView();
		titleTextView.setText("歌曲");
        View v = View.inflate(getActivity(), R.layout.listview_empty_row, null);
        elasticListView.addHeaderView(v);
		elasticListView.addHeaderView(firstHeaderView);
		elasticListView.addHeaderView(secondHeaderView);
		elasticListView.addFooterView(getFirstFooterView());
		elasticListView.addFooterView(getSecondFooterView());
		screenWidth = MainActivity.getScreenWidth();
		screenHeight = MainActivity.getScreenHeight();
		//titleHegiht = Dip2Px.dip2px(getActivity(), 48);
		titleBgRL.getBackground().setAlpha(232);
		// mBlurredImageHeader.setScreenWidth(screenWidth);
		if (!PlayerService.isPlaying) {
			isPlayingRL.setVisibility(View.GONE);
		} else {
			isPlayingRL.setVisibility(View.VISIBLE);
		}
	}

	public View getSecondFooterView(){
		View view = 	View.inflate(getActivity(), R.layout.listview_empty_row, null);
		view.setClickable(false);
		return view;
	}

	public View getFirstFooterView() {
		View v = View.inflate(getActivity(), R.layout.footer_view, null);
		footerTV = (TextView) v.findViewById(R.id.textView_footer);
		v.setClickable(false);
		return v;
	}

	private class ViewOnClickListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.RelativeLayout_frame_isPlaying:
				enterPlayActivity();
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

	public View getFirstHeaderView() {
        View headerView = LayoutInflater.from(context).inflate(
                R.layout.search_ll, null);
		return headerView;
	}

	public View getSecondHeaderView() {
		View headView = LayoutInflater.from(context).inflate(
				R.layout.random_playrow_rl, null);
		return headView;
	}

	public void initSideBar() {
		songListViewAdapter = new SongListViewAdapter(getActivity(),
				R.layout.songs_list_item_layout, null, new String[] {},
				new int[] {}, 0);
		if (itemNum >= 50) { // 歌曲数目大于50开启导航
			isTurnOnNav = true;
			elasticListView.setPadding(0, 0, Dip2Px.px2dip(getActivity(), 60),
					0);
			elasticListView.setVerticalScrollBarEnabled(false);// 隐藏滚动条啦，，，天哪我要抑郁症了
			songListViewAdapter.setTurnOnNav(isTurnOnNav);
		} else {
			isTurnOnNav = false;
			songListViewAdapter.setTurnOnNav(isTurnOnNav);
			elasticListView.setVerticalScrollBarEnabled(true);
			sideBar.setVisibility(View.GONE);
		}

		songListViewAdapter.setTurnOnNav(isTurnOnNav);
		elasticListView.setAdapter(songListViewAdapter);
		elasticListView.setOnScrollListener(mScrollListener);
		elasticListView
				.setOnItemClickListener(new MusicListItemClickListener());
		sideBar.setListView(elasticListView);
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

	public void setUpReceiver() {
		homeReceiver = new HomeReceiver();
		// 创建IntentFilter
		IntentFilter filter = new IntentFilter();
		// 指定BroadcastReceiver监听的Action
		filter.addAction(UPDATE_ACTION);
		filter.addAction(MUSIC_CURRENT);
		filter.addAction(MUSIC_DURATION);
		filter.addAction(REPEAT_ACTION);
		filter.addAction(SHUFFLE_ACTION);
		// 注册BroadcastReceiver
		getActivity().registerReceiver(homeReceiver, filter);
		repeatState = isNoneRepeat; // 初始状态为无重复播放状态,后期改用数据库
	}

	/**
	 * 滚动时监听器
	 */
	OnScrollListener mScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_FLING:
				songListViewAdapter.setFlagBusy(true);

				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				songListViewAdapter.setFlagBusy(false);
				songListViewAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				songListViewAdapter.setFlagBusy(true);
				break;
			default:
				break;
			}

		}

		private SoftReference<Bitmap> bmp;
		private Bitmap bitmap;

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			/*
			 * bitmap = generateBluredBitmap(); bmp = new
			 * SoftReference<Bitmap>(bitmap);
			 * mBlurredImageHeader.setImageBitmap(bmp.get()); bitmap = null;
			 */
		}
	};

	public Bitmap generateBluredBitmap() {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 16;
		Bitmap bitmap = Bitmap.createBitmap(screenWidth, titleHegiht,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		elasticListView.draw(canvas);
		bitmap = Blur.fastblur(getActivity(), bitmap, 25);
		return bitmap;

	}

	private void updateView(Bitmap bmpBlurred) {
		bmpBlurred = Bitmap
				.createScaledBitmap(
						bmpBlurred,
						screenWidth,
						(int) (bmpBlurred.getHeight() * ((float) titleHegiht) / (float) bmpBlurred
								.getWidth()), false);
		// mBlurredImageHeader.setoriginalImage(bmpBlurred);
	}

	/**
	 * 点击列表播放音乐
	 */
	private class MusicListItemClickListener implements OnItemClickListener {
		private final int ANIM_DURATION = 150;
		private RelativeLayout searchTextRL;
		private RelativeLayout searchRL;
		private Animation tr1, tr2, scaleAnimation;
		private TextView searchTV;
		private ImageView searchBg;
		private EditText searchEditText;
		private AlertDialog dialog;
		private ElasticListView searchELV;
		private SearchListViewSongAdapter searchListViewAdapter;
		private Cursor cursor;
		private Bitmap blurbmp;
		private List<Mp3Info> searchMp3Infos;
		private MatrixCursor matrixCursor;

		public void findView(View view) {
			searchTextRL = (RelativeLayout) view
					.findViewById(R.id.search_text_RelativeLayout);
			searchRL = (RelativeLayout) view
					.findViewById(R.id.search_RelativeLayout);
			searchBg = (ImageView) getActivity().getWindow().getDecorView()
					.findViewById(R.id.imageView_blur);
		}

		public void setAnim(View view) {
			tr1 = new TranslateAnimation(0, 0, 0, -Dip2Px.dip2px(getActivity(),
					52));
			tr1.setDuration(ANIM_DURATION);
			tr1.setFillAfter(true);

			tr2 = new TranslateAnimation(0, (float) (-screenWidth / 2.65), 0, 0);
			tr2.setDuration(ANIM_DURATION);
			tr2.setFillAfter(true);

			scaleAnimation = new ScaleAnimation(0, 0, 0, 0.65f);
			scaleAnimation.setDuration(ANIM_DURATION);
			scaleAnimation.setFillAfter(true);
		}

		private Bitmap getSearcgBgBmp() {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 8;
			screenHeight = MainActivity.getScreenHeight();
			screenWidth = MainActivity.getScreenWidth();
			Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			getActivity().getWindow().getDecorView().draw(canvas);
			bitmap = Blur.fastblur(getActivity(), bitmap, 25);
			return bitmap;
		}

		@Override
		public void onItemClick(AdapterView<?> parent, final View view,
				int position, long id) {
			if (position > itemNum -1+2+1) {
				return;
			}
            if(position == 0){
                return;
            }
			if (position == 1) {
				findView(view);
				resetView();
				setAnim(view);
				listTitleRL.startAnimation(tr1);
				searchTextRL.startAnimation(tr2);
				elasticListView.startAnimation(tr1);
				tr2.setAnimationListener(new AnimationListener() {

					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {

						showDialog();
						makeViewGone();
						searchBg.bringToFront();

					}
				});
			} else if (position == 2) {
				startRandomPlay();
			} else {
				listPosition = position-1; // 获取列表点击的位置
				playMusic(listPosition);
			}

		}

		private void makeViewGone() {
			listTitleRL.setVisibility(View.GONE);
			searchTextRL.setVisibility(View.GONE);
			elasticListView.setVisibility(View.GONE);
			sideBar.setVisibility(View.GONE);
		}

		private void resetView() {
			listTitleRL.setVisibility(View.VISIBLE);
			searchTextRL.setVisibility(View.VISIBLE);
			elasticListView.setVisibility(View.VISIBLE);
			if(isTurnOnNav){
				sideBar.setVisibility(View.VISIBLE);
			}
			
		}

		private void showDialog() {
			dialog = new AlertDialog.Builder(getActivity())
					.setCancelable(false).create();
			dialog.show();
			dialog.setContentView(R.layout.search_dialog_rl);
			setDialogAttr();
			elasticListView.invalidate();
			setDialogListener();

		}

		private void setDialogListener() {
			dialog.findViewById(R.id.textView_search_cancel)
					.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							listTitleRL.clearAnimation();
							elasticListView.clearAnimation();
							searchTextRL.clearAnimation();
							searchTextRL.invalidate();
							listTitleRL.invalidate();
							elasticListView.invalidate();
							resetView();
							searchBg.setImageBitmap(null);
							dialog.dismiss();
						}
					});

		}

		/**
		 * 设置dialog属性
		 */
		private void setDialogAttr() {
			searchEditText = (ClearEditText) dialog
					.findViewById(R.id.clearEditText_search);
			Window window = dialog.getWindow();
			/** 弹出软键盘 **/
			window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

			/** 防止Activity挤变形 **/
			/*
			 * getActivity() .getWindow() .setSoftInputMode(
			 * WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN |
			 * WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
			 */
			/** 设置dialog宽度 **/
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.width = Dip2Px.dip2px(getActivity(), 360);
			lp.gravity = Gravity.TOP;
			window.setAttributes(lp);
			window.setWindowAnimations(R.anim.empty);
			setList(window);
			addEditTextListener();

		}

		private void setList(Window window) {
			searchELV = (ElasticListView) dialog
					.findViewById(R.id.elasticListView_search);
			searchListViewAdapter = new SearchListViewSongAdapter(
					getActivity(), R.layout.songs_list_item_layout, null,
					new String[] {}, new int[] {}, 0);
			searchELV.setAdapter(searchListViewAdapter);
			new Handler().post(new Runnable() {

				@Override
				public void run() {
					blurbmp = getSearcgBgBmp();
					searchBg.setImageBitmap(blurbmp);
				}

			});

			searchELV.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					playSearchMusic(matrixCursor, position);
				}
			});
		}

		private void playSearchMusic(Cursor cursor, int position) {
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
			// 添加一系列要传递的数据
			String title = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			cursor.moveToPosition(position);
			Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
			intent.putExtra("title", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE)));
			intent.putExtra("url", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA)));
			intent.putExtra("artist", cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
			for (int i = 0; i < mp3Infos.size(); i++) {
				if (title.equals(mp3Infos.get(i).getTitle())) {
					position = i;
				}
			}
			intent.putExtra("listPosition", position);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			intent.putExtra("repeatState", repeatState);//
			intent.putExtra("shuffleState", isShuffle);//
			intent.putExtra("mp3Infos", (Serializable) mp3Infos);// 别传递错了
			startActivity(intent);
		}

		private void addEditTextListener() {
			searchEditText.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start,
						int before, int count) {
					String[] colums = { MediaStore.Audio.Media.DATA,// 歌曲文件的路径
							MediaStore.Audio.Media._ID,// 歌曲ID
							MediaStore.Audio.Media.TITLE,// 歌曲标题
							MediaStore.Audio.Media.ARTIST,// 歌曲的歌手名
							MediaStore.Audio.Media.ALBUM,// 歌曲的唱片集
							MediaStore.Audio.Media.DURATION, // 歌曲的总播放时长
							MediaStore.Audio.Media.ALBUM_ID };
					cursor = getActivity().getContentResolver().query(
							MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
							colums, null, null,
							MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
					changeContainCursor(s.toString(), colums);
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {

				}

				@Override
				public void afterTextChanged(Editable s) {

				}
			});
		}

		private void changeContainCursor(String contain, String[] colums) {
			if (contain.equals("")) {
				searchListViewAdapter.changeCursor(null);
				return;
			}
			String containWords = null;
			matrixCursor = new MatrixCursor(colums);
			while (cursor.moveToNext()) {
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				String wordReg = "(?i)" + contain;// 用(?i)来忽略大小写
				Matcher matcher = Pattern.compile(wordReg).matcher(title);

				if (matcher.find()) {
					containWords = matcher.group();
					searchListViewAdapter.setContainWords(containWords);
					Log.e(containWords, "");
					String url = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
					long id = cursor.getLong(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
					String album = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
					String artist = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
					long albumId = cursor
							.getLong(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
					long duration = cursor
							.getLong(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
					matrixCursor.addRow(new Object[] { url, id, title, artist,
							album, duration, albumId });

				} else {
					searchListViewAdapter.changeCursor(null);
				}
			}
			searchListViewAdapter.changeCursor(matrixCursor);
			searchListViewAdapter.notifyDataSetChanged();

		}
	}

	/**
	 * 
	 * @param listPosition
	 */
	private void playMusic(int listPosition) {
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
		Mp3Info mp3Info = mp3Infos.get(listPosition - 2);
		// 添加一系列要传递的数据
		Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
		intent.putExtra("title", mp3Info.getTitle());
		intent.putExtra("url", mp3Info.getUrl());
		intent.putExtra("artist", mp3Info.getArtist());
		intent.putExtra("listPosition", listPosition - 2);
		intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
		intent.putExtra("repeatState", repeatState);//
		intent.putExtra("shuffleState", isShuffle);//
		intent.putExtra("mp3Infos", (Serializable) mp3Infos);
		startActivity(intent);
	}

	public void startRandomPlay() {
        if(mCursor.getCount() == 0){
            return;
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

	// 自定义的BroadcastReceiver，负责监听从Service传回来的广播
	public class HomeReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MUSIC_CURRENT)) {
				// currentTime代表当前播放的时间
				currentTime = intent.getIntExtra("currentTime", -1);
			} else if (action.equals(MUSIC_DURATION)) {
				duration = intent.getIntExtra("duration", -1);
			} else if (action.equals(UPDATE_ACTION)) {
				// 获取Intent中的current消息，current代表当前正在播放的歌曲
				listPosition = intent.getIntExtra("current", -1);
				if (listPosition >= 0) {
				}
			} else if (action.equals(REPEAT_ACTION)) {
				repeatState = intent.getIntExtra("repeatState", -1);
				switch (repeatState) {
				case isCurrentRepeat: // 单曲循环
					break;
				case isAllRepeat: // 全部循环
					break;
				case isNoneRepeat: // 无重复
					break;
				}
			} else if (action.equals(SHUFFLE_ACTION)) {
				isShuffle = intent.getBooleanExtra("shuffleState", false);
				if (isShuffle) {
					isNoneShuffle = false;
				} else {
					isNoneShuffle = true;
				}
			}
		}

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

	/**
	 * 退出程序
	 */
	private void exit() {
		Intent intent = new Intent(getActivity(), PlayerService.class);
		getActivity().stopService(intent);
		getActivity().finish();
	}

}
