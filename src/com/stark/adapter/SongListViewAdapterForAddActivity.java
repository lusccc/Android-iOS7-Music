package com.stark.adapter;

import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.stark.domain.Mp3Info;
import com.stark.music.R;
import com.stark.music.fragment.addmusic.SongsFragmentInAddActivity;
import com.stark.music.fragment.main.SongsFragment;
import com.stark.util.ConvertToShortTitle;
import com.stark.util.Dip2Px;
import com.stark.util.ImageLoaderForSongListAdd;
import com.stark.util.ImageLoaderForSongsList;
import com.stark.util.SortCursor;

public class SongListViewAdapterForAddActivity extends SimpleCursorAdapter
		implements SectionIndexer {

	private Context context;
	private LayoutInflater listContainer;
	private List<Mp3Info> mp3Infos; // 存放Mp3Info引用的集合
	private Mp3Info mp3Info; // Mp3Info对象引用
	private boolean mBusy = false;
	private ImageLoaderForSongListAdd mImageLoader;
	private boolean isTurnOnNav = true;
	private SectionIndexer mIndexer;
	private WeakReference<ViewHolder> holderReference;
	private SortCursor sc;

	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public boolean getFlagbusy() {
		return mBusy;
	}
	
	public void setTurnOnNav(boolean is){
		isTurnOnNav =is;
	}
	

	/**
	 * a list view adapter of first fragment
	 * 
	 * @param context
	 * @param listItems
	 *            put content in Map
	 */
	public SongListViewAdapterForAddActivity(Context context, int layout,
			Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.isTurnOnNav = isTurnOnNav;
		this.context = context;
		listContainer = LayoutInflater.from(context);
		mImageLoader = new ImageLoaderForSongListAdd(context);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		final View view = listContainer.inflate(
				R.layout.songs_add_list_item_layout, null);
		SortCursor mCursor = (SortCursor) getItem(position);
		sc = mCursor;
		ViewHolder viewHolder;
		/*
		 * if (view != null) { viewHolder = new ViewHolder(); holderReference =
		 * new WeakReference<ViewHolder>(viewHolder);
		 * view.setTag(holderReference.get()); } else { viewHolder =
		 * (ViewHolder) convertView.getTag(); }
		 */
		viewHolder = new ViewHolder();
		viewHolder.albumImage = (ImageView) view.findViewById(R.id.albumImage);
		viewHolder.musicTitle = (TextView) view.findViewById(R.id.music_title);
		viewHolder.musicArtist = (TextView) view
				.findViewById(R.id.music_Artist);
		viewHolder.albumName = (TextView) view.findViewById(R.id.album_name);
		viewHolder.letterTV = (TextView) view
				.findViewById(R.id.textView_letter);
		// view.setTag(viewHolder); // 表示给View添加一个格外的数据，

		// viewHolder = holderReference.get();
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
		long duration = mCursor.getLong(SongsFragment.mDurationIndex);
		viewHolder.albumImage.setImageResource(R.drawable.song_pic);
		mImageLoader.loadImage(albumId, id, this, viewHolder, position);
		viewHolder.musicTitle.setText(ConvertToShortTitle.getSubString(title,
				-1, context));
		viewHolder.musicArtist.setText(ConvertToShortTitle.getSubString(artist,
				1, context)); // 显示艺术家

		viewHolder.albumName.setText(ConvertToShortTitle.getSubString(album, 0,
				context));// 显示专辑名
		if (artist.equals(album))
			viewHolder.albumName.setText(R.string.unknow_album);

		if(SongsFragmentInAddActivity.choosedSongs.contains(position)){
			view.setClickable(false);
			ImageView add_circle = (ImageView) view
					.findViewById(R.id.imageView_add_circle);
			RelativeLayout rl = (RelativeLayout) view
					.findViewById(R.id.RelativeLayout_song_list_add_root);
			rl.setBackgroundDrawable(null);
			add_circle.setImageResource(R.drawable.add_circle_pressed);
		}
		viewHolder.letterTV.setVisibility(View.GONE);
		// convertView.setPadding(Dip2Px.px2dip(context, 20), 0, 0, 0);
		return view;

	}

	/**
	 * 根据ListView的当前位置获取分类的首字母的Char ascii值
	 */
	public int getSectionForPosition(int position) {
		// Log.e("", "getview");
		return sc.sortList.get(position).sortLetters.charAt(0);
	}

	/**
	 * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
	 */
	public int getPositionForSection(int section) {
		for (int i = 0; i < getCount(); i++) {
			String sortStr = sc.sortList.get(i).sortLetters;
			char firstChar = sortStr.toUpperCase().charAt(0);
			if (firstChar == section) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * 提取英文的首字母，非英文字母用#代替。
	 * 
	 * @param str
	 * @return
	 */
	private String getAlpha(String str) {
		String sortStr = str.trim().substring(0, 1).toUpperCase();
		// 正则表达式，判断首字母是否是英文字母
		if (sortStr.matches("[A-Z]")) {
			return sortStr;
		} else {
			return "#";
		}
	}

	@Override
	public Object[] getSections() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * @Override public boolean isItemViewTypePinned(int viewType) { // TODO
	 * Auto-generated method stub return true; }
	 */

}
