package com.face.recognition.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.face.recognition.model.ImageDTO;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;
import software.amazon.awssdk.services.rekognition.model.FaceMatch;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Landmark;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageRequest;
import software.amazon.awssdk.services.rekognition.model.SearchFacesByImageResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class PaymentServiceImpl implements PaymentServiceApi {
	
	@Value("${bucket.name}")
	String BUCKET_NAME;
	
	private S3Client s3;

	private RekognitionClient rekognition;

	private DynamoDbClient dynamodb;
	
	 public PaymentServiceImpl() {
	    	this.s3 = S3Client.builder()
	                .region(Region.US_EAST_1)  
	                .build();
	    	
	    	this.rekognition = RekognitionClient.builder().region(Region.US_EAST_1).build();
	        this.dynamodb = DynamoDbClient.builder().region(Region.US_EAST_1).build();
	    }
	

//	@Override
//	public String beginKYC(ImageDTO imageDTO) {
//		
//		// TODO Auto-generated method stub
//		try
//		{
//			String base64Image = imageDTO.getImage().split(",")[1];
//		    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
//		    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
//
//		    // Convert BufferedImage to byte array for S3
//		    ByteArrayOutputStream os = new ByteArrayOutputStream();
//		    ImageIO.write(img, "png", os);
//		    byte[] buffer = os.toByteArray();
//
//		    // Generate a random file name 
//		    Random rand = new Random();
//		    String fileName = "index/image_" + rand.nextInt(100000000) + ".png";
//
//		    // Creating a metadata map
//		    Map<String, String> metadataMap = new HashMap<>();
//		    metadataMap.put("fullname", imageDTO.getUsername()); 
//
//		    // Upload to S3
//		    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//		            .bucket(BUCKET_NAME)
//		            .key(fileName)
//		            .metadata(metadataMap)  // Setting metadata
//		            .build();
//
//		    
//		    s3.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(buffer));
//
//		    System.out.println("Image uploaded to S3 with name: " + fileName);
//		    
//		    return "KYC Completed";
//		}
//		catch(Exception e)
//		{
//			System.out.println("Exception occured in KYC - "+e.getMessage());
//			return "Something went wrong";
//		}
//				
//	}

	 @Override
	 public String beginKYC(ImageDTO imageDTO) {
	     try {
	         String base64Image = imageDTO.getImage().split(",")[1];
	         byte[] imageBytes = Base64.getDecoder().decode(base64Image);
	         BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

	         // Detect faces and quality in the image
	         Image imageForRekognition = Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build();
	         DetectFacesResponse detectFacesResponse = rekognition.detectFaces(DetectFacesRequest.builder().image(imageForRekognition).build());

	         int detectedFacesCount = detectFacesResponse.faceDetails().size();

	         // Check the number of detected faces
	         if (detectedFacesCount != 1) {
	             return detectedFacesCount == 0 ? "No face detected" : "Multiple faces detected";
	         }

	         FaceDetail faceDetails = detectFacesResponse.faceDetails().get(0);
	         
	      // Check if sunglasses are detected
	         if (faceDetails.sunglasses() != null && faceDetails.sunglasses().value()) {
	             return "Face with sunglasses detected. Please remove them for KYC verification.";
	         }

	         // Check for obscured landmarks
	      // Get a list of all detected landmark types
	         List<String> detectedLandmarkTypes = faceDetails.landmarks().stream()
	             .map(landmark -> landmark.typeAsString().toUpperCase())
	             .collect(Collectors.toList());
	         
	         System.out.println("detectedLandmarkTypes :"+detectedLandmarkTypes);
	         
	         // List of landmarks you want to ensure are visible
	         List<String> essentialLandmarks = Arrays.asList("EYELEFT", "EYERIGHT", "NOSE", "MOUTHLEFT", "MOUTHRIGHT");

	         // Check if any essential landmark is missing
	         boolean isAnyLandmarkMissing = essentialLandmarks.stream()
	             .anyMatch(landmark -> !detectedLandmarkTypes.contains(landmark));

	         if (isAnyLandmarkMissing) {
	             return "Face is partially obscured or not fully visible.";
	         }

	         // Check image quality (assuming thresholds are 40 for both brightness and sharpness for this example)
	         if (faceDetails.quality().brightness() < 40 || faceDetails.quality().sharpness() < 40) {
	             return "Poor image quality. Ensure good lighting and focus.";
	         }

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
	                 .metadata(metadataMap)
	                 .build();

	         s3.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(buffer));

	         System.out.println("Image uploaded to S3 with name: " + fileName);

	         return "KYC Completed";
	     } catch (Exception e) {
	         System.out.println("Exception occurred in KYC - " + e.getMessage());
	         return "Something went wrong";
	     }
	 }

	 
	 @Override
	 public String validateFace(byte[] imageBytes) {
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
	             result.append("Found Person: ")
	                   .append(face.get("FullName").s())
	                   .append(" with similarity: ")
	                   .append(match.similarity())  // Add the similarity score
	                   .append("%\n");
	             found = true;
	         }
	     }

	     if (!found) {
	         result.append("Person cannot be recognized");
	     }

	     return result.toString();
	 }


}
