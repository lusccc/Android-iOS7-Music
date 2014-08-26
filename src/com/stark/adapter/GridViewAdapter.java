package com.stark.adapter;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.AlphaAnimation;
import android.view.ViewGroup;
import android.widget.AbsListView.OnScrollListener;
import android.widget.FrameLayout.LayoutParams;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.stark.music.R;
import com.stark.music.activity.GridViewActivity;
import com.stark.music.activity.MainActivity;
import com.stark.music.fragment.main.SongsFragment;
import com.stark.util.ConvertToShortTitle;
import com.stark.util.Dip2Px;
import com.stark.util.ImageLoaderForGridView;
import com.stark.util.SortCursor;
import com.stark.view.MyGridView;

public class GridViewAdapter extends SimpleCursorAdapter {

	private Context context;
	private LayoutInflater mInflater;
	private int disp_rows = 3; // 显示多少行
	private final static int COLUMN_CNT = 45; // 显示多少列，这个要和layout文件里面对应起来
	private SortCursor sc;
	private WeakReference<ViewHolder> holderReference;
	private boolean mBusy = false;
	private ImageLoaderForGridView mImageLoader;

	private MyGridView gridView;

	public void setGridView(MyGridView gridView) {
		this.gridView = gridView;
		gridView.setOnScrollListener(mScrollListener);
	}

	public GridViewAdapter(Context context, int layout, Cursor c,
			String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = context;
		mImageLoader = new ImageLoaderForGridView(context);
		// Log.e("", ""+mImageLoader1+" "+mImageLoader2+" "+mImageLoader3);

	}

	public void setFlagBusy(boolean busy) {
		this.mBusy = busy;
	}

	public boolean getFlagbusy() {
		return mBusy;
	}

	// create a new ImageView for each item referenced by the Adapter
	// ImageView 放在了自定义的格子排版文件中，可以扩展使用，也就是说，格子显示的内容可以自己扩展
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// Log.e("" + position, "getview");
		// Log.e("", ""+mBusy);
		final View view = super.getView(position, convertView, parent);
		if (position == 0) {
			return view;
		}
		int a = MainActivity.getScreenHeight();
		int b = MainActivity.getScreenWidth();
		int screenHeight = Math.max(a, b); // 横屏时高度为竖屏时宽度
		int screenWidth = Math.min(b, a);
		int finalEachGridWidth = screenWidth / 3;
		SortCursor mCursor = (SortCursor) getItem(position);
		sc = mCursor;
		final ViewHolder viewHolder;
		if (view != null) {
			viewHolder = new ViewHolder();
			holderReference = new WeakReference<ViewHolder>(viewHolder);
			view.setTag(holderReference.get());
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.albumImage = (ImageView) view
				.findViewById(R.id.ImageView_gridview);
		view.setTag(viewHolder); // 表示给View添加一个格外的数据，
		viewHolder.albumImage.setLayoutParams(new LinearLayout.LayoutParams(
				finalEachGridWidth, finalEachGridWidth));
		viewHolder.albumImage.setImageResource(R.drawable.song_pic_black);
		final long id = mCursor.getLong(GridViewActivity.mSongIdIndex);
		String title = mCursor.getString(GridViewActivity.mSongTitleIndex);
		String album = mCursor.getString(GridViewActivity.mAlbumNameIndex);
		final String artist = mCursor
				.getString(GridViewActivity.mArtistNameIndex);
		final long albumId = mCursor.getLong(GridViewActivity.mAlbumIdIndex);
		long duration = mCursor.getLong(GridViewActivity.mDurationIndex);
		// if(!mBusy)
		

		mImageLoader.loadImage("", artist, GridViewAdapter.this, viewHolder,
				position, context, albumId, id);

		return view;
	}

	private int firstVisibleItem, totalItemCount;
	/**
	 * 滚动时监听器
	 */
	OnScrollListener mScrollListener = new OnScrollListener() {

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_FLING:
				GridViewAdapter.this.setFlagBusy(true);
				break;
			case OnScrollListener.SCROLL_STATE_IDLE:
				GridViewAdapter.this.setFlagBusy(false);

				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				GridViewAdapter.this.setFlagBusy(true);
				break;
			default:
				break;
			}

		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			// Log.e(""+firstVisibleItem, ""+totalItemCount);
			GridViewAdapter.this.firstVisibleItem = firstVisibleItem;
			GridViewAdapter.this.totalItemCount = totalItemCount;

		}
	};

}
