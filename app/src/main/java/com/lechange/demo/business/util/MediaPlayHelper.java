package com.lechange.demo.business.util;

import java.io.File;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.os.Environment;
import android.view.WindowManager;

public class MediaPlayHelper {
	
	private final static String ProjectName = "LechangeDemo";
	
	public enum DHFilesType {
		DHImage,
		DHVideo
	}
	
	
	public static void setFullScreen(Activity activity) {
		activity.getWindow().setFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}

	public static void quitFullScreen(Activity activity) {
		final WindowManager.LayoutParams attrs = activity.getWindow().getAttributes();
		attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
		activity.getWindow().setAttributes(attrs);
		activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}
	
	

	/**
	 * 创建文件路径
	 * @param file
	 * @param filePath
	 * @return
	 */
	public static boolean createFilePath(File file, String filePath) {
		int index = filePath.indexOf("/");
		if (index == -1) {
			return false;
		}
		if (index == 0) {
			filePath = filePath.substring(index + 1, filePath.length());
			index = filePath.indexOf("/");
		}
		String path = filePath.substring(0, index);
		File fPath;
		if (file == null) {
			fPath = new File(path);
		} else {
			fPath = new File(file.getPath() + "/" + path);
		}
		if (!fPath.exists()) {
			if (!fPath.mkdir()) // SD卡已满无法在下载文件
			{
				return false;
			}
		}
		if (index < (filePath.length() - 1)) {
			String exPath = filePath.substring(index + 1, filePath.length());
			createFilePath(fPath, exPath);
		}
		return true;

	}
	
	/**
	 * 生成抓图路径或录像存放路径
	 * 
	 */
	public static String getCaptureAndVideoPath(DHFilesType type, String cameraName) {
		String path = null;
		String picType = null;
		java.util.Date now = new java.util.Date();
		SimpleDateFormat tf = new SimpleDateFormat("yyyyMMddHHmmss");
		String sdPath = Environment.getExternalStorageDirectory().getPath();
		if(type == DHFilesType.DHImage){
			picType = "image";
			path = sdPath + "/" + ProjectName + "/" + tf.format(now) + "_" + picType + "_"
					+ cameraName + ".jpg";
		} else{
			picType = "video";
			path = sdPath + "/" + ProjectName + "/" + tf.format(now) + "_" + picType + "_"
					+ cameraName + ".mp4";
		}
		createFilePath(null, path);
		return path;
	}
	
	/**
	 * 生成下载录像存放路径
	 * 
	 */
	public static String getDownloadVideoPath(int type, String recordID, long startTime) {
		String path = null;
		String sdPath = Environment.getExternalStorageDirectory().getPath();
			String picType = "download";
			picType += type==0 ? "_cloud" : "_remote";

			java.util.Date now = new java.util.Date(startTime);
			SimpleDateFormat tf = new SimpleDateFormat("yyyyMMddHHmmss");
			path = sdPath + "/" + ProjectName + "/" + tf.format(now) + "_" + picType + "_"
					+ recordID + ".mp4";
		createFilePath(null, path);
		return path;
	}
	
	/**
	 * 删除下载录像存放的录像
	 * 
	 */
	public static void deleteDownloadVideo(String recordID, long startTime) {
		String path = null;
		String sdPath = Environment.getExternalStorageDirectory().getPath();
			String picType = "download";
			java.util.Date now = new java.util.Date(startTime);
			SimpleDateFormat tf = new SimpleDateFormat("yyyyMMddHHmmss");
			path = sdPath + "/" + ProjectName + "/" + tf.format(now) + "_" + picType + "_"
					+ recordID + ".mp4";
		
			File soFile = new File(path);
			if (soFile.exists()) {
				soFile.delete();
			}
		
	}	

}
