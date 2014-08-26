package com.stark.adapter;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.stark.music.R;
import com.stark.music.fragment.main.PerformerFragment;
import com.stark.util.ConvertToShortTitle;
import com.stark.util.Dip2Px;
import com.stark.util.ImageLoaderForAlbumList;
import com.stark.util.ImageLoaderForPerformerList;
import com.stark.util.SortCursor;
import com.stark.util.TextArtWork;


public class PerformerListViewAdapter extends SimpleCursorAdapter implements SectionIndexer {
	private WeakReference<ViewHolder> holderReference;
	private Context mContext;
	private LayoutInflater listContainer;
	private boolean mBusy = false;
	private ImageLoaderForPerformerList mImageLoader;
	private boolean isTurnOnNav = true;
	private SectionIndexer mIndexer;
	private SortCursor sc ;
	public PerformerListViewAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		mContext = context;
		mImageLoader = new ImageLoaderForPerformerList(context);
		listContainer = LayoutInflater.from(context);
	}
	public void setTurnOnNav(boolean is){
		isTurnOnNav =is;
	}
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
		final View view = super.getView(position, convertView, parent);

		SortCursor mCursor = (SortCursor) getItem(position);
        sc = mCursor;
        final ViewHolder viewHolder;
        if(view != null){
        	viewHolder = new ViewHolder();
        	holderReference = new WeakReference<ViewHolder>(viewHolder);
        	view.setTag(holderReference.get());
        } else{
        	viewHolder = (ViewHolder)convertView.getTag();
        }
        viewHolder.performerImage = (ImageView) view
				.findViewById(R.id.performerImage_in_performerlist);
		viewHolder.performerDetail = (TextView) view
				.findViewById(R.id.textView_performer_detail);
		viewHolder.musicArtist = (TextView) view
				.findViewById(R.id.performer_name_in_performerlist);
		viewHolder.letterTV = (TextView) view.findViewById(R.id.textView_letter_in_performerlist);
		view.setTag(viewHolder);
        // Album name
       // String albumName = mCursor.getString(PerformerFragment.mAlbumNameIndex);
        // Artist name
        String artistName = mCursor.getString(PerformerFragment.mArtistNameIndex);
        holderReference.get().musicArtist.setText(ConvertToShortTitle.getSubString(artistName, 1, mContext));
        // Album ID
        String songNum = mCursor.getString(PerformerFragment.mSongNumIndex);
        String albumNum = mCursor.getString(PerformerFragment.mAlbumNumIndex);
        holderReference.get().performerImage.setImageResource(R.drawable.song_pic);
        holderReference.get().performerDetail.setText(albumNum+ " 张专辑, "+
				songNum
				+ " 首歌曲");
        mImageLoader.loadImage(/*albumName*/ "", artistName, this, holderReference.get(), position,mContext);
        if(isTurnOnNav){
			// 根据position获取分类的首字母的Char ascii值
			int section = getSectionForPosition(position );
			if (position  == getPositionForSection(section)) {
				
				viewHolder.letterTV.setVisibility(View.VISIBLE);
				viewHolder.letterTV.setText(mCursor.sortList.get(position).sortLetters);
			} else {
				viewHolder.letterTV.setVisibility(View.GONE);
			}

		}else{
			viewHolder.letterTV.setVisibility(View.GONE);
		}
        
        return view;
		
	}
	
	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public boolean getFlagbusy() {
		return mBusy;
	}

	@Override
	public Object[] getSections() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * 根据ListView的当前位置获取分类的首字母的Char ascii值
	 */
	public int getSectionForPosition(int position) {
		return sc.sortList.get(position).sortLetters.charAt(0);
	}

	/**
	 * 根据分类的首字母的Char ascii值获取其第一次出现该首字母的位置
	 */
	public int getPositionForSection(int section) {
		for (int i = 0; i < getCount() ; i++) {
			String sortStr =  sc.sortList.get(i).sortLetters;
			char firstChar = sortStr.toUpperCase().charAt(0);
			if (firstChar == section) {
				return i;
			}
		}

		return -1;
	}
}
