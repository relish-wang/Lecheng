package com.lechange.demo.business.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Helper
{
	private static byte[] hex = new byte[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	 /**  
     * 对字符串进行MD5加密  
     * @param rawString   要加密的字符串
     * @return MD5摘要码  
     */ 
	public static String encode(String rawString)
	{
		String md5String = null;
		
		try
		{
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(rawString.getBytes());
			md5String = convertToHexString(md5.digest());
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		
		if (null != md5String)
		{
			return md5String.toUpperCase();
		}
		
		return md5String;
	}
	
	public static String encodeToLowerCase(String rawString){
		return encode(rawString).toLowerCase();
	}
	
	 /**  
     * 对文件全文生成MD5摘要  
     * @param file   要加密的文件  
     * @return MD5摘要码  
     */  
    public static String getMD5(File file) {   
        FileInputStream fis = null;   
        try {   
            MessageDigest md = MessageDigest.getInstance("MD5");   
  
            fis = new FileInputStream(file);   
            byte[] buffer = new byte[2048];   
            int length = -1;   
            while ((length = fis.read(buffer)) != -1) {   
                md.update(buffer, 0, length);   
            }   
            byte[] b = md.digest();   
            return convertToHexString(b);//byteToHexString(b);   
            // 16位加密   
            // return buf.toString().substring(8, 24);   
        } catch (Exception ex) {   
            ex.printStackTrace();   
            return null;   
        } finally {   
            try {   
                fis.close();   
            } catch (IOException ex) {   
                ex.printStackTrace();   
            }   
        }   
    } 

	private static String convertToHexString(byte[] digests)
	{
		byte[] md5String = new byte[digests.length * 2];
		
		int index = 0;
		for (byte digest : digests)
		{
			md5String[index] 		= hex[(digest >> 4) & 0x0F];
			md5String[index + 1] 	= hex[digest &0x0F];
			index += 2;
		}
		
		return new String(md5String);
	}
}
