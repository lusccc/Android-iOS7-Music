package com.stark.util;

import android.content.Context;
import android.widget.ImageView;

public class MyClone implements Cloneable {
	private ImageView iv;
	private Context context;

	public MyClone(Context context) {
		// TODO Auto-generated constructor stub
		this.context = context;
		iv = new ImageView(context);
	}

	public Object clone() {
		MyClone o = null;
		try {
			o = (MyClone) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		o.iv = new ImageView(context);
		return o;
	}
	
	public ImageView getClonedImageView(){
		return iv;
	}
}