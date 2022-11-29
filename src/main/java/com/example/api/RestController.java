package com.example.api;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.Info7255Application;
import com.example.helper.*;
import com.example.service.AuthorizationService;
import com.example.service.JSONService;


@Controller
public class RestController extends API {
	
	 
	 static HashMap<String, Boolean> authorizationStatus = new HashMap<>();
	 
	 @Autowired
	 AuthorizationService authService;
	 
	 @Autowired
	 JSONService jsonService;
	 
	 @Autowired
	 private RabbitTemplate template;
	 
	 
	 
	 /**
	  * This method is used to save JSON data to Redis
	  * @param body9
	  * @param headers
	  * @return
	  */
	 
	 @RequestMapping(value = "/add", method = RequestMethod.POST)
	 public ResponseEntity<String> Save(@RequestBody String body, @RequestHeader Map<String, String> headers, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token)
	 {
		authorizationStatus = authService.authorize(token);
	 	if(authorizationStatus.containsValue(true))
	 	{
	 		String ETag = "";
			 try {
				 JSONObject jsonObject = jsonService.ValidateWhetherSchemaIsValid(body);
				 String objType = jsonObject.getString("objectType");
				 String objID = jsonObject.getString("objectId");
				 String keyOfJSONBody = jsonService.GenerateKeyForJSONObject(objType, objID);
				 if(!jsonService.DoesPlanExistInSystem(keyOfJSONBody))
				 {
					 ETag = jsonService.saveJSON(body, objType, objID);
				 }
				 else 
				 {
					 return conflict(AppConstants.OBJECT_ALREADY_EXISTS);
				 }
				 
			 }
			 catch(ValidationException v) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ message : '" + v.getMessage() + "' }");
				}
			 catch(Exception ex) {
				 return internalServerError(AppConstants.INTERNAL_SERVER_ERROR);

			 }
			 template.convertAndSend(Info7255Application.EXCHANGE, body);
			 return created(ETag);
	 	}
	 	else
	 	{
	 		return forbidden(authorizationStatus.keySet().toString());
	 	}
	 }
	 
	 /**
	  * 
	  * @param objectType
	  * @param objectID
	  * @return
	  */
	 
	 @SuppressWarnings("unused")
	 @RequestMapping(value = "/get/{objectType}/{ID}", method = RequestMethod.GET)
	 @ResponseBody
	 private ResponseEntity<String> GetJSON(@PathVariable("objectType") String objectType, @PathVariable("ID") String objectID, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token) 
	 {
		 	authorizationStatus = authService.authorize(token);
		 	if(authorizationStatus.containsValue(true))
		 	{
		 		String jsonInString = "";
			 	String ETag = "";
				try {
					 jsonInString = jsonService.getPlanRecord(objectType, objectID);
					 JSONObject jsonObject = new JSONObject(new JSONTokener(jsonInString));
					 String eTagKey = jsonService.GenerateETagKeyForJSONObject(objectType, objectID);
					 ETag = jsonService.GetETagByETagKey(eTagKey);
					 String hashedETag = jsonService.GetETagByETagKey(eTagKey);
				}
				catch(NullPointerException n) 
				{
					 return notFound(AppConstants.OBJECT_NOT_FOUND);
				}
				catch(Exception ex) 
				{
					 return internalServerError(ex.getMessage());
				}
				return OK(jsonInString, ETag);
		 	}
		 	else 
		 	{
		 		return forbidden(authorizationStatus.keySet().toString());
		 	}
	 }
	 
	 /**
	  * 
	  * @param objectType
	  * @param objectId
	  * @param ifMatch
	  * @return
	  */
	 
	 @RequestMapping(value = "/get/{objectType}/{ID}", method = RequestMethod.GET, headers = "If-Match")
	 @ResponseBody
	 private ResponseEntity<String> GetJSONWithETag(@PathVariable("objectType") String objectType,
	            @PathVariable("ID") String objectId,
	            @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch, @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token)
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 try 
			 {
				 String eTagKey = jsonService.GenerateETagKeyForJSONObject(objectType, objectId);
				 String ETag = jsonService.GetETagByETagKey(eTagKey);		 
				 if(!ETag.equals(ifMatch)) 
				 {
					 JSONObject jsonObject = jsonService.GetPlanByKey(jsonService.GenerateKeyForJSONObject(objectType, objectId));
					 return ResponseEntity.status(HttpStatus.OK).eTag(ETag).body(jsonObject.toString());
				 }
				 else 
				 {
					 return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(ETag).body("Not Modified");
				 }
			 }
			 catch(NullPointerException n) 
			 {
				 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ message : '" + AppConstants.OBJECT_NOT_FOUND + "' }");
			 }
			 catch(Exception ex) 
			 {
				 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{ message : '" + ex.getMessage() + "' }");
			  }
		 }
		 else 
		 {
			 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{ message : '" +authorizationStatus.keySet().toString() + "' }");
		 }
		 
	 }
	 
	 /**
	  * 
	  * @param objectType
	  * @param objectID
	  * @return
	  */
	 
	 @RequestMapping(value = "/delete/{objectType}/{ID}", method = RequestMethod.DELETE)
	 @ResponseBody
	 private ResponseEntity<String> DeleteJSON(@PathVariable("objectType") String objectType, @PathVariable("ID") String objectID, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token)
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 try {
				 String keyOfJSONBody = jsonService.GenerateKeyForJSONObject(objectType, objectID);
				 if(jsonService.DoesPlanExistInSystem(keyOfJSONBody))
				 {
					jsonService.deletePlanRecord(objectType, objectID);
					 return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{ message : '" + AppConstants.OBJECT_DELETED + "' }");
				 }
				 else 
				 {
					 return notFound(AppConstants.OBJECT_NOT_FOUND);				 }
			 	  }
			 catch(Exception ex) {
				 
			 }
			 return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{ message : '" + AppConstants.OBJECT_DELETED + "' }");
		 }
		 else
		 {
			 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{ message : '" +authorizationStatus.keySet().toString() + "' }");
		 }
		 
	 }
	 
	 
	 @RequestMapping(value = "/delete/{objectType}/{ID}", method = RequestMethod.DELETE, headers = "If-Match")
	 @ResponseBody
	 private ResponseEntity<String> DeleteJSONIfMatch(@PathVariable("objectType") String objectType, 
			 @PathVariable("ID") String objectID, @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token
			 )
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 String eTagKey = jsonService.GenerateETagKeyForJSONObject(objectType, objectID);
			 String ETag = jsonService.GetETagByETagKey(eTagKey);
			 if(ifMatch.equals(ETag))
			 {
				 jsonService.deletePlanRecord(objectType, objectID);

			 }
			 else 
			 {
				 return notFound(AppConstants.OBJECT_NOT_FOUND);
			 }
			 return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{ message : '" + AppConstants.OBJECT_DELETED + "' }");
		 }
		 else
		 {
			 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{ message : '" +authorizationStatus.keySet().toString() + "' }");
		 }
		 
	 }
	 
	 @RequestMapping(value = "/edit/{object}/{id}", method = RequestMethod.PATCH, headers = "If-Match")
	    @ResponseBody
	    public ResponseEntity<String> patchJsonIfNoneMatch(@PathVariable("object") String objectType,
	            @PathVariable("id") String objectId, @RequestBody String body,
	            @RequestHeader(name = HttpHeaders.IF_MATCH) String eTagFromHeader, 
	            @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token) throws NoSuchAlgorithmException 
	 {
	        try
	        {
	        	String actualEtag = null;
	        	String newETag = "";
		        JSONObject updatedJSONObject = null;
		        
		        authorizationStatus = authService.authorize(token);
				if(authorizationStatus.containsValue(true))
				{
					String key = jsonService.GenerateKeyForJSONObject(objectType, objectId);
					JSONObject jsonObject = jsonService.GetPlanByKey(key);
					if(!jsonService.DoesPlanExistInSystem(key))
					{
						return notFound(AppConstants.OBJECT_NOT_FOUND);
					}
					actualEtag = jsonService.GetETagOfSavedPlan(objectType, objectId);

					  if (eTagFromHeader != null && !eTagFromHeader.equals(actualEtag)) 
					  {
			                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
			                        .body(new JSONObject().put("message", "Plan was updated by another user").toString());
			           }
					  JSONObject bodyOfUpdatedJSON = jsonService.ValidateWhetherSchemaIsValid(body);
					  updatedJSONObject = jsonService.mergeJson(bodyOfUpdatedJSON, jsonService.GenerateKeyForJSONObject(objectType, objectId));
					  if(jsonObject == null || jsonObject.isEmpty())
					  {
						  return notFound(AppConstants.OBJECT_NOT_FOUND);
					  }
					  newETag = MD5Helper.hashString(body);
					  jsonService.updatePlan(updatedJSONObject, newETag, objectType, objectId);
				}
		       return successfulUpdate(newETag);
	        }
	        catch(JSONException jex)
	        {
	        	return internalServerError(AppConstants.INVALID_FORMAT);
	        }

	    }
	 
	 
	
}