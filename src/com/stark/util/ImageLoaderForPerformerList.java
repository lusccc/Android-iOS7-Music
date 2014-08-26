package com.stark.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.BaseAdapter;

import com.stark.adapter.AlbumListViewAdapter;
import com.stark.adapter.PerformerListViewAdapter;
import com.stark.adapter.ViewHolder;
import com.stark.domain.Mp3Info;

public class ImageLoaderForPerformerList {
	private static final String TAG = "ImageLoader";
	private static final int MAX_CAPACITY = 50;
	private static final long DELAY_BEFORE_PURGE = 10 * 1000;
	private Context context;

	private ConcurrentHashMap<Integer, SoftReference<Bitmap>> mSoftCache = new ConcurrentHashMap<Integer, SoftReference<Bitmap>>(
			MAX_CAPACITY / 2);

	private HashMap<Integer, Bitmap> mHardCache = new LinkedHashMap<Integer, Bitmap>(
			MAX_CAPACITY / 2, 0.75f, true) {

		private static final long serialVersionUID = 1L;

		protected boolean removeEldestEntry(Entry<Integer, Bitmap> eldest) {
			if (size() > MAX_CAPACITY) {
				mSoftCache.put(eldest.getKey(), new SoftReference<Bitmap>(
						eldest.getValue()));
				return true;
			}
			return false;
		};
	};

	public ImageLoaderForPerformerList(Context mContext) {
		context = mContext;
	}

	private Runnable mClearCache = new Runnable() {
		@Override
		public void run() {
			clear();
		}
	};
	private Handler mPurgeHandler = new Handler();

	private void resetPurgeTimer() {
		mPurgeHandler.removeCallbacks(mClearCache);
		mPurgeHandler.postDelayed(mClearCache, DELAY_BEFORE_PURGE);
	}

	/**
	 * 返回缓存，如果没有则返回null
	 * 
	 * @return
	 */
	public Bitmap getBitmapFromCache(int position) {
		//Log.e("", ""+position);
		Bitmap bitmap = null;
		synchronized (mHardCache) {
			bitmap = mHardCache.get(position);
			if (bitmap != null) {
				mHardCache.remove(position);
				mHardCache.put(position, bitmap);
				return bitmap;
			}
		}

		SoftReference<Bitmap> softReference = mSoftCache.get(position);
		if (softReference != null) {
			bitmap = softReference.get();
			if (bitmap == null) {// 已经被gc回收了
				// Log.e("SoftCacheBMP", "null");
				mSoftCache.remove(position);
			}
		}
		return bitmap;
	}

	public void loadImage(String albumName, String artistName,
			PerformerListViewAdapter adapter, ViewHolder holder, int position,Context context) {
		while (!adapter.getFlagbusy()) {

			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			if (bitmap == null) {
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				imageLoadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						position, adapter, holder, albumName, artistName,context);
				// System.out.println("tag");
			} else {
				// System.out.println("tag");
				if (!adapter.getFlagbusy())
					holder.performerImage.setImageBitmap(bitmap);
			}
			return;
		}
	}

	public void addImage2Cache(int position, Bitmap value) {
		if (value == null || position < 0) {
			return;
		}
		synchronized (mHardCache) {
			mHardCache.put(position, value);
		}
	}

	class ImageLoadTask extends AsyncTask<Object, Void, Bitmap> {
		private int position;
		private String albumName;
		private String artistName;
		private PerformerListViewAdapter pfmAdapter;
		private ViewHolder holder;
		private Context context;

		@Override
		protected Bitmap doInBackground(Object... params) {
			/*
			 * System.out.println("doinbackground"); Log.e("",
			 * "doinbackground");
			 */
			position = (Integer) params[0];
			pfmAdapter = (PerformerListViewAdapter) params[1];
			holder = (ViewHolder) params[2];
			albumName = (String) params[3];
			artistName = (String) params[4];
			context = (Context) params[5];
			

			Bitmap drawable = loadTextArtWork(albumName, artistName,context);// 获取图片
			//System.out.println(drawable);
			return drawable;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			/*
			 * System.out.println("onPostdrawable"); Toast.makeText(context,
			 * "tag", Toast.LENGTH_SHORT).show();
			 */
			if (result == null) {
				
				//holder.performerImage.setImageBitmap(null);
				addImage2Cache(position, null);
				return;
			}
			//holder.performerImage.setImageBitmap(result);
			if (!pfmAdapter.getFlagbusy())
				pfmAdapter.notifyDataSetChanged();
			addImage2Cache(position, result);// 放入缓存

		}
	}

	public Bitmap loadTextArtWork(String albumName, String artistName,Context context) {
		 //Log.e("load", "image");
		// System.out.println("loadimage");
		return TextArtWork.getTextArtwork(albumName, artistName, context);
	}

	private void clear() {
		mHardCache.clear();
		mSoftCache.clear();
	}
}
