package com.example.api;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.helper.AppConstants;


public class API {
	AppConstants AppConstants = new AppConstants();
	
	public ResponseEntity<String> created(String ETag)
	{
		return ResponseEntity.status(HttpStatus.CREATED).eTag(ETag).body(new JSONObject().put("message", AppConstants.SUCCESS_MESSAGE).toString());
	}
	
	public ResponseEntity<String> notModified(String jsonBody, String ETag)
	{
		 return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(ETag).body(jsonBody);
	}
	
	public ResponseEntity<String> notFound(String message)
	{
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new JSONObject().put("message", message).toString());
	}
	
	public ResponseEntity<String> OK(String jsonBody, String eTag)
	{
		return ResponseEntity.status(HttpStatus.OK).eTag(eTag).body(jsonBody);
	}
	
	public ResponseEntity<String> OK(String message)
	{
		return ResponseEntity.status(HttpStatus.OK).body(new JSONObject().put("message", message).toString());

	}
	
	public ResponseEntity<String> successfulUpdate(String eTag)
	{
		return ResponseEntity.status(HttpStatus.OK).eTag(eTag).body(new JSONObject().put("message", AppConstants.OBJECT_UPDATED).toString());
	}
	
	public ResponseEntity<String> noContent(String message)
	{
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(new JSONObject().put("message", message).toString());

	}
	
	public ResponseEntity<String> forbidden(String message)
	{
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONObject().put("message", message).toString());

	}
	
	public ResponseEntity<String> internalServerError(String message)
	{
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("message", message).toString());

	}
	
	public ResponseEntity<String> conflict(String message)
	{
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new JSONObject().put("message", message).toString());

	}
	
	public ResponseEntity<String> badRequest(String message)
	{
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", message).toString());

	}
}