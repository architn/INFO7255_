package com.example.api;

import java.io.IOException;
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
import com.example.model.Plan;
import com.example.service.AuthorizationService;
import com.example.service.IndexingService;
import com.example.service.JSONService;


@Controller
public class RestController extends API {
	
	 
	 static Map<String, Boolean> authorizationStatus = new HashMap<>();
	 
	 @Autowired
	 AuthorizationService authService;
	 
	 @Autowired
	 JSONService jsonService;
	 
	 @Autowired
	 private RabbitTemplate template;

	 
	 @RequestMapping(method = RequestMethod.GET, value = "/plan")
	 public ResponseEntity<String> GetAllPlans(@RequestHeader(name = HttpHeaders.AUTHORIZATION) String token)
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 Map<String, Plan> plans = jsonService.findAll();
			 return OK(AppConstants.SUCCESS_MESSAGE) ;
		 }
		 return forbidden(authorizationStatus.keySet().toString());
	 }
	 
	 /**
	  * This method is used to save JSON data to Redis
	  * @param body9
	  * @param headers
	  * @return
	 * @throws IOException 
	  */
	 
	 @RequestMapping(value = "/plan", method = RequestMethod.POST)
	 public ResponseEntity<String> Save(@RequestBody String body, @RequestHeader Map<String, String> headers, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token) throws IOException
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
			 Map<String, String> actionMap = new HashMap<>();
             actionMap.put("operation", "SAVE");
             actionMap.put("body", body);
			 template.convertAndSend(Info7255Application.EXCHANGE, actionMap);
			 return created(ETag);
	 	}
	 	else
	 	{
	 		return forbidden(authorizationStatus.keySet().toString());
	 	}
	 }
	 
	 
	 @RequestMapping(value = "/add", method = RequestMethod.POST)
	 public ResponseEntity<String> AddNewPlan(@RequestBody String request, @RequestHeader HttpHeaders requestHeaders) throws IOException
	 {
		 try {
			 String token = requestHeaders.getFirst("Authorization");
			 authorizationStatus = authService.authorize(token);
			 JSONObject jsonPlan = new JSONObject(request);
			 if(authorizationStatus.containsValue(true))
			 {
				 JSONObject jsonObject = jsonService.ValidateWhetherSchemaIsValid(request);
				 String key = jsonPlan.get("objectType") + ":" + jsonPlan.get("objectId");
				 boolean existingPlan = jsonService.checkIfKeyExists(key);
				 if(!existingPlan)
				 {
					String eTag = jsonService.save(jsonPlan, key, request);
	                JSONObject obj = new JSONObject();
	                obj.put("ObjectId", jsonPlan.get("objectId"));

	                Map<String, String> actionMap = new HashMap<>();
	                actionMap.put("operation", "SAVE");
	                actionMap.put("body", request);
	                template.convertAndSend(Info7255Application.QUEUE, actionMap);
	                return created(eTag);
				 }
				 else {
					 return conflict(AppConstants.OBJECT_ALREADY_EXISTS);
				 }
			 }
			 else {
				 return forbidden(authorizationStatus.keySet().toString());
			 }
		 }
		 catch(ValidationException v) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{ message : '" + v.getMessage() + "' }");
			}
		 catch(Exception ex) {
			 ex.printStackTrace();
			 return internalServerError(AppConstants.INTERNAL_SERVER_ERROR);

		 }
	 }
	 
	 
	 /**
	  * 
	  * @param objectType
	  * @param objectID
	  * @return
	  */
	 
	 @SuppressWarnings("unused")
	 @RequestMapping(value = "/{objectType}/{ID}", method = RequestMethod.GET)
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
	 
	 @RequestMapping(value = "/{objectType}/{ID}", method = RequestMethod.GET, headers = "If-Match")
	 @ResponseBody
	 private ResponseEntity GetJSONWithETag(@PathVariable("objectType") String objectType,
	            @PathVariable("ID") String objectId,
	            @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch, @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token)
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 try 
			 {
		         String key = objectType + ":" + objectId;
		         Map<String, Object> plan = jsonService.getPlan(key);
		         if(plan == null || plan.isEmpty())
		         {
					 return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{ message : '" + AppConstants.OBJECT_NOT_FOUND + "' }");

		         }
		         String ETag = jsonService.getEtag(key);
				 if(!ETag.equals(ifMatch)) 
				 {
					 return ResponseEntity.status(HttpStatus.OK).eTag(ETag).body(plan);
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
	 
	 @RequestMapping(value = "/{objectType}/{ID}", method = RequestMethod.DELETE)
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
				 	Map<String, Object> plan = jsonService.getPlan(keyOfJSONBody);
	                Map<String, String> actionMap = new HashMap<>();
	                actionMap.put("operation", "DELETE");
	                actionMap.put("body", new JSONObject(plan).toString());

	                template.convertAndSend(Info7255Application.QUEUE, actionMap);
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
	 
	 
	 @RequestMapping(value = "/{objectType}/{ID}", method = RequestMethod.DELETE, headers = "If-Match")
	 @ResponseBody
	 private ResponseEntity<String> DeleteJSONIfMatch(@PathVariable("objectType") String objectType, 
			 @PathVariable("ID") String objectID, @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch, 
			 @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token
			 )
	 {
		 authorizationStatus = authService.authorize(token);
		 if(authorizationStatus.containsValue(true))
		 {
			 String key = objectType + ":" + objectID;
	            boolean existingPlan = jsonService.checkIfKeyExists(key);
	            if (!existingPlan) {
	            	return ResponseEntity.status(HttpStatus.NOT_FOUND)
		        			.body(new JSONObject().put("msg", "Plan not found!").toString());
	            } 
	            
	            String actualEtag = jsonService.getEtag(key);
	            if (ifMatch == null || ifMatch.isEmpty()) {
	                return new ResponseEntity<>("E-Tag not provided!", HttpStatus.BAD_REQUEST);
	            }
	            if (ifMatch != null && !ifMatch.equals(actualEtag)) {
	                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
	                		.body(new JSONObject().put("msg", "Plan cannot be found or has been already deleted!!").toString());
	            }
	            Map<String, Object> plan = jsonService.getPlan(key);
	            Map<String, String> actionMap = new HashMap<>();
	            actionMap.put("operation", "DELETE");
	            actionMap.put("body", new JSONObject(plan).toString());

	            template.convertAndSend(Info7255Application.QUEUE, actionMap);

	            jsonService.delete(key);
	            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("{ message : '" + AppConstants.OBJECT_DELETED + "' }");
		 }
		 else
		 {
			 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{ message : '" +authorizationStatus.keySet().toString() + "' }");
		 }
		 
	 }
	 
	 @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PATCH, headers = "If-Match")
	    @ResponseBody
	    public ResponseEntity<String> patchJsonIfNoneMatch(@PathVariable("object") String objectType,
	            @PathVariable("id") String objectId, @RequestBody String body,
	            @RequestHeader(name = HttpHeaders.IF_MATCH) String eTagFromHeader, 
	            @RequestHeader(name = HttpHeaders.AUTHORIZATION) String token) throws NoSuchAlgorithmException 
	 {
	        try
	        {
	        	Map<String, String> actionMap = new HashMap<>();
	        	String actualEtag = null;
		        
		        authorizationStatus = authService.authorize(token);
				if(authorizationStatus.containsValue(true))
				{
		            JSONObject jsonPlan = new JSONObject(body);
					String key =  objectType + ":" + objectId;
					if(!jsonService.checkIfKeyExists(key))
					{
						return notFound(AppConstants.OBJECT_NOT_FOUND);
					}

					actualEtag = jsonService.getEtag(key);
					  if (eTagFromHeader != null && !eTagFromHeader.equals(actualEtag)) 
					  {
			                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).eTag(actualEtag)
			                        .body(new JSONObject().put("message", "Plan was updated by another user").toString());
			           }
					  String newEtag = jsonService.save(jsonPlan, key, body);
			          Map<String, Object> plan = jsonService.getPlan(key);

			          actionMap.put("operation", "SAVE");
			          actionMap.put("body", new JSONObject(plan).toString());

			          System.out.println("Sending message: " + actionMap);
			          template.convertAndSend(Info7255Application.QUEUE, actionMap);
					  return successfulUpdate(newEtag);

				}
				else {
					 return ResponseEntity.status(HttpStatus.FORBIDDEN).body("{ message : '" +authorizationStatus.keySet().toString() + "' }");

				}
	            
	        }
	        catch(JSONException jex)
	        {
	        	return internalServerError(AppConstants.INVALID_FORMAT);
	        }

	    }
	 
	 
	 
	 
	
}