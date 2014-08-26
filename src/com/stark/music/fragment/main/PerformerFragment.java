package com.stark.music.fragment.main;

import java.io.Serializable;
import java.io.ObjectInputStream.GetField;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Audio.ArtistColumns;
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
import android.view.animation.AlphaAnimation;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stark.adapter.PerformerListViewAdapter;
import com.stark.adapter.SearchListViewPerformerAdapter;
import com.stark.adapter.SearchListViewSongAdapter;
import com.stark.blur.Blur;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.activity.MainActivity;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.service.PlayerService;
import com.stark.util.Dip2Px;
import com.stark.util.MediaUtil;
import com.stark.util.SortCursor;
import com.stark.view.ClearEditText;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

public class PerformerFragment extends Fragment implements
		LoaderCallbacks<Cursor> {
	private ElasticListView elasticListView;
	private View view;
	private PerformerListViewAdapter pfmAdapter;
	private Cursor mCursor;
	private SideBar sideBar;
	private Context context;
	private boolean isTurnOnNav = false; // 是否开启字母导航
	private int itemNum;
	private RelativeLayout isPlayingRL;
	private RelativeLayout titleBgRL;
	private RelativeLayout listTitleRL;
	private TextView artistTitle;
	private TextView footerTV;
	
	private DBHelperofPlayState dbHelperofPlayState;
	private SQLiteDatabase db;
	private Cursor c;

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isShuffle = false; // 随机播放
	private boolean isNoneShuffle = true; // 顺序播放

	private int screenWidth, screenHeight;

	// Audio columns
	public static int mAlbumNameIndex, mSongNumIndex, mAlbumNumIndex,
			mArtistNameIndex, mIdIndex;
	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// mp3Infos = MediaUtil.getMp3Infos(getActivity()); // 获取歌曲对象集合
		// getAlbumListInfos();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		context = getActivity();
		view = inflater.inflate(R.layout.fragment_view_frame_rl, null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initView();
		setUpView();
		setReceiver();
		setViewClickListener();
		// Important!
		getLoaderManager().initLoader(0, null, this);
	}

	public void initView() {

		sideBar = (SideBar) view.findViewById(R.id.sideBar);
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_list);
		isPlayingRL = (RelativeLayout) view
				.findViewById(R.id.RelativeLayout_frame_isPlaying);
		artistTitle = (TextView) view
				.findViewById(R.id.textView_fragment_title);
		titleBgRL = (RelativeLayout) view
				.findViewById(R.id.RelativeLayout_titlebg);
		listTitleRL = (RelativeLayout) view
				.findViewById(R.id.list_title_RelativeLayout);

	}

	public void setUpView() {
		View v = View.inflate(getActivity(), R.layout.listview_empty_row, null);
		elasticListView.addHeaderView(v);
		elasticListView.addHeaderView(getHeaderView());
		elasticListView.addFooterView(getFirstFooterView());
		elasticListView.addFooterView(getSecondFooterView());
		pfmAdapter = new PerformerListViewAdapter(getActivity(),
				R.layout.performer_list_item_layout, null, new String[] {},
				new int[] {}, 0);
		elasticListView.setAdapter(pfmAdapter);
		elasticListView.setOnScrollListener(mScrollListener);
		elasticListView
				.setOnItemClickListener(new PerformerListItemClickListener());
		sideBar.setListView(elasticListView);
		artistTitle.setText(R.string.performer);
		screenWidth = MainActivity.getScreenWidth();
		screenHeight = MainActivity.getScreenHeight();
		titleBgRL.getBackground().setAlpha(230);
		if (!PlayerService.isPlaying) {
			isPlayingRL.setVisibility(View.GONE);
		} else {
			isPlayingRL.setVisibility(View.VISIBLE);
		}
	}
	public View getSecondFooterView(){
		return View.inflate(getActivity(), R.layout.listview_empty_row, null);
	}
	public View getFirstFooterView(){
		View v = View.inflate(getActivity(), R.layout.footer_view, null);
		footerTV = (TextView)v.findViewById(R.id.textView_footer);
		return v;
	}
	public View getHeaderView() {
		View headView = LayoutInflater.from(context).inflate(
				R.layout.search_ll, null);
		/*headView.setPadding(Dip2Px.px2dip(context, 40),
				Dip2Px.px2dip(context, 480), Dip2Px.px2dip(context, 40),
				Dip2Px.px2dip(context, 30));*/
		return headView;
	}

	public void setReceiver() {
		PlayingReceiver receiver = new PlayingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOT_PLAYING_STAT);
		filter.addAction(PLAYING_STAT);
		getActivity().getApplicationContext()
				.registerReceiver(receiver, filter);
	}

	public void setViewClickListener() {
		ViewOnClickListener listener = new ViewOnClickListener();
		isPlayingRL.setOnClickListener(listener);
	}

	public void initSideBar() {
		if (itemNum > 30) {
			isTurnOnNav = true;
			pfmAdapter.setTurnOnNav(isTurnOnNav);
			elasticListView.setPadding(0, 0, Dip2Px.px2dip(getActivity(), 60),
					0);
			elasticListView.setVerticalScrollBarEnabled(false);// 隐藏滚动条啦，，，天哪我要抑郁症了

		} else {
			isTurnOnNav = false;
			pfmAdapter.setTurnOnNav(isTurnOnNav);
			sideBar.setVisibility(View.GONE);
			elasticListView.setVerticalScrollBarEnabled(true);
		}
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] projection = {
				BaseColumns._ID,// 必须加入_id列 否则无法查询
				Audio.ArtistColumns.ARTIST,
				Audio.ArtistColumns.NUMBER_OF_ALBUMS,
				Audio.ArtistColumns.NUMBER_OF_TRACKS, /* Audio.Media.ALBUM */};
		Uri uri = Audio.Artists.EXTERNAL_CONTENT_URI;
		String sortOrder = Audio.Artists.DEFAULT_SORT_ORDER/*
															 * Audio.Albums.ALBUM
															 */;

		return new CursorLoader(getActivity(), uri, projection, null, null,
				sortOrder);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		// Check for database errors
		if (data == null) {
			return;
		}
		itemNum = data.getCount();
		initSideBar();
		mIdIndex = data.getColumnIndexOrThrow(BaseColumns._ID);
		mArtistNameIndex = data.getColumnIndexOrThrow(ArtistColumns.ARTIST);
		mSongNumIndex = data
				.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_TRACKS);
		mAlbumNumIndex = data
				.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_ALBUMS);
		// mAlbumNameIndex = data.getColumnIndexOrThrow(Audio.Media.ALBUM);
		SortCursor sc = new SortCursor(data, ArtistColumns.ARTIST);
		pfmAdapter.changeCursor(sc);
		mCursor = sc;
		String string = this.getString(R.string.num_of_performer);
		footerTV.setText(itemNum+string);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (pfmAdapter != null)
			pfmAdapter.changeCursor(null);
	}

	/**
	 * 滚动时监听器
	 */
	OnScrollListener mScrollListener = new OnScrollListener() {
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_FLING:
				pfmAdapter.setFlagBusy(true);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				pfmAdapter.setFlagBusy(false);
				pfmAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				pfmAdapter.setFlagBusy(true);
				break;
			default:
				break;
			}

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {

		}
	};

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

	private class PerformerListItemClickListener implements OnItemClickListener {
		private final int ANIM_DURATION = 150;
		private RelativeLayout searchTextRL;
		private RelativeLayout searchRL;
		private Animation tr1, tr2, scaleAnimation;
		private TextView searchTV;
		private ImageView searchBg;
		private EditText searchEditText;
		private AlertDialog dialog;
		private ElasticListView searchELV;
		private SearchListViewPerformerAdapter slvpAdapter;
		private Cursor cursor;
		private Bitmap blurbmp;
		private List<Mp3Info> searchMp3Infos;
		private MatrixCursor matrixCursor;
		private List<Mp3Info> mp3Infos = MediaUtil.getMp3Infos(getActivity());

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
			options.inSampleSize = 1;
			Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight,
					Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			getActivity().getWindow().getDecorView().draw(canvas);
			bitmap = Blur.fastblur(getActivity(), bitmap, 25);
			return bitmap;
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
			slvpAdapter = new SearchListViewPerformerAdapter(getActivity(),
					R.layout.songs_list_item_layout, null, new String[] {},
					new int[] {}, 0);
			searchELV.setAdapter(slvpAdapter);
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
				slvpAdapter.changeCursor(null);
				return;
			}
			String containWords = null;
			matrixCursor = new MatrixCursor(colums);
			while (cursor.moveToNext()) {
				String artist = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				String wordReg = "(?i)" + contain;// 用(?i)来忽略大小写
				Matcher matcher = Pattern.compile(wordReg).matcher(artist);

				if (matcher.find()) {
					containWords = matcher.group();
					slvpAdapter.setContainWords(containWords);
					String title = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
					String url = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
					long id = cursor.getLong(cursor
							.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
					String album = cursor
							.getString(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));

					long albumId = cursor
							.getLong(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
					long duration = cursor
							.getLong(cursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
					matrixCursor.addRow(new Object[] { url, id, title, artist,
							album, duration, albumId });
				} else {
					slvpAdapter.changeCursor(null);
				}
			}
			slvpAdapter.changeCursor(matrixCursor);
			slvpAdapter.notifyDataSetChanged();

		}

		@SuppressLint("NewApi")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (position == itemNum+1){
				//最后一项,footerview!!!!!
				return;
			}
			if (position == 0) {
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
			} else {
				showTitleAnim();
				view.setPressed(true);
				mCursor.moveToPosition(position - 1);
				String album = mCursor.getString(mAlbumNameIndex);
				String songNum = mCursor.getString(mAlbumNumIndex);
				String artistName = mCursor.getString(mArtistNameIndex);
				Bundle bundle = new Bundle();
				bundle.putString("album_name", album);
				bundle.putString("song_num", songNum);
				bundle.putString("artist_name", artistName);
				PerformerUiListFragment pfmUiFragment = PerformerUiListFragment
						.newInstance(bundle);
				FragmentManager fm = getActivity().getFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.addToBackStack(null);
				ft.setCustomAnimations(R.anim.fragment_slide_in_left,
						R.anim.fragment_silde_out_right,
						R.anim.fragment_slide_right_enter,
						R.anim.fragment_slide_right_exit);
				ft.replace(R.id.content, pfmUiFragment).commit();
			}
		}
	}

	/**
	 * fragment切换动画
	 */
	public void showTitleAnim() {
		Animation alphaAnimation = new AlphaAnimation(1f, 0f);
		alphaAnimation.setDuration(250);
		artistTitle.startAnimation(alphaAnimation);
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
