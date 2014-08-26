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
import com.stark.adapter.PlayListViewAdapter;
import com.stark.adapter.PlayListViewAdapterForAddActivity;
import com.stark.adapter.SongListViewAdapter;
import com.stark.adapter.SongListViewAdapterForAddActivity;
import com.stark.adapter.ViewHolder;
import com.stark.domain.Mp3Info;

public class ImageLoader {
	private static final String TAG = "ImageLoader";
	private static final int MAX_CAPACITY = 30;
	private static final long DELAY_BEFORE_PURGE = 10 * 1000;
	private List<Mp3Info> mp3Infos;
	private Mp3Info mp3Info;
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

	public ImageLoader(List<Mp3Info> mp3Infos, Context context) {
		// TODO Auto-generated constructor stub
		this.mp3Infos = mp3Infos;
		this.context = context;
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
	/***
	 * PlayListViewAdapter的 loadImage()
	 * @param position
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(int position, PlayListViewAdapter adapter, ViewHolder holder){
		while (!adapter.getFlagbusy()) {
			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			if (bitmap == null) {
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				imageLoadTask.execute(position, adapter, holder);
			} else {
				if (!adapter.getFlagbusy())
					holder.albumImage.setImageBitmap(bitmap);
			}
			return;
		}
	}
	/***
	 * SongListViewAdapter的 loadImage()
	 * @param position
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(int position, SongListViewAdapter adapter, ViewHolder holder){
		while (!adapter.getFlagbusy()) {
			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			if (bitmap == null) {
				/*Log.e("tag", "");
				System.out.println("tag");*/
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				imageLoadTask.execute(position, adapter, holder);
			} else {
				if (! adapter.getFlagbusy())
					holder.albumImage.setImageBitmap(bitmap);
			}
			return;
		}
	}
	/***
	 * SongListViewAdapter的 loadImage()
	 * @param position
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(int position, SongListViewAdapterForAddActivity adapter, ViewHolder holder){
		while (!adapter.getFlagbusy()) {
			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			
			if (bitmap == null) {
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				//imageLoadTask.execute(position, adapter, holder);
				
			} else {
				if (! adapter.getFlagbusy())
					
					holder.albumImage.setImageBitmap(bitmap);
			}
			return;
		}
	}
	/***
	 * PlayListViewAdapterForAddActivity的 loadImage()
	 * @param position
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(int position, PlayListViewAdapterForAddActivity adapter, ViewHolder holder){
		while (!adapter.getFlagbusy()) {
			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			if (bitmap == null) {
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				imageLoadTask.execute(position, adapter, holder);
			} else {
				if (! adapter.getFlagbusy())
					holder.albumImage.setImageBitmap(bitmap);
			}
			return;
		}
	}
	/***
	 * AlbumListViewAdapter的 loadImage()
	 * @param position
	 * @param adapter
	 * @param holder
	 */
	public void loadImage(int position, AlbumListViewAdapter adapter, ViewHolder holder){
		while (!adapter.getFlagbusy()) {
			resetPurgeTimer();
			Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
			if (bitmap == null) {
				ImageLoadTask imageLoadTask = new ImageLoadTask();
				imageLoadTask.execute(position, adapter, holder);
			} else {
				if (! adapter.getFlagbusy())
					holder.albumImage.setImageBitmap(bitmap);
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
		int position;
		private SongListViewAdapter sAdapter;
		private SongListViewAdapterForAddActivity sAdapterAdd;
		private PlayListViewAdapter pAdapter;
		private PlayListViewAdapterForAddActivity pAdapterAdd;
		private AlbumListViewAdapter abmAdapter;
		

		@Override
		protected Bitmap doInBackground(Object... params) {
			position = (Integer) params[0];
			Object o = (BaseAdapter) params[1];
			/** 通过反射来判断adapter的类型 **/
			if (o.getClass().equals(SongListViewAdapter.class)) {
				sAdapter = (SongListViewAdapter) o;
			} else if (o.getClass().equals(
					SongListViewAdapterForAddActivity.class)) {
				sAdapterAdd = (SongListViewAdapterForAddActivity) o;
			} else if(o.getClass().equals(PlayListViewAdapter.class)){
				pAdapter = (PlayListViewAdapter) o;
			} else if(o.getClass().equals(PlayListViewAdapterForAddActivity.class)){
				pAdapterAdd = (PlayListViewAdapterForAddActivity) o ;
			} else {
				abmAdapter  =(AlbumListViewAdapter)o;
			}

			Bitmap drawable = loadImageFromMediaUtil(position);// 获取图片
			return drawable;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (result == null) {
				return;
			}
			if (sAdapter != null) {
				sAdapter.notifyDataSetChanged();
				addImage2Cache(position, result);// 放入缓存
			} else if (sAdapterAdd != null) {
				sAdapterAdd.notifyDataSetChanged();
				addImage2Cache(position, result);// 放入缓存
			} else if(pAdapter != null){
				pAdapter.notifyDataSetChanged();
				addImage2Cache(position, result);// 放入缓存
			} else if(pAdapterAdd!=null){
				pAdapterAdd.notifyDataSetChanged();
				addImage2Cache(position, result);// 放入缓存
			} else{
				abmAdapter.notifyDataSetChanged();
				addImage2Cache(position, result);// 放入缓存
			}

		}
	}

	public Bitmap loadImageFromMediaUtil(int position) {

		Bitmap bitmap;
		while (true) {// 循环到Bmps这个集合准备好之后
			while (position < MediaUtil.getBmps().size()) {
				bitmap = MediaUtil.getBmps().get(position);
				if (bitmap == null) {
					// Log.e("bmp","null");
				}
				return bitmap;
			}
		}

	}

	private void clear() {
		mHardCache.clear();
		mSoftCache.clear();
	}
}
