package com.stark.adapter;

import android.widget.ImageView;
import android.widget.TextView;

/**
 * 好几个adapter公用是viewholder雷沃
 * 
 * @author Administrator
 * 
 */
public final class ViewHolder {
	/** 播放列表中 列表名 **/
	public TextView listName;
	/** 专辑图片 **/
	public ImageView albumImage; //
	/** 音乐标题 **/
	public TextView musicTitle; //
	/** 专辑名称 **/
	public TextView albumName; //
	/** 音乐艺术家 **/
	public TextView musicArtist; //
	/** 字母分隔行 ***/
	public TextView letterTV; //
	/**专辑列表中的歌曲统计信息行**/
	public TextView albumSongsStat;
	/**专辑歌曲界面 序号***/
	public TextView albumSongOrder;
	/**歌曲时长**/
	public TextView songDuration;
	/**艺术家封面图片**/
	public ImageView performerImage;
	/**艺术家详细内容**/
	public TextView performerDetail;
}
