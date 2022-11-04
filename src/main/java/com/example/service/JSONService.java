package com.example.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.example.dao.PlanDAO;
import com.example.helper.MD5Helper;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;


public class JSONService {
	
	 PlanDAO planDAO = new PlanDAO();
	
	 /**
	  * 
	  * @param json
	  * @return
	  */
	
	public JSONObject ValidateWhetherSchemaIsValid(String json) 
	{
		InputStream schemaStream = JSONService.class.getResourceAsStream("/schema.json");
		
		JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
		JSONObject jsonCurrentObject = new JSONObject(new JSONTokener(json));
		
		Schema schema = SchemaLoader.load(jsonSchema);
		schema.validate(jsonCurrentObject);
		return jsonCurrentObject;
	}
	
	/**
	 * 
	 * @param objectType
	 * @param objectID
	 * @return
	 */
	
	public String GenerateKeyForJSONObject(String objectType, String objectID)
	{
		String keyForJSON = objectType + "-" + objectID;
		return keyForJSON;
	}
	
	/**
	 * 
	 * @param keyOfJSON
	 * @return
	 */
	
	public boolean DoesPlanExistInSystem(String keyOfJSONPlan) 
	{
		String jsonInString = planDAO.getPlanRecordFromRedis(keyOfJSONPlan);
		if(jsonInString != null)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * @param objectType
	 * @param objectID
	 * @return
	 */
	
	public String GenerateETagKeyForJSONObject(String objectType, String objectID)
	{
		return objectType + "|" + objectID;
	}
	
	/**
	 * 
	 * @param eTagKey
	 * @return
	 */
	
	public String GetETagByETagKey(String eTagKey)
	{
		String eTag = planDAO.getETagByKey(eTagKey);
		return eTag;
	}
	
	public String GetETagOfSavedPlan(String objectType, String objectID)
	{
		String jsonKey = GenerateETagKeyForJSONObject(objectType, objectID);
		return planDAO.getETagByPlanKey(jsonKey);
	}
	
	/**
	 * 
	 * @param jsonKey
	 * @return
	 */
	
	public JSONObject GetPlanByKey(String jsonKey)
	{
		String jsonInString = planDAO.getPlanRecordFromRedis(jsonKey);
		return new JSONObject(new JSONTokener(jsonInString));
	}
	
	
	public String saveJSON(String requestBody, String objectType, String objectId) throws NoSuchAlgorithmException
	{
		 String keyOfJSONBody = GenerateKeyForJSONObject(objectType, objectId);
		 String ETag = MD5Helper.hashString(requestBody);
		 String ETagKey = GenerateETagKeyForJSONObject(objectType, objectId);
		 planDAO.savePlanToRedis(ETagKey, ETag, keyOfJSONBody, requestBody);
		 return ETag;

	}
	
	public String getPlanRecord(String objectType, String objectID)
	{
		String keyOfJSONBody = GenerateKeyForJSONObject(objectType, objectID);
		String jsonInString = planDAO.getPlanRecordFromRedis(keyOfJSONBody);
		return jsonInString;
	}
	
	public void deletePlanRecord(String objectType, String objectID)
	{
		 String keyOfJSONBody = GenerateKeyForJSONObject(objectType, objectID);
		 String keyForETag = GenerateETagKeyForJSONObject(objectType, objectID);
		 planDAO.deletePlanFromRedis(keyOfJSONBody, keyForETag);

	}
	
	 // merge the incoming json object with the object in db.
    public JSONObject mergeJson(JSONObject json, String objectKey) 
    {
        JSONObject savedObject = GetPlanByKey(objectKey);
        if (savedObject == null)
            return null;

        // iterate the new json object
        for(String jsonKey : json.keySet()) {
            Object jsonValue = json.get(jsonKey);

            // check if this is an existing object
            if (savedObject.get(jsonKey) == null) {
                savedObject.put(jsonKey, jsonValue);
            } 
            else 
            {
                if (jsonValue instanceof JSONObject) 
                {
                    JSONObject jsonValueObject = (JSONObject)jsonValue;
                    String jsonObjKey = jsonKey + "_" + jsonValueObject.get("objectId");
                    if (((JSONObject)savedObject.get(jsonKey)).get("objectId").equals(jsonValueObject.get("objectId"))) 
                    {
                        savedObject.put(jsonKey, jsonValue);
                    } 
                    else 
                    {
                        JSONObject updatedJsonValue = this.mergeJson(jsonValueObject, jsonObjKey);
                        savedObject.put(jsonKey, updatedJsonValue);
                    }
                } 
                else if (jsonValue instanceof JSONArray) 
                {
                    JSONArray jsonValueArray = (JSONArray) jsonValue;
                    JSONArray savedJSONArray = savedObject.getJSONArray(jsonKey);
                    for (int i = 0; i < jsonValueArray.length(); i++) 
                    {
                        JSONObject arrayItem = (JSONObject)jsonValueArray.get(i);
                        //check if objectId already exists in savedJSONArray
                        int index = getIndexOfObjectId(savedJSONArray, (String)arrayItem.get("objectId"));
                        if(index >= 0) 
                        {
                            savedJSONArray.remove(index);
                        }
                        savedJSONArray.put(arrayItem);
                    }
                    savedObject.put(jsonKey, savedJSONArray);
                } 
                else 
                {
                    savedObject.put(jsonKey, jsonValue);
                }
            }

        }
        return savedObject;
    }
    
    public void updatePlan(JSONObject json, String ETag, String objectType, String objectId )
    {
    	String keyOfJSONBody = GenerateKeyForJSONObject(objectType, objectId);
    	String keyForETag = GenerateETagKeyForJSONObject(objectType, objectId);
    	planDAO.deletePlanFromRedis(keyOfJSONBody, keyForETag);
    	
    	planDAO.savePlanToRedis(keyForETag, ETag, keyOfJSONBody, json.toString());
    }
    
    private int getIndexOfObjectId(JSONArray array, String objectId) {
    	 

        for (int i = 0; i < array.length(); i++) {
            JSONObject arrayObj = (JSONObject)array.get(i);
            String itemId = (String)arrayObj.get("objectId");
            if (objectId.equals(itemId)){
                return i;
            }
        }

        return -1;
    }
}