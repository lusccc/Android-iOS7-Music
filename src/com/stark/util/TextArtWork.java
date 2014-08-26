package com.stark.util;

import com.stark.music.activity.MainActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class TextArtWork {

	/**
	 * 创建文字的专辑封面
	 * 
	 * @param albumName
	 * @param artistName
	 * @return
	 */
	public static Bitmap getTextArtwork(String albumName, String artistName,
			Context context) {
		int width = Dip2Px.dip2px(context, 70);
		int height = Dip2Px.dip2px(context,70);
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.rotate(-35, width / 2, height / 2);
		canvas.translate(0, height / 2);
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(28.0F);
		textPaint.setColor(Color.parseColor("#525252"));
		String string1 = (albumName + artistName);
		String string2 = ConvertToShortTitle.tosubstring(string1, 25, "");
		StaticLayout sl = new StaticLayout(string2, textPaint,
				bitmap.getWidth() - 8, Layout.Alignment.ALIGN_CENTER, 1.0f,
				0.0f, false);
		sl.draw(canvas);
		// Log.e("", ""+bitmap);
		return bitmap;
	}
	
	
	public static Bitmap getTextArtworkForGridView(String albumName, String artistName,
			Context context) {
		int a = MainActivity.getScreenHeight()/2;
		int b = MainActivity.getScreenWidth()/2;
		int screenWidth = Math.min(b, a);
		int width = screenWidth/3;
		int height = width;
		
		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.parseColor("#000000"));
		canvas.rotate(-35, width / 2, height / 2);
		canvas.translate(width/15, height / 4);
		TextPaint textPaint = new TextPaint();
		textPaint.setAntiAlias(true);
		textPaint.setTextSize(36.0F);
		textPaint.setColor(Color.parseColor("#FFFFFF"));
		String string1 = (albumName + artistName);
		String string2 = ConvertToShortTitle.tosubstring(string1, 16, "");
		StaticLayout sl = new StaticLayout(string2, textPaint,
				bitmap.getWidth() - 8, Layout.Alignment.ALIGN_CENTER, 1.0f,
				0.0f, false);
		sl.draw(canvas);
		return bitmap;
	}
}
