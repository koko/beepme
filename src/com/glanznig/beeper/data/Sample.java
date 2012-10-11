package com.glanznig.beeper.data;

import java.util.Date;

public class Sample {
	
	private long id;
	private Date timestamp;
	private String title;
	private String description;
	private Boolean accepted;
	private String photoUri;
	private byte[] photoThumb;
	
	public Sample() { }
	
	public Sample(long id) {
		setId(id);
	}
	
	public long getId() {
		return id;
	}
	
	private void setId(long id) {
		this.id = id;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
		this.accepted = accepted;
	}
	
	public void setPhotoUri(String photoUri) {
		this.photoUri = photoUri;
	}
	
	public String getPhotoUri() {
		return photoUri;
	}
	
	public void setPhotoThumb(byte[] photoThumb) {
		this.photoThumb = photoThumb;
	}
	
	public byte[] getPhotoThumb() {
		return photoThumb;
	}

}