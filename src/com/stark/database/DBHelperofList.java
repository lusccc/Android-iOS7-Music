package com.stark.database;

import com.stark.domain.Mp3Info;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelperofList extends SQLiteOpenHelper {

	private final static int DB_VERSION = 1;
	private final static String DB_NAME = "play_lists.db";
	/** 播放列表名的表 **/
	private final static String LISTNAMETABLE_NAME = "list_name";
	/** 播放列表中歌曲信息的表 **/
	private String MUSIC_IN_LIST_INFO_NAME = "list_music_infos";
	private String ID = "_id";

	private SQLiteDatabase db;

	public DBHelperofList(Context context) {

		super(context, DB_NAME, null, DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + LISTNAMETABLE_NAME
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, list_name text)");

		// Log.e("db", "creat");
	}

	/**
	 * 插入新播放列表
	 * 
	 * @param list_name_by_user
	 */
	public void insertNewList(String list_name_by_user) {
		MUSIC_IN_LIST_INFO_NAME = list_name_by_user;
		db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("list_name", list_name_by_user);
		db.insert(LISTNAMETABLE_NAME, null, cv);

		db.execSQL("CREATE TABLE "
				+ MUSIC_IN_LIST_INFO_NAME
				+ " (_id INTEGER PRIMARY KEY AUTOINCREMENT, id integer, title text, artist text, album text, album_id integer, duration integer, url text)");
		db.close();

	}

	public void insertMusicInfo( String TABLE_NAME,long id,String title,String artist,String album,long album_id,long duration,String url) {
		db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put("id", id);
		cv.put("title", title);
		cv.put("artist", artist);
		cv.put("album", album);
		cv.put("album_id",album_id);
		cv.put("duration", duration);
		cv.put("url", url);
		db.insert(TABLE_NAME, null, cv);
		db.close();

	}
	
	public void deletePlayList(String TABLE_NAME){
		db = this.getWritableDatabase();
		db.delete(TABLE_NAME, null, null);
		db.delete(LISTNAMETABLE_NAME, "list_name=?", new String[]{TABLE_NAME});
		db.close();
	}
	public void cleanPlayList(String TABLE_NAME){
		db = this.getWritableDatabase();
		db.delete(TABLE_NAME, null, null);
		db.close();
	}
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

		String sql = "DROP TABLE IF EXISTS " + LISTNAMETABLE_NAME;
		db.execSQL(sql);
		sql = "DROP TABLE IF EXISTS " + MUSIC_IN_LIST_INFO_NAME;
		db.execSQL(sql);
		onCreate(db);
	}

}
