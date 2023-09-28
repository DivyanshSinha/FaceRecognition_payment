package com.face.recognition.controller;

import java.awt.image.BufferedImage;
import java.util.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.face.recognition.model.ImageDTO;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


@RestController
public class PaymentController {
//	
//	@Autowired
//	S3Client s3;
	
//	@Autowired
//	ImageDTO imageDto;
	
//	@Autowired
	RekognitionClient rekognition;
	
//	@Autowired
	DynamoDbClient dynamodb;
	
	private final String BUCKET_NAME = "faces3bucket"; // Change to your bucket name
	
//	@Autowired
    S3Client s3;

    public PaymentController() {
    	this.s3 = S3Client.builder()
                .region(Region.US_EAST_1)  
                .build();
    	
    	this.rekognition = RekognitionClient.builder().region(Region.US_EAST_1).build();
        this.dynamodb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
    }
	
	@GetMapping("/openCamera")
	public ModelAndView openCamera() {
		System.out.println("Open camera triggered");
		ModelAndView modelAndView = new ModelAndView();
		modelAndView.setViewName("cameraView");  
		return modelAndView;
	}
	
	@PostMapping("/storeImage")
	public ResponseEntity<Map<String, String>> storeImage(@RequestBody ImageDTO imageDTO) throws Exception {
		
		System.out.println("Store images to S3 triggered");
		
	    String base64Image = imageDTO.getImage().split(",")[1];
	    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
	    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

	    // Convert BufferedImage to byte array for S3
	    ByteArrayOutputStream os = new ByteArrayOutputStream();
	    ImageIO.write(img, "png", os);
	    byte[] buffer = os.toByteArray();

	    // Generate a random file name 
	    Random rand = new Random();
	    String fileName = "index/image_" + rand.nextInt(100000000) + ".png";

	    // Creating a metadata map
	    Map<String, String> metadataMap = new HashMap<>();
	    metadataMap.put("fullname", imageDTO.getUsername()); 

	    // Upload to S3
	    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
	            .bucket(BUCKET_NAME)
	            .key(fileName)
	            .metadata(metadataMap)  // Setting metadata
	            .build();

	    
	    s3.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(buffer));

	    System.out.println("Image uploaded to S3 with name: " + fileName);
	    
	    Map<String, String> responseMap = new HashMap<>();
	    responseMap.put("message", "KYC done");
	    
	    return ResponseEntity.ok(responseMap);
	}
	
	@PostMapping("/checkFace")
	public ResponseEntity<Map<String, String>> checkFace(@RequestBody Map<String, String> payload) throws Exception {
		
		System.out.println("Face verification started");
		
	    
	    String base64Image = payload.get("image").split(",")[1];
	    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
		
	    //byte[] imageBytes = file.getBytes();

	    SearchFacesByImageRequest request = SearchFacesByImageRequest.builder()
	            .collectionId("facerecognition_collection")
	            .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
	            .build();

	    SearchFacesByImageResponse response = rekognition.searchFacesByImage(request);

	    boolean found = false;
	    StringBuilder result = new StringBuilder();

	    for (FaceMatch match : response.faceMatches()) {
	        GetItemRequest getItemRequest = GetItemRequest.builder()
	                .tableName("face_recognition")
	                .key(Collections.singletonMap("RekognitionId", AttributeValue.builder().s(match.face().faceId()).build()))
	                .build();

	        Map<String, AttributeValue> face = dynamodb.getItem(getItemRequest).item();

	        if (face != null && face.containsKey("FullName")) {
	            result.append("Found Person: ").append(face.get("FullName").s()).append("\n");
	            found = true;
	        }
	    }

	    if (!found) {
	        result.append("Person cannot be recognized");
	    }
	    
	    System.out.println(result.toString());
	    
	    Map<String, String> responseMap = new HashMap<>();
	    responseMap.put("message", result.toString());
	    
	    return ResponseEntity.ok(responseMap);
	}


}
