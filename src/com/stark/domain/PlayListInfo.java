package com.stark.domain;

public class PlayListInfo {
	private String listName;
	private long songId = 0;
	private long albumId = 0;
	
	public void setListName(String name){
		this.listName = name;
	}
	public void setSongId(long id){
		songId = id;
	}
	public void setAlbumId(long id){
		albumId = id;
	}
	public String getListName(){
		return listName;
	}
	public long getSongId(){
		return songId;
	}
	public long getAlbumId(){
		return albumId;
	}
	
}
