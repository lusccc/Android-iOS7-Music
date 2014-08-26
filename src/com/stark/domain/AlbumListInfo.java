package com.stark.domain;

import java.util.List;

/***
 * 储存专辑信息类
 * 
 * @author Administrator
 * 
 */
public class AlbumListInfo {
	private String albumName;
	private long total_duration;
	private List<Mp3Info> mp3Infos_album;
	private String albumArtist;
	private String sortLetters; // 显示数据拼音的首字母
	private long id;
	private String amount;
	private long albumId;

	public void setAlbumName(String name) {
		this.albumName = name;
	}

	public void setTotalDuration(long duration) {
		this.total_duration = duration;
	}

	public void addMp3Infos_album(List<Mp3Info> mp3Infos_album) {
		this.mp3Infos_album = mp3Infos_album;
	}

	public void setAlbumArtist(String artistName) {
		this.albumArtist = artistName;
	}
	public void setId(long id){
		this.id = id;
	}
	public void setAmount(String amount){
		this.amount = amount;
	}
	public void setAlbumId(long albumId){
		this.albumId = albumId;
	}
	public String getAlbumName() {
		return albumName;
	}

	public String getAmount() {
		return amount;
	}

	public long getTotalDuration() {
		return total_duration;
	}

	public List<Mp3Info> getMp3Infos_album() {
		return mp3Infos_album;
	}

	public String getAlbumArtist() {
		return albumArtist;
	}

	public String getSortLetters() {
		return sortLetters;
	}
	public long getId(){
		return id;
	}

	public void setSortLetters(String sortLetters) {
		this.sortLetters = sortLetters;
	}
	public long getAlbumId(){
		return albumId;
	}
}
