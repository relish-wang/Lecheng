package com.lechange.demo.business.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

public class SignHelper {
	 public static String getSystem(String data, String appId, String appSecret, String sysVersion){
	    	StringBuffer sign = new StringBuffer();
	    	try {
	    		JSONObject params = new JSONObject(data);
				Iterator<?> it = params.keys();
				List<String> keyList = new ArrayList<String>();
				while(it.hasNext()){
					keyList.add(it.next().toString());
				}
				Collections.sort(keyList);
				for(String key : keyList){
					sign.append("").append(key).append(":").append(params.get(key).toString()).append(",");
				}
				//System.out.println(sign);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	String time = Long.toString(System.currentTimeMillis()/1000);
	    	String nonce = randomString(32);
	    	sign.append("time").append(":").append(time).append(",");
	    	sign.append("nonce").append(":").append(nonce).append(",");
	    	sign.append("appSecret").append(":").append(appSecret);
	    	//System.out.println(sign);
	    	JSONObject system = new JSONObject();
	    	try {
				system.put("ver", sysVersion);
				system.put("sign", md5Hex(sign.toString()));
				system.put("appId", appId);
				system.put("time", time);
				system.put("nonce", nonce);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    	return system.toString();
	    }
	 
	 public static String md5Hex(String str) {
			try {
				byte hash[] = MessageDigest.getInstance("MD5").digest(str.getBytes());
				StringBuilder hex = new StringBuilder(hash.length * 2);
				for (byte b : hash) {
					if ((b & 0xFF) < 0x10) {
						hex.append("0");
					}
					hex.append(Integer.toHexString(b & 0xFF));
				}
				return hex.toString();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		}
	 
	 final static String VEC = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
	 private static Random rand;
	 public static String randomString(int length) {
			if (rand == null) {
				rand = new Random(System.currentTimeMillis());
			}
			String ret = "";
			for (int i = 0; i < length; i++) {
				ret = ret + VEC.charAt(rand.nextInt(VEC.length()));
			}
			return ret;
		}
}
