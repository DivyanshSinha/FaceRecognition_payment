package com.face.recognition.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class UIController {
	
	@GetMapping("/openCamera")
	public ModelAndView openCamera() {
		System.out.println("Open camera triggered");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("cameraView");  
		return modelAndView;
	}

}
