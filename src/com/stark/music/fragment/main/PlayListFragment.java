package com.stark.music.fragment.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stark.adapter.PlayListViewAdapter;
import com.stark.adapter.SearchListViewSongAdapter;
import com.stark.blur.Blur;
import com.stark.database.DBHelperofList;
import com.stark.database.DBHelperofPlayState;
import com.stark.domain.AppConstant;
import com.stark.domain.Mp3Info;
import com.stark.domain.PlayListInfo;
import com.stark.music.R;
import com.stark.music.activity.AddMusicToListActivity;
import com.stark.music.activity.MainActivity;
import com.stark.music.activity.PlayMusicActivity;
import com.stark.service.PlayerService;
import com.stark.util.Dip2Px;
import com.stark.util.MediaUtil;
import com.stark.view.ClearEditText;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

public class PlayListFragment extends Fragment {
	private View view;
	private LinearLayout.LayoutParams layoutParams;
	private RelativeLayout isPlayingRL;
	private RelativeLayout listTitleRL;
	private TextView titleTV;
	private ElasticListView elasticListView;
	
	private  PlayListViewAdapter playListViewAdapter;
	
	private DBHelperofList dbHelper;
	private SQLiteDatabase db;
	private Intent intent;
	private Cursor c;
	private List<Mp3Info> mp3Infos = null;
	private List<PlayListInfo> listInfos;
	private Handler handler;
	private SideBar sideBar;
	private Context context;
	private DBHelperofPlayState dbHelperofPlayState;

	private int repeatState; // 循环标识
	private final int isCurrentRepeat = 1; // 单曲循环
	private final int isAllRepeat = 2; // 全部循环
	private final int isNoneRepeat = 3; // 无重复播放
	private boolean isShuffle = false; // 随机播放
	private boolean isNoneShuffle = true; // 顺序播放

	private final static String LISTNAMETABLE_NAME = "list_name";
	public static final String PLAYING_STAT = "com.stark.action.IS_PLAYING";
	public static final String NOT_PLAYING_STAT = "com.stark.action.NOT_PLAYING_STAT";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.fragment_view_frame_rl, null);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		context = getActivity();
		initView();
		setHeaderView();
		setFooterView();
	}

	public void setReceiver() {
		PlayingReceiver receiver = new PlayingReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(NOT_PLAYING_STAT);
		filter.addAction(PLAYING_STAT);
		getActivity().getApplicationContext()
				.registerReceiver(receiver, filter);
	}

	public void initView() {
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_list);
		sideBar = (SideBar) view.findViewById(R.id.sideBar);
		isPlayingRL = (RelativeLayout) view
				.findViewById(R.id.RelativeLayout_frame_isPlaying);
		titleTV = (TextView)view.findViewById(R.id.textView_fragment_title);
		listTitleRL = (RelativeLayout) view
				.findViewById(R.id.list_title_RelativeLayout);
	}

	public void setUpView() {
		playListViewAdapter = new PlayListViewAdapter(getActivity(), listInfos);
		elasticListView.setOnItemClickListener(new PLayListOnClickListener());
		elasticListView.setAdapter(playListViewAdapter);
		sideBar.setVisibility(View.GONE);
		if (!PlayerService.isPlaying) {
			isPlayingRL.setVisibility(View.GONE);
		}
	}

	public void setHeaderView() {
		View v = View.inflate(getActivity(), R.layout.listview_empty_row, null);
		elasticListView.addHeaderView(v);
		elasticListView.addHeaderView(getFirstHeaderView());
		elasticListView.addHeaderView(getSecondHeaderView());
	}
	public void setFooterView(){
		elasticListView.addFooterView(View.inflate(getActivity(), R.layout.listview_empty_row,null));
	}

	public void setViewClickListener() {
		ViewOnClickListener listener = new ViewOnClickListener();
		isPlayingRL.setOnClickListener(listener);
	}

	public View getFirstHeaderView() {
		View headerView = LayoutInflater.from(context).inflate(
				R.layout.search_ll, null);

		/*headerView.setPadding(Dip2Px.px2dip(context, 40), 0,
				Dip2Px.px2dip(context, 40), Dip2Px.px2dip(context, 30));*/
		return headerView;
	}

	public View getSecondHeaderView() {
		View headerView = LayoutInflater.from(context).inflate(
				R.layout.new_playlist_scrollrow_rl, null);
		return headerView;
	}

	/***
	 * 可以更新ui的
	 */
	public void onResume() {
		listInfos = getListItemsFromDB();
		setUpView();
		setViewClickListener();
		super.onResume();
	}

	/***
	 * 把数据库中的列表信息转入集合
	 * 
	 * @return
	 */
	public List<PlayListInfo> getListItemsFromDB() {
		List<PlayListInfo> listInfos = new ArrayList<PlayListInfo>();
		dbHelper = new DBHelperofList(getActivity());
		db = dbHelper.getWritableDatabase();

		c = db.query(LISTNAMETABLE_NAME, null, null, null, null, null,
				"_id DESC");

		while (c.moveToNext()) {
			PlayListInfo p = new PlayListInfo();
			String _listName = c.getString(c.getColumnIndex("list_name"));
			String listName = _listName.substring(1, _listName.length());// 去除表名前的"_"
			p.setListName(listName);
			Cursor c2 = db.query(_listName, null, null, null, null, null,
					"_id asc");
			if (c2.moveToFirst()) {
				long albumId = c2.getLong(c2.getColumnIndex("album_id"));
				long songId = c2.getLong(c2.getColumnIndex("id"));
				p.setAlbumId(albumId);
				p.setSongId(songId);
			}
			listInfos.add(p);
		}

		c.close();
		db.close();
		return listInfos;

	}

	/***
	 * 列表监听器类
	 * 
	 * @author Administrator
	 * 
	 */
	public class PLayListOnClickListener implements OnItemClickListener {
		private AlertDialog dialog;
		private EditText newPlaylistED;
		private TextView dialogSaveTV;
		private TextView dialogCancelTV;
		private String listName;
		
		private final int ANIM_DURATION = 150;
		private RelativeLayout searchTextRL;
		private RelativeLayout searchRL;
		private Animation tr1, tr2, scaleAnimation;
		private TextView searchTV;
		private ImageView searchBg;
		private EditText searchEditText;
		private AlertDialog dialog_search;
		private ElasticListView searchELV;
		private SearchListViewSongAdapter searchListViewAdapter;
		private Cursor cursor;
		private Bitmap blurbmp;
		private MatrixCursor matrixCursor;
		
		private int  screenWidth;
		private int screenHeight ;
		
		private List<Mp3Info> search_mp3Infos;

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
		}

		private void showSearchDialog() {
			dialog = new AlertDialog.Builder(getActivity())
					.setCancelable(false).create();
			dialog.show();
			dialog.setContentView(R.layout.search_dialog_rl);
			setSearchDialogAttr();
			elasticListView.invalidate();
			setSearchDialogListener();

		}

		private void setSearchDialogListener() {
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
		private void setSearchDialogAttr() {
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
			searchListViewAdapter = new SearchListViewSongAdapter(getActivity(),
					R.layout.songs_list_item_layout, null, new String[] {},
					new int[] {}, 0);
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
		
		private void playSearchMusic(Cursor cursor,int position){
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
			String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
			cursor.moveToPosition(position);
			Intent intent = new Intent(getActivity(), PlayMusicActivity.class);
			intent.putExtra("title", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
			intent.putExtra("url", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
			intent.putExtra("artist", cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
			search_mp3Infos = MediaUtil.getMp3Infos(getActivity());
			for(int i = 0;i<search_mp3Infos.size();i++){
				if(title.equals(search_mp3Infos.get(i).getTitle())){
					position = i;
				}
			}
			intent.putExtra("listPosition", position);
			intent.putExtra("MSG", AppConstant.PlayerMsg.PLAY_MSG);
			intent.putExtra("repeatState", repeatState);//
			intent.putExtra("shuffleState", isShuffle);//
			intent.putExtra("mp3Infos", (Serializable) search_mp3Infos);//别传递错了
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
			String containWords = null ;
			 matrixCursor = new MatrixCursor(colums);
			while (cursor.moveToNext()) {
				String title = cursor.getString(cursor
						.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				String wordReg = "(?i)"+contain;//用(?i)来忽略大小写 
				Matcher matcher = Pattern.compile(wordReg).matcher(title);
				
				if (matcher.find()) {
					containWords= matcher.group();
					searchListViewAdapter.setContainWords(containWords);
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
		@SuppressLint("NewApi")
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if(position<=0){
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
						
						showSearchDialog();
						makeViewGone();
						searchBg.bringToFront();

					}
				});
			} else if (position == 2) {
				dialog = new AlertDialog.Builder(getActivity()).setCancelable(
						false).create();
				dialog.show();
				dialog.setContentView(R.layout.new_playlist_dialog);
				setDialogAttr();

			} else {
				showTitleAnim();
				view.setPressed(true);
				Bundle bundle = new Bundle();
				bundle.putString("list_table_name",
						"_" + listInfos.get(position - 3).getListName());
				PlayListUiListFragment pluFragment = PlayListUiListFragment
						.newInstance(bundle);
				FragmentManager fm = getActivity().getFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.addToBackStack(null);
				ft.setCustomAnimations(R.anim.fragment_slide_in_left,
						R.anim.fragment_silde_out_right,
						R.anim.fragment_slide_right_enter,
						R.anim.fragment_slide_right_exit);
				ft.replace(R.id.content, pluFragment).commit();
			}

		}
		/**
		 * fragment切换动画
		 */
		public void showTitleAnim() {
			Animation alphaAnimation = new AlphaAnimation(1f, 0f);
			alphaAnimation.setDuration(250);
			titleTV.startAnimation(alphaAnimation);
		}
		/**
		 * 设置dialog属性
		 */
		private void setDialogAttr() {
			Window window = dialog.getWindow();
			/** 弹出软键盘 **/
			window.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

			/** 防止Activity挤变形 **/
			getActivity()
					.getWindow()
					.setSoftInputMode(
							WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
									| WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
			/** 设置dialog宽度 **/
			WindowManager.LayoutParams lp = window.getAttributes();
			lp.width = Dip2Px.dip2px(getActivity(), 300);
			window.setAttributes(lp);

			dialogSaveTV = (TextView) dialog
					.findViewById(R.id.textView_new_playlist_dialog_save);
			dialogCancelTV = (TextView) dialog
					.findViewById(R.id.textView_new_playlist_dialog_cancel);
			newPlaylistED = (EditText) dialog
					.findViewById(R.id.editText_new_playList);

			setDialogListener();
		}

		/**
		 * 设置对话框按钮监听
		 * 
		 * @param view2
		 */
		private void setDialogListener() {
			dialogCancelTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					dialog.dismiss();
					// Log.e("", "dismiss");
				}
			});

			dialogSaveTV.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					listName = newPlaylistED.getText().toString();
					if (listName.equals("")) {
						dialog.dismiss();
					} else {
						dbHelper = new DBHelperofList(getActivity());
						try {
							dbHelper.insertNewList("_" + listName);
						} catch (Exception e) {
							Toast.makeText(getActivity(), "列表名已存在:(",
									Toast.LENGTH_SHORT).show();
							e.printStackTrace();
						}
						dialog.dismiss();
						intent = new Intent(getActivity(),
								AddMusicToListActivity.class);
						intent.putExtra("listName", "_" + listName);
						startActivity(intent);

					}
				}
			});
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
				playListViewAdapter.setFlagBusy(true);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				playListViewAdapter.setFlagBusy(false);
				playListViewAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				playListViewAdapter.setFlagBusy(true);
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
