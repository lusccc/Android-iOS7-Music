package com.stark.adapter;

import java.lang.ref.WeakReference;
import java.util.List;

import android.R.integer;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.fragment.main.SongsFragment;
import com.stark.util.ConvertToShortTitle;
import com.stark.util.ImageLoaderForSongSearchList;

public class SearchListViewSongAdapter extends SimpleCursorAdapter {

	private Context context;
	private LayoutInflater listContainer;
	private List<Mp3Info> mp3Infos; // 存放Mp3Info引用的集合
	private Mp3Info mp3Info; // Mp3Info对象引用
	private boolean mBusy = false;
	private ImageLoaderForSongSearchList mImageLoader;
	private boolean isTurnOnNav = true;
	private SectionIndexer mIndexer;
	private WeakReference<ViewHolder> holderReference;
	private Cursor sc;
	private String containWords;
	private Spanned spanned;

	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public boolean getFlagbusy() {
		return mBusy;
	}

	public void setContainWords(String containWords) {
		this.containWords = containWords;
	}

	/**
	 * a list view adapter of first fragment
	 * 
	 * @param context
	 * @param listItems
	 *            put content in Map
	 */
	public SearchListViewSongAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = context;
		listContainer = LayoutInflater.from(context);
		mImageLoader = new ImageLoaderForSongSearchList(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);
		Cursor mCursor = (Cursor) getItem(position);
		sc = mCursor;
		ViewHolder viewHolder;
		if (view != null) {
			viewHolder = new ViewHolder();
			holderReference = new WeakReference<ViewHolder>(viewHolder);
			view.setTag(holderReference.get());
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.albumImage = (ImageView) view.findViewById(R.id.albumImage);
		viewHolder.musicTitle = (TextView) view.findViewById(R.id.music_title);
		viewHolder.musicArtist = (TextView) view
				.findViewById(R.id.music_Artist);
		viewHolder.albumName = (TextView) view.findViewById(R.id.album_name);
		viewHolder.letterTV = (TextView) view
				.findViewById(R.id.textView_letter);
		view.setTag(viewHolder); // 表示给View添加一个格外的数据，

		viewHolder = holderReference.get();
		long id = mCursor.getLong(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
		String title = mCursor.getString(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
		String album = mCursor.getString(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
		String artist = mCursor.getString(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
		long albumId = mCursor.getLong(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
		long duration = mCursor.getLong(mCursor
				.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

		viewHolder.albumImage.setImageResource(R.drawable.song_pic);
		mImageLoader.loadImage(albumId, id, this, viewHolder, position);
		String html = title.replaceAll("(?u)"+containWords,"<b>"+containWords+"</b><br />");
		spanned = Html.fromHtml(html);
		viewHolder.musicTitle.setText(spanned);
		viewHolder.musicArtist.setText(ConvertToShortTitle.getSubString(artist,
				1, context)); // 显示艺术家

		viewHolder.albumName.setText(ConvertToShortTitle.getSubString(album, 0,
				context));// 显示专辑名
		viewHolder.letterTV.setVisibility(View.GONE);
		if (artist.equals(album))
			viewHolder.albumName.setText(R.string.unknow_album);

		return view;

	}

}
