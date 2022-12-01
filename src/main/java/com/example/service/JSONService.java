package com.example.service;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import com.example.dao.PlanDAO;
import com.example.helper.MD5Helper;
import com.example.model.Plan;

import redis.clients.jedis.Jedis;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;

@Service
public class JSONService {
	
	final static String PLAN = "PLAN";
	 PlanDAO planDAO = new PlanDAO();
	 private Jedis jedis = new Jedis();
	 private HashOperations hashOperations;
	 /**
	  * 
	  * @param json
	  * @return
	  */
	 
	 public static JSONService ReturnJSONService()
	 {
		 return new JSONService();
	 }
	
	public JSONObject ValidateWhetherSchemaIsValid(String json) 
	{
		InputStream schemaStream = JSONService.class.getResourceAsStream("/schema.json");
		
		JSONObject jsonSchema = new JSONObject(new JSONTokener(schemaStream));
		JSONObject jsonCurrentObject = new JSONObject(new JSONTokener(json));
		
		Schema schema = SchemaLoader.load(jsonSchema);
		schema.validate(jsonCurrentObject);
		return jsonCurrentObject;
	}
	
	public Map<String, Plan> findAll() {
        return hashOperations.entries(PLAN);
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
                    String jsonObjKey = jsonKey + "-" + jsonValueObject.get("objectId");
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
    
    private boolean isStringInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    
    public void updatePlan(JSONObject json, String ETag, String objectType, String objectId )
    {
    	String keyOfJSONBody = GenerateKeyForJSONObject(objectType, objectId);
    	String keyForETag = GenerateETagKeyForJSONObject(objectType, objectId);
    	planDAO.deletePlanFromRedis(keyOfJSONBody, keyForETag);
    	
    	planDAO.savePlanToRedis(keyForETag, ETag, keyOfJSONBody, json.toString());
    }
    
    private Map<String, Object> getOrDeleteData(String redisKey, Map<String, Object> outputMap, boolean isDelete) {
        Set<String> keys = jedis.keys(redisKey + ":*");
        keys.add(redisKey);
        jedis.close();
        for (String key : keys) {
            if (key.equals(redisKey)) {
                if (isDelete) {
                    jedis.del(new String[]{key});
                    jedis.close();
                } else {
                    Map<String, String> val = jedis.hgetAll(key);
                    jedis.close();
                    for (String name : val.keySet()) {
                        if (!name.equalsIgnoreCase("eTag")) {
                            outputMap.put(name,
                                    isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
                        }
                    }
                }

            } else {
                String newStr = key.substring((redisKey + ":").length());
                Set<String> members = jedis.smembers(key);
                jedis.close();
                if (members.size() > 1 || newStr.equals("linkedPlanServices")) {
                    List<Object> listObj = new ArrayList<Object>();
                    for (String member : members) {
                        if (isDelete) {
                            getOrDeleteData(member, null, true);
                        } else {
                            Map<String, Object> listMap = new HashMap<String, Object>();
                            listObj.add(getOrDeleteData(member, listMap, false));

                        }
                    }
                    if (isDelete) {
                        jedis.del(new String[]{key});
                        jedis.close();
                    } else {
                        outputMap.put(newStr, listObj);
                    }

                } else {
                    if (isDelete) {
                        jedis.del(new String[]{members.iterator().next(), key});
                        jedis.close();
                    } else {
                        Map<String, String> val = jedis.hgetAll(members.iterator().next());
                        jedis.close();
                        Map<String, Object> newMap = new HashMap<String, Object>();
                        for (String name : val.keySet()) {
                            newMap.put(name,
                                    isStringInt(val.get(name)) ? Integer.parseInt(val.get(name)) : val.get(name));
                        }
                        outputMap.put(newStr, newMap);
                    }
                }
            }
        }
        return outputMap;
    }
    
    public Map<String, Object> getPlan(String key) {
        Map<String, Object> outputMap = new HashMap<String, Object>();
        getOrDeleteData(key, outputMap, false);
        return outputMap;
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