package com.stark.adapter;

import java.util.List;
import java.util.Map;

import com.stark.domain.Mp3Info;
import com.stark.domain.PlayListInfo;
import com.stark.music.R;
import com.stark.util.Dip2Px;
import com.stark.util.ImageLoader;
import com.stark.util.MediaUtil;

import android.content.Context;
import android.media.Image;
import android.media.audiofx.Visualizer.MeasurementPeakRms;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class PlayListViewAdapterForAddActivity extends BaseAdapter {

	private Context context;
	private LayoutInflater listContainer;
	private List<PlayListInfo> listInfos;
	private PlayListInfo listInfo;
	private List<Mp3Info> mp3Infos;
	private Mp3Info mp3Info;

	private boolean mBusy = false;
	private ImageLoader mImageLoader;

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
	public PlayListViewAdapterForAddActivity(Context context, List<PlayListInfo> listInfos) {
		this.context = context;
		listContainer = LayoutInflater.from(context);
		this.listInfos = listInfos;
		mp3Infos = MediaUtil.getMp3Infos(context);
		mImageLoader = new ImageLoader(mp3Infos, context);
	}

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return listInfos.size() + 1;
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

		if (position == 0) {
			View view = listContainer.inflate(R.layout.search_ll, null);

			view.setPadding(Dip2Px.px2dip(context, 40),
					Dip2Px.px2dip(context, 480), Dip2Px.px2dip(context, 40),
					Dip2Px.px2dip(context, 30));
			return view;
		} else {
			if (convertView == null || position < 10) {
				viewHolder = new ViewHolder();
				convertView = listContainer.inflate(
						R.layout.play_list_item_layout, null);
				viewHolder.albumImage = (ImageView) convertView
						.findViewById(R.id.albumImage_in_playlist);
				viewHolder.listName = (TextView) convertView
						.findViewById(R.id.music_title_in_playlist);

				convertView.setTag(viewHolder);

				/** 数据库存储的是每首歌在MP3Infos中的位置 **/
				int position_in_mp3Infos = 0;
				viewHolder = (ViewHolder) convertView
						.getTag();
				if (!mBusy) {
					listInfo = listInfos.get(position - 1);
					viewHolder.listName
							.setText(listInfo.getListName());
					//position_in_mp3Infos = listInfo.getPosition();
					mImageLoader.loadImage(position_in_mp3Infos, this,
							viewHolder);
				}
			}
		}

		return convertView;
	}

}
