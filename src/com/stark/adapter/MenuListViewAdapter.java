package com.stark.adapter;

import com.stark.music.R;
import com.stark.util.MediaUtil;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MenuListViewAdapter extends BaseAdapter{
	private Cursor cursor;
	private Context context;
	private LayoutInflater listContainer;
	public MenuListViewAdapter(Context context,Cursor cursor){
		this.context = context;
		this.cursor = cursor;
		listContainer = LayoutInflater.from(context);
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return cursor.getCount();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		if(convertView == null){
			viewHolder = new ViewHolder();
			convertView = listContainer.inflate(R.layout.menu_list_item, null);
			viewHolder.musicTitle = (TextView)convertView.findViewById(R.id.textView_song_name_in_menu);
			viewHolder.songDuration  =(TextView)convertView.findViewById(R.id.textView_menu_duration);
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		cursor.moveToPosition(position);
		String musicTitle = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
		long songDuration = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
		viewHolder.musicTitle.setText(musicTitle);
		viewHolder.songDuration.setText(MediaUtil.formatTime(songDuration));
		return convertView;
	}

}
