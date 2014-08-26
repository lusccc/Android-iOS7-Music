package com.stark.util;



import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.database.Cursor;
import android.database.CursorWrapper;
/**
 * Cursor再次排序
 * @author Administrator
 *
 */
public class SortCursor extends CursorWrapper{
	private Cursor mCursor;
	/**
	 * 汉字转换成拼音的类
	 */
	private static CharacterParser characterParser;
	public ArrayList<SortEntry> sortList = new ArrayList<SortEntry>();
	int mPos = 0;
	
	public class SortEntry {
		public String key;
		public int order;
		public String sortLetters;  //显示数据拼音的首字母
		public String getSortLetters() {
			return sortLetters;
		}
		public void setSortLetters(String sortLetters) {
			this.sortLetters = sortLetters;
		}
	}
	
	//直接初始化,加快比较速度,在G3上从3s->0.2s
	@SuppressWarnings("rawtypes")
	private Comparator cmp = Collator.getInstance(java.util.Locale.CHINA);
	
	@SuppressWarnings("unchecked")	
	public Comparator<SortEntry> comparator = new Comparator<SortEntry>(){		
		@Override
		public int compare(SortEntry entry1, SortEntry entry2) {			
			if (entry2.getSortLetters().equals("#")) {
				return -1;
			} else if (entry1.getSortLetters().equals("#")) {
				return 1;
			} else {
				return entry1.getSortLetters().compareTo(
						entry2.getSortLetters());
			}       
		}	
	};

	public SortCursor(Cursor cursor,String columnName) {		
		super(cursor);
		// TODO Auto-generated constructor stub
		mCursor = cursor;
		if(mCursor != null && mCursor.getCount() > 0) {
			int i = 0;
			int column = cursor.getColumnIndexOrThrow(columnName);
			for(mCursor.moveToFirst();!mCursor.isAfterLast();mCursor.moveToNext(),i++){
				SortEntry sortKey = new SortEntry();
				sortKey.key = cursor.getString(column);
				sortKey.order = i;
				sortList.add(sortKey);
				// 汉字转换成拼音
				characterParser = CharacterParser.getInstance();
				String pinyin = characterParser.getSelling(sortKey.key);
				String sortString = pinyin.substring(0, 1).toUpperCase(); // 转换大写字母

				// 正则表达式，判断首字母是否是英文字母
				if (sortString.matches("[A-Z]")) {
					sortKey.setSortLetters(sortString.toUpperCase());
				} else {
					sortKey.setSortLetters("#");
				}
			}
		}
		//排序
		Collections.sort(sortList,comparator);
	}
	
    public boolean moveToPosition(int position)
    {
    	if(position >= 0 && position < sortList.size()){
    		mPos = position;
    		int order = sortList.get(position).order;
    		return mCursor.moveToPosition(order);
    	}
    	if(position < 0){
    		mPos = -1;
    	}
    	if(position >= sortList.size()){
    		mPos = sortList.size();
    	}
    	return mCursor.moveToPosition(position);        
    }
    
    public boolean moveToFirst() {    	
        return moveToPosition(0);
    }
    
    public boolean moveToLast(){
    	return moveToPosition(getCount() - 1);
    }
    
    public boolean moveToNext() {    	    	
        return moveToPosition(mPos+1);
    }
    
    public boolean moveToPrevious() {    	
        return moveToPosition(mPos-1);
    }
	
    public boolean move(int offset) {    	
        return moveToPosition(mPos + offset);
    }
    
    public int getPosition() {    	
    	return mPos;
    }
}
