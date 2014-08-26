package com.stark.adapter;

import java.util.List;
import java.util.Map;

import com.stark.domain.Mp3Info;
import com.stark.domain.PlayListInfo;
import com.stark.music.R;
import com.stark.util.Dip2Px;
import com.stark.util.ImageLoader;
import com.stark.util.ImageLoaderForPlayList;
import com.stark.util.MediaUtil;

import android.content.Context;
import android.media.Image;
import android.media.audiofx.Visualizer.MeasurementPeakRms;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayListViewAdapter extends BaseAdapter {

	private Context context;
	private LayoutInflater listContainer;
	private List<PlayListInfo> listInfos;
	private PlayListInfo listInfo;
	private List<Mp3Info> mp3Infos;
	private Mp3Info mp3Info;

	private boolean mBusy = false;
	private ImageLoaderForPlayList mImageLoader;

	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public boolean getFlagbusy() {
		return mBusy;
	}

	/**
	 * a list view adapter of first fragment
	 * 
	 * @param context
	 * @param listItems
	 *            put content in Map
	 */
	public PlayListViewAdapter(Context context, List<PlayListInfo> listInfos) {
		this.context = context;
		listContainer = LayoutInflater.from(context);
		this.listInfos = listInfos;
		mp3Infos = MediaUtil.getMp3Infos(context);
		mImageLoader = new ImageLoaderForPlayList(context);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return listInfos.size() ;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
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
			convertView = listContainer.inflate(
					R.layout.play_list_item_layout, null);
			viewHolder.albumImage = (ImageView) convertView
					.findViewById(R.id.albumImage_in_playlist);
			viewHolder.listName = (TextView) convertView
					.findViewById(R.id.music_title_in_playlist);

			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
		listInfo = listInfos.get(position );
		viewHolder.listName
				.setText(listInfo.getListName());
		if(listInfo.getAlbumId() == 0 ||listInfo.getSongId() ==0){
			viewHolder.albumImage.setImageResource(R.drawable.song_pic);
		}else{
			viewHolder.albumImage.setImageResource(R.drawable.song_pic);
			mImageLoader.loadImage(listInfo.getAlbumId(), listInfo.getSongId(), this, viewHolder, position);
		}
		

		return convertView;
	}
	
}
