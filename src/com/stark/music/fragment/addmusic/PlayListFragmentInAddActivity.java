package com.stark.music.fragment.addmusic;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stark.adapter.PlayListViewAdapter;
import com.stark.database.DBHelperofList;
import com.stark.domain.Mp3Info;
import com.stark.domain.PlayListInfo;
import com.stark.music.R;
import com.stark.music.activity.AddMusicToListActivity;
import com.stark.util.Dip2Px;
import com.stark.util.MediaUtil;
import com.stark.view.ElasticListView;
import com.stark.view.SideBar;

public class PlayListFragmentInAddActivity extends Fragment {
	private View view;
	private LinearLayout.LayoutParams layoutParams;
	private ElasticListView elasticListView;
	private PlayListViewAdapter playListViewAdapter;
	private DBHelperofList dbHelper;
	private SQLiteDatabase db;
	private Intent intent;
	private Cursor c;
	private List<Mp3Info> mp3Infos = null;
	private List<PlayListInfo> listInfos;
	private Handler handler;
	private SideBar sideBar;

	private final static String LISTNAMETABLE_NAME = "list_name";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		view = inflater.inflate(R.layout.fragment_view_frame_add_rl, null);
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		//listInfos = getListItemsFromDB();
	}

	public void initView(){
		elasticListView = (ElasticListView) view
				.findViewById(R.id.elasticListView_list);
		playListViewAdapter = new PlayListViewAdapter(getActivity(), listInfos);
		elasticListView.setAdapter(playListViewAdapter);
		elasticListView.setOnItemClickListener(new PLayListOnClickListener());
		sideBar = (SideBar)view.findViewById(R.id.sideBar);
		sideBar.setVisibility(View.GONE);
	}
	
	/***
	 * 可以更新ui的
	 */
	public void onResume() {
		listInfos=getListItemsFromDB();
		mp3Infos = MediaUtil.getMp3Infos(getActivity()); // 获取歌曲对象集合
		initView();
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
			String listName = c.getString(c.getColumnIndex("list_name"));
			p.setListName(listName);
			long totoalDuration = 0;
			Cursor c2 = db.query(listName, null, null, null, null, null,
					"_id asc");
			/*if (c2.moveToFirst()) {
				while (c2.moveToNext()) {
					c2.move(0);
					*//** 列表中第一首歌 **//*
					int first_position = c2.getInt(c2
							.getColumnIndex("position"));
					p.setPosition(first_position);
					for (int j = 0; j < c2.getCount(); j++) {
						int position = c2.getInt(c2.getColumnIndex("position"));
						Mp3Info mp3Info = mp3Infos.get(position);
						totoalDuration = totoalDuration + mp3Info.getDuration();
					}
					//p.setTotalDuration(totoalDuration);
				}

			}*/
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

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// TODO Auto-generated method stub
			if (position == 1) {
				dialog = new AlertDialog.Builder(getActivity()).setCancelable(
						false).create();
				dialog.show();
				dialog.setContentView(R.layout.new_playlist_dialog);
				setDialogAttr();

			}else{
				
			}

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
		 * 设置按钮监听
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
						dbHelper.insertNewList(listName);
						dialog.dismiss();
						intent = new Intent(getActivity(),
								AddMusicToListActivity.class);
						intent.putExtra("listName", listName);
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
				// 获取时间差
				/*
				 * if(endTime - startTime >10000){
				 * songListViewAdapter.setFlagBusy(false); }else{
				 * songListViewAdapter.setFlagBusy(true); }
				 */
				playListViewAdapter.setFlagBusy(true);
				// songListViewAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				playListViewAdapter.setFlagBusy(false);
				playListViewAdapter.notifyDataSetChanged();
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				playListViewAdapter.setFlagBusy(true);
				// songListViewAdapter.notifyDataSetChanged();
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

}
