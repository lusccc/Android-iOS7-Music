package com.stark.util;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.R.integer;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.util.Log;

import com.stark.domain.AlbumListInfo;
import com.stark.domain.Mp3Info;

public class MediaUtil {

	static List<Bitmap> bmps;
	static List<Mp3Info> mp3Infos;
	static List<AlbumListInfo> albumListInfos;
	static Context mContext;

	/**
	 * 汉字转换成拼音的类
	 */
	private static CharacterParser characterParser;

	// 获取专辑封面的Uri
	private static final Uri albumArtUri = Uri
			.parse("content://media/external/audio/albumart");

	/**
	 * 用于从数据库中查询歌曲的信息，保存在List当中
	 * 
	 * @return
	 */
	public static List<Mp3Info> getMp3Infos(Context context) {
		mContext = context;
		Cursor cursor = context.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		bmps = new ArrayList<Bitmap>();
		mp3Infos = new ArrayList<Mp3Info>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToNext();
			Mp3Info mp3Info = new Mp3Info();
			long id = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID)); // 音乐id
			String title = cursor.getString((cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE))); // 音乐标题
			String artist = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST)); // 艺术家
			String album = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM)); // 专辑
			String displayName = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
			long albumId = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			long duration = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION)); // 时长
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.SIZE)); // 文件大小
			String url = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA)); // 文件路径
			int isMusic = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)); // 是否为音乐

			// 汉字转换成拼音
			characterParser = CharacterParser.getInstance();
			String pinyin = characterParser.getSelling(title);
			String sortString = pinyin.substring(0, 1).toUpperCase(); // 转换大写字母

			// 正则表达式，判断首字母是否是英文字母
			if (sortString.matches("[A-Z]")) {
				mp3Info.setSortLetters(sortString.toUpperCase());
			} else {
				mp3Info.setSortLetters("#");
			}
			if (isMusic != 0) { // 只把音乐添加到集合当中
				mp3Info.setId(id);
				mp3Info.setTitle(title);
				mp3Info.setArtist(artist);
				mp3Info.setAlbum(album);
				mp3Info.setDisplayName(displayName);
				mp3Info.setAlbumId(albumId);
				mp3Info.setDuration(duration);
				mp3Info.setSize(size);
				mp3Info.setUrl(url);
				mp3Infos.add(mp3Info);
			}

		}
		/**
		 * 中英混合排序
		 * 
		 * @author Administrator
		 * 
		 */
		class ComparatorMp3 implements Comparator {

			Collator collator = Collator.getInstance(java.util.Locale.CHINA); // 调入这个是解决中文排序问题

			@Override
			public int compare(Object object1, Object object2) {
				Mp3Info mp3Info1 = (Mp3Info) object1;
				Mp3Info mp3Info2 = (Mp3Info) object2;

				if (mp3Info2.getSortLetters().equals("#")) {
					return -1;
				} else if (mp3Info1.getSortLetters().equals("#")) {
					return 1;
				} else {
					return mp3Info1.getSortLetters().compareTo(
							mp3Info2.getSortLetters());
				}
			}
		}
		ComparatorMp3 comparator = new ComparatorMp3();
		Collections.sort(mp3Infos, comparator);

		// new Thread(new BackgroundBmp(mp3Infos, context)).start();
		cursor.close();
		return mp3Infos;

	}

	/**
	 * 获取用来显示专辑fragment的集合
	 * @param context
	 * @return
	 */
	public static List<AlbumListInfo> getAlbumListInfos(Context context) {
		mContext = context;
		Cursor cursor = context.getContentResolver().query(
				MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
		albumListInfos = new ArrayList<AlbumListInfo>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToNext();
			AlbumListInfo albumListInfo = new AlbumListInfo();
			long songId = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID)); // 音乐id
			String artist = cursor.getString(cursor
					.getColumnIndex(AlbumColumns.ARTIST)); // 艺术家
			String album = cursor.getString(cursor
					.getColumnIndex(AlbumColumns.ALBUM)); // 专辑
			long albumId = cursor
					.getInt(cursor.getColumnIndex(BaseColumns._ID));
			String numSongs = cursor.getString(cursor
					.getColumnIndex(AlbumColumns.NUMBER_OF_SONGS));

			Cursor c = context.getContentResolver().query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					new String[]{MediaStore.Audio.Media.DURATION}, "duration=?", new String[]{album},
					MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
			long totalDuration = 0;
			/*for(int j = 0 ;j<c.getCount();j++){
				c.moveToNext();
				String durationString = c.getString(c.getColumnIndex(MediaStore.Audio.Media.DURATION));
				totalDuration = totalDuration + Long.parseLong(durationString);
			}*/
			c.close();
			// 汉字转换成拼音
			characterParser = CharacterParser.getInstance();
			String pinyin = characterParser.getSelling(album);
			String sortString = pinyin.substring(0, 1).toUpperCase(); // 转换大写字母

			// 正则表达式，判断首字母是否是英文字母
			if (sortString.matches("[A-Z]")) {
				albumListInfo.setSortLetters(sortString.toUpperCase());
			} else {
				albumListInfo.setSortLetters("#");
			}
			
			
			albumListInfo.setId(songId);
			albumListInfo.setAlbumArtist(artist);
			albumListInfo.setAlbumName(album);
			albumListInfo.setAmount(numSongs);
			albumListInfo.setAlbumId(albumId);
			albumListInfo.setTotalDuration(totalDuration);
			albumListInfos.add(albumListInfo);
		}
		
		cursor.close();
		
		/**
		 * 中英混合排序
		 * 
		 * @author Administrator
		 * 
		 */
		 class ComparatorAlbum implements Comparator {

			Collator collator = Collator.getInstance(java.util.Locale.CHINA); // 调入这个是解决中文排序问题

			@Override
			public int compare(Object object1, Object object2) {
				AlbumListInfo albumListInfo1 = (AlbumListInfo) object1;
				AlbumListInfo albumListInfo2 = (AlbumListInfo) object2;

				if (albumListInfo2.getSortLetters().equals("#")) {
					return -1;
				} else if (albumListInfo1.getSortLetters().equals("#")) {
					return 1;
				} else {
					return albumListInfo1.getSortLetters().compareTo(
							albumListInfo2.getSortLetters());
				}
			}
		}
		 ComparatorAlbum comparator = new ComparatorAlbum();
		Collections.sort(albumListInfos, comparator);
		Log.e("", ""+albumListInfos.size());
		return albumListInfos;

	}

	

	public static void prepareBmps() {
		for (int i = 0; i < mp3Infos.size(); i++) {
			Mp3Info mp3Info = mp3Infos.get(i);
			Bitmap bitmap = MediaUtil.getArtwork(mContext, mp3Info.getId(),
					mp3Info.getAlbumId(), true, false);
			// Log.e("bmp", ""+bitmap);
			bmps.add(bitmap);
		}
	}

	/*
	 * private static class BackgroundBmp implements Runnable { List<Mp3Info>
	 * mp3Infos; Context context;
	 * 
	 * public BackgroundBmp(List<Mp3Info> mp3Infos, Context context) { // TODO
	 * Auto-generated constructor stub this.mp3Infos = mp3Infos; this.context =
	 * context; }
	 * 
	 * @Override public void run() { // TODO Auto-generated method stub
	 * 
	 * } }
	 */

	public static List<Bitmap> getBmps() {
		return bmps;
	}

	/**
	 * 往List集合中添加Map对象数据，每一个Map对象存放一首音乐的所有属性
	 * 
	 * @param mp3Infos
	 * @return
	 */
	public static List<HashMap<String, String>> getMusicMaps(
			List<Mp3Info> mp3Infos) {
		List<HashMap<String, String>> mp3list = new ArrayList<HashMap<String, String>>();
		for (Iterator iterator = mp3Infos.iterator(); iterator.hasNext();) {
			Mp3Info mp3Info = (Mp3Info) iterator.next();
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("title", mp3Info.getTitle());
			map.put("Artist", mp3Info.getArtist());
			map.put("album", mp3Info.getAlbum());
			map.put("displayName", mp3Info.getDisplayName());
			map.put("albumId", String.valueOf(mp3Info.getAlbumId()));
			map.put("duration", formatTime(mp3Info.getDuration()));
			map.put("size", String.valueOf(mp3Info.getSize()));
			map.put("url", mp3Info.getUrl());
			mp3list.add(map);
		}
		return mp3list;
	}

	/**
	 * 格式化时间，将毫秒转换为分:秒格式
	 * 
	 * @param time
	 * @return
	 */
	public static String formatTime(long time) {
		String min = time / (1000 * 60) + "";
		String sec = time % (1000 * 60) + "";
		if (min.length() < 2) {
			min = "0" + time / (1000 * 60) + "";
		} else {
			min = time / (1000 * 60) + "";
		}
		if (sec.length() == 4) {
			sec = "0" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 3) {
			sec = "00" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 2) {
			sec = "000" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 1) {
			sec = "0000" + (time % (1000 * 60)) + "";
		}
		return min + ":" + sec.trim().substring(0, 2);
	}

	/**
	 * 获取默认专辑图片
	 * 
	 * @param context
	 * @return
	 */
	public static Bitmap getDefaultArtwork(Context context, boolean small) {
		/*
		 * BitmapFactory.Options opts = new BitmapFactory.Options();
		 * opts.inPreferredConfig = Bitmap.Config.RGB_565; if (small) { // 返回小图片
		 * return BitmapFactory.decodeStream(context.getResources()
		 * .openRawResource(R.drawable.song_pic), null, opts); } return
		 * BitmapFactory.decodeStream(context.getResources()
		 * .openRawResource(R.drawable.song_pic), null, opts);
		 */
		return null;
	}

	/**
	 * 从文件当中获取专辑封面位图
	 * 
	 * @param context
	 * @param songid
	 * @param albumid
	 * @return
	 */
	private static Bitmap getArtworkFromFile(Context context, long songid,
			long albumid) {
		Bitmap bm = null;
		if (albumid < 0 && songid < 0) {
			throw new IllegalArgumentException(
					"Must specify an album or a song id");
		}
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			FileDescriptor fd = null;
			if (albumid < 0) {
				Uri uri = Uri.parse("content://media/external/audio/media/"
						+ songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			} else {
				Uri uri = ContentUris.withAppendedId(albumArtUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver()
						.openFileDescriptor(uri, "r");
				if (pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			}
			options.inSampleSize = 1;
			// 只进行大小判断
			options.inJustDecodeBounds = true;
			// 调用此方法得到options得到图片大小
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			// 我们的目标是在800pixel的画面上显示
			// 所以需要调用computeSampleSize得到图片缩放的比例
			options.inSampleSize = 100;
			// 我们得到了缩放的比例，现在开始正式读入Bitmap数据
			options.inJustDecodeBounds = false;
			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			// 根据options参数，减少所需要的内存
			bm = BitmapFactory.decodeFileDescriptor(fd);
			// bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bm;
	}

	/**
	 * 获取专辑封面位图对象
	 * 
	 * @param context
	 * @param song_id
	 * @param album_id
	 * @param allowdefalut
	 * @return
	 */
	public static Bitmap getArtwork(Context context, long song_id,
			long album_id, boolean allowdefalut, boolean small) {
		if (album_id < 0) {
			if (song_id < 0) {
				Bitmap bm = getArtworkFromFile(context, song_id, -1);
				if (bm != null) {
					return bm;
				}
			}
			if (allowdefalut) {
				return getDefaultArtwork(context, small);
			}
			return null;
		}
		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(albumArtUri, album_id);
		if (uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				// 先制定原始大小
				options.inSampleSize = 1;
				// 只进行大小判断
				options.inJustDecodeBounds = true;
				// 调用此方法得到options得到图片的大小
				BitmapFactory.decodeStream(in, null, options);
				/** 我们的目标是在你N pixel的画面上显示。 所以需要调用computeSampleSize得到图片缩放的比例 **/
				/** 这里的target为800是根据默认专辑图片大小决定的，800只是测试数字但是试验后发现完美的结合 **/
				if (small) {
					options.inSampleSize = computeSampleSize(options, 40);
				} else {
					options.inSampleSize = computeSampleSize(options, 600);
				}
				// 我们得到了缩放比例，现在开始正式读入Bitmap数据
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				in = res.openInputStream(uri);
				return BitmapFactory.decodeStream(in, null, options);
			} catch (FileNotFoundException e) {
				Bitmap bm = getArtworkFromFile(context, song_id, album_id);
				if (bm != null) {
					if (bm.getConfig() == null) {
						bm = bm.copy(Bitmap.Config.RGB_565, false);
						if (bm == null && allowdefalut) {
							return getDefaultArtwork(context, small);
						}
					}
				} else if (allowdefalut) {
					bm = getDefaultArtwork(context, small);
				}
				return bm;
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * 对图片进行合适的缩放
	 * 
	 * @param options
	 * @param target
	 * @return
	 */
	public static int computeSampleSize(Options options, int target) {
		int w = options.outWidth;
		int h = options.outHeight;
		int candidateW = w / target;
		int candidateH = h / target;
		int candidate = Math.max(candidateW, candidateH);
		if (candidate == 0) {
			return 1;
		}
		if (candidate > 1) {
			if ((w > target) && (w / candidate) < target) {
				candidate -= 1;
			}
		}
		if (candidate > 1) {
			if ((h > target) && (h / candidate) < target) {
				candidate -= 1;
			}
		}
		return candidate;
	}
	
	
}
