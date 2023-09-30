package com.face.recognition.service;

import com.face.recognition.model.ImageDTO;

public interface PaymentServiceApi {

	String beginKYC(ImageDTO imageDTO);
	
	String validateFace(byte[] imageBytes);

	
}
