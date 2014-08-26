package com.stark.util;

import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.graphics.Paint;

import com.stark.music.R;
/**
 * 精简字符串类
 * @author Administrator
 *
 */
public class ConvertToShortTitle {
	/**
	 * 获取精简的String
	 * @param string
	 * @param flag 判断标题类型，1：艺术家，0：专辑名:-1:标题
	 * @return
	 */
	public static String getSubString(String string , int flag , Context context){
		if(string.equals("<unknown>")){
			if(flag == 1){
				return context.getResources().getString(R.string.unknow_performer);
			}else{
				return context.getResources().getString(R.string.unknow_album);
			}
		}
		
		if(flag == 1){
			if( getStringWidth(string)>=250){
				int n = (int) (0.75 * string.length());
				string = string.substring(0, n);
				return string + "...";
			}else{
				return string;
			}
		}else{
			return string;
		}
		
	}
	private static  int getStringWidth(String string){
		Paint paint = new Paint();
		return  (int)paint.measureText(string);
	}
	
	
	public static boolean isLetter(char c) {  
        int k = 0x80;  
        return c / k == 0 ? true : false;  
    }  
  
    /** 
     * 得到一个字符串的长度,显示的长度,一个汉字或日韩文长度为2,英文字符长度为1 
     *  
     * @param s 需要得到长度的字符串 
     * @return i得到的字符串长度 
     */  
    public static int strlength(String s) {  
        if (s == null)  
            return 0;  
        char[] c = s.toCharArray();  
        int len = 0;  
        for (int i = 0; i < c.length; i++) {  
            len++;  
            if (!isLetter(c[i])) {  
                len++;  
            }  
        }  
        return len;  
    }  
  
    /** 
     * 截取一段字符的长度,不区分中英文,如果数字不正好，则少取一个字符位 
     *  
     *  
     * @param  origin 原始字符串 
     * @param len 截取长度(一个汉字长度按2算的) 
     * @param c 后缀            
     * @return 返回的字符串 
     */  
    public static String tosubstring(String origin, int len,String c) {  
        if (origin == null || origin.equals("") || len < 1)  
            return "";  
        byte[] strByte = new byte[len];  
        if (len > strlength(origin)) {  
            return origin+c;  
        }  
        try {  
            System.arraycopy(origin.getBytes("GBK"), 0, strByte, 0, len);  
            int count = 0;  
            for (int i = 0; i < len; i++) {  
                int value = (int) strByte[i];  
                if (value < 0) {  
                    count++;  
                }  
            }  
            if (count % 2 != 0) {  
                len = (len == 1) ? ++len : --len;  
            }  
            origin="";
            return new String(strByte, 0, len, "GBK")+c;  
        } catch (UnsupportedEncodingException e) {  
            throw new RuntimeException(e);  
        }  
    }
}
