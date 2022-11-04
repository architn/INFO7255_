package com.example.dao;

import redis.clients.jedis.Jedis;

public class PlanDAO {
	
	 private Jedis redisMemory = new Jedis();

	 public void savePlanToRedis(String ETagKey, String ETag, String keyOfJSONBody, String requestBody)
	 {
		 redisMemory.set(ETagKey, ETag);
		 redisMemory.set(keyOfJSONBody, requestBody);
	 }
	 
	 public void deletePlanFromRedis(String keyOfJSONBody, String keyForETag) 
	 {
		 redisMemory.del(keyOfJSONBody);
		 redisMemory.del(keyForETag);
	 }
	 
	 public String getPlanRecordFromRedis(String keyOfJSONBody)
	 {
		 return redisMemory.get(keyOfJSONBody);
	 }
	 
	 public String getETagByKey(String eTagKey)
	 {
		 return redisMemory.get(eTagKey);
	 }
	 
	 public String getETagByPlanKey(String jsonKey)
	 {
		 return redisMemory.get(jsonKey);
	 }

}