package com.face.recognition.model;

import org.hibernate.annotations.Entity;

@Entity
public class ImageDTO {
	
	private String image;
	
	private String username;

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	
	
}
