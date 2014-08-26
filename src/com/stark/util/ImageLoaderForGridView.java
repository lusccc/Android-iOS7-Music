package com.stark.util;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

import com.stark.adapter.GridViewAdapter;
import com.stark.adapter.ViewHolder;
import com.stark.music.activity.MainActivity;

public class ImageLoaderForGridView {
	private static final String TAG = "ImageLoader";
	private static final int MAX_CAPACITY = 50;
	private static final long DELAY_BEFORE_PURGE = 10 * 1000;
	private Context context;

	private ImageLoadTask imageLoadTask;
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

	public ImageLoaderForGridView(Context mContext) {
		context = mContext;

	}

	private Runnable mClearCache = new Runnable() {
		@Override
		public void run() {
			clear();
		}
	};
	private Handler mPurgeHandler = new Handler();

	public void cancelTask() {
		imageLoadTask.cancel(true);
	}

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
		// Log.e("", ""+position);
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
			GridViewAdapter adapter, ViewHolder holder, int position,
			Context context, long albumId, long songId) {
		resetPurgeTimer();
		Bitmap bitmap = getBitmapFromCache(position);// 从缓存中读取
		if (adapter.getFlagbusy()) {
		}
		if (bitmap == null) {
			imageLoadTask = new ImageLoadTask();
			imageLoadTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
					position, adapter, holder, albumName, artistName, context,
					albumId, songId);

		} else {
			holder.albumImage.setImageBitmap(bitmap);
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
		private GridViewAdapter gvAdapter;
		private ViewHolder holder;
		private Context context;
		private long albumId;
		private long songId;

		@Override
		protected Bitmap doInBackground(Object... params) {
			if (imageLoadTask.isCancelled()) {
				// Log.e("", "canceled");
				return null;
			}
			position = (Integer) params[0];
			gvAdapter = (GridViewAdapter) params[1];
			holder = (ViewHolder) params[2];
			albumName = (String) params[3];
			artistName = (String) params[4];
			context = (Context) params[5];
			albumId = (Long) params[6];
			songId = (Long) params[7];

			Bitmap drawable = null;
			drawable = loadImageFromMediaUtil(albumId, songId);
			// Log.e(""+albumName, ""+artistName);
			if (drawable == null) {
				drawable = loadTextArtWork(albumName, artistName, context);
				;
			}
			// System.out.println(drawable);
			return drawable;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			final Bitmap bitmap;
			if (imageLoadTask.isCancelled()) {
				return;
			}
			if (result == null) {

				addImage2Cache(position, null);
				return;
			}
			if (MainActivity.zAxis > 0) {
				// 想右转
				Matrix m = new Matrix();
				m.setRotate(90, (float) result.getWidth() / 2,
						(float) result.getHeight() / 2);
				bitmap = Bitmap.createBitmap(result, 0, 0, result.getWidth(),
						result.getHeight(), m, true);
			} else {
				// 想右转
				Matrix m = new Matrix();
				m.setRotate(-90, (float) result.getWidth() / 2,
						(float) result.getHeight() / 2);
				bitmap = Bitmap.createBitmap(result, 0, 0, result.getWidth(),
						result.getHeight(), m, true);
			}
			
			final AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
			alphaAnimation.setDuration(1000);
			new Handler().post(new Runnable() {

				@Override
				public void run() {
					holder.albumImage.startAnimation(alphaAnimation);

				}
			});
			alphaAnimation.setAnimationListener(new AnimationListener() {
				
				@Override
				public void onAnimationStart(Animation animation) {
					holder.albumImage.setImageBitmap(bitmap);
				}
				
				@Override
				public void onAnimationRepeat(Animation animation) {
					
				}
				
				@Override
				public void onAnimationEnd(Animation animation) {
					
				}
			});
			addImage2Cache(position, bitmap);// 放入缓存

		}
	}

	public Bitmap loadImageFromMediaUtil(long albumId, long songId) {
		return MediaUtil.getArtwork(context, songId, albumId, true, false);
	}

	public Bitmap loadTextArtWork(String albumName, String artistName,
			Context context) {
		return TextArtWork.getTextArtworkForGridView(albumName, artistName,
				context);
	}

	private void clear() {
		mHardCache.clear();
		mSoftCache.clear();
	}
}
