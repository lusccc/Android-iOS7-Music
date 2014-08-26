package com.stark.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelperofPlayState extends SQLiteOpenHelper {

	private final static int DB_VERSION = 1;
	private final static String DB_NAME = "play_state.db";
	private final static String TABLE_NAME = "play_state";
	private String ID = "_id";
	
	public DBHelperofPlayState(Context context){
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE "
				+ TABLE_NAME
				+ "  (repeat_state integer, is_shuffle integer, is_none_shuffle)");
		db.execSQL(
				"insert into "
						+ TABLE_NAME
						+ "(repeat_state ,is_shuffle ,is_none_shuffle) values(?,?,?)",
				new Object[] { 2, 0 ,1});
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
		db.execSQL(sql);
		onCreate(db);
	}
	
	public void changeData(String item, String value) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put(item, value);
		db.update(TABLE_NAME, cv, null, null);
	}
	
}
