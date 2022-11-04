package com.example.helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class MD5Helper {

	
	public static byte[] Digest(byte[] input) throws NoSuchAlgorithmException
	{
		MessageDigest md = MessageDigest.getInstance("MD5");
		byte[] resultOfDigest = md.digest(input);
		return resultOfDigest;
	}
	
	public static String bytesToHexadecimal(byte[] byteInput)
	{
		StringBuilder sb = new StringBuilder();
		for(byte b : byteInput)
		{
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
	
	public static String hashString(String data) throws NoSuchAlgorithmException
	{
		byte[] md5HashingInBytes = MD5Helper.Digest(data.getBytes(StandardCharsets.UTF_8));
		String bytesToHex = MD5Helper.bytesToHexadecimal(md5HashingInBytes);
		return bytesToHex;
	}
}