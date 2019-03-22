package com.lechange.demo.business.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeHelper {
	
	public static String getDateHMS(long time) {
		SimpleDateFormat format = new SimpleDateFormat("yy/MM/dd HH:mm:ss");
		Date date = new Date(time);
		String strdate = format.format(date);
		return strdate;// 2012-10-03 23:41:31
	}
	
	public static String getTimeHMS(long time) {
		SimpleDateFormat format1 = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date(time);
		String date1 = format1.format(date);
		return date1;// 2012-10-03 23:41:31
	}
	
	public static String getDateEN(long time) {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	
		Date date = new Date(time);
		String date1 = format1.format(date);
		return date1;// 2012-10-03 23:41:31
	}

	public static long getTimeStamp(String time) {
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = format1.parse(time);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		if (date == null) {
			return -1;
		}	
		return date.getTime();
	}
}
