package com.lechange.demo.business.entity;

import java.util.UUID;

public class RecordInfo {
	
	public enum RecordType
	{
		DeviceLocal,			// 设备本地录像
		PrivateCloud,			// 私有云
		PublicCloud,			// 公有云
	}
	
	public enum RecordEventType
	{
		All,			// 所有录像
		Manual,			// 手动录像
		Event,			// 事件录像
	}

	private String id = UUID.randomUUID().toString();
	private RecordType type;		// 录像类型
	private float fileLength;		// 文件长度
	private float downLength = -1;	// 已下载长度
	private long startTime;			// 开始时间
	private long endTime;			// 结束时间
	private String deviceId;  		//设备ID
	private String deviceKey;
	private String backgroudImgUrl;	// 录像文件Url
	private String chnlUuid;		// 通道的uuid
	private RecordEventType eventType;	// 事件类型
	private String recordID;			//录像ID
	private String recordPath;		//录像ID(设备录像)
	
    
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getDeviceKey() {
		return deviceKey;
	}
	public void setDeviceKey(String deviceKey) {
		this.deviceKey = deviceKey;
	}
	
	public RecordType getType() {
		return type;
	}
	public void setType(RecordType type) {
		this.type = type;
	}
	public long getStartTime() {
		return startTime;
	}
	public float getFileLength() {
		return fileLength;
	}
	public void setFileLength(float fileLength) {
		this.fileLength = fileLength;
	}
	public float getDownLength() {
		return downLength;
	}
	public void setDownLength(float downLength) {
		this.downLength = downLength;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	public String getBackgroudImgUrl() {
		return backgroudImgUrl;
	}
	public void setBackgroudImgUrl(String backgroudImgUrl) {
		this.backgroudImgUrl = backgroudImgUrl;
	}
	public String getChnlUuid() {
		return chnlUuid;
	}
	public void setChnlUuid(String chnlUuid) {
		this.chnlUuid = chnlUuid;
	}
	public RecordEventType getEventType() {
		return eventType;
	}
	public void setEventType(RecordEventType eventType) {
		this.eventType = eventType;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setRecordID(String id) {
		this.recordID = id;
	}
	public String getRecordID() {
		return this.recordID;
	}
	
	public boolean isDownload() {
		return downLength >= 0;
	}
    public String getRecordPath() {
		return recordPath;
	}
	public void setRecordPath(String recordPath) {
		this.recordPath = recordPath;
	}
}
