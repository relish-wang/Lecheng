package com.lechange.demo.business.entity;


public class AlarmMessageInfo {
	
	private String chnlUuid;		// 通道的uuid

	/** [long]消息ID */
	private long alarmId;
	
	/** [int]报警类型 */
	private int type;
	
	/** 设备或通道的名称 */
	private String name = "";
	
	/** ***LLLJHHDF */
	private String thumbUrl = "";
	private String picUrl = "";
	
	/** 报警时设备本地时间，格式如2014-12-12 12:12:12 */
	private String localDate = "";
	
	private String deviceId;  //设备ID
	private String deviceKey; //设备秘钥
	
	/** [long]报警时间UNIX时间戳秒 */
	private long time;


	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public long getAlarmId() {
		return alarmId;
	}

	public void setAlarmId(long alarmId) {
		this.alarmId = alarmId;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPicUrl() {
		return picUrl;
	}

	public void setPicUrl(String picUrl) {
		this.picUrl = picUrl;
	}

	public String getThumbUrl() {
		return thumbUrl;
	}

	public void setThumbUrl(String thumbUrl) {
		this.thumbUrl = thumbUrl;
	}
	
	public String getLocalDate() {
		return localDate;
	}

	public void setLocalDate(String localDate) {
		this.localDate = localDate;
	}



	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getChnlUuid() {
		return chnlUuid;
	}

	public void setChnlUuid(String chnlUuid) {
		this.chnlUuid = chnlUuid;
	}

	public String getDeviceKey() {
		return deviceKey;
	}

	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}
	
	
}
