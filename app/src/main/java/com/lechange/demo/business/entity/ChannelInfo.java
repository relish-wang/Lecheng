package com.lechange.demo.business.entity;

import java.util.UUID;

public class ChannelInfo {
	
	public enum ChannelState
	{
		Online,				// 在线
		Offline,			// 离线
		Upgrade,			// 升级中
	}
	
	private String id = UUID.randomUUID().toString();
	private int index;					// 通道索引
	private String name;				// 通道名称
	private ChannelState state;			// 通道状态
	private String backgroudImgURL; 	// 背景图URL
	private String deviceCode;			// 关联的设备id
	private String deviceModel;			// 关联的设备型号
	private String encryptKey;			// 秘钥的key
	private int encryptMode;         // 秘钥模式:0-默认模式，1-自定义模式
	private int encrypt;				// 是否支持加密
	private int ability;

	private int cloudMealStates;        //云套餐状态
	private int alarmStatus;            //动检计划状态
	
	/**
	 * 设备能力集合
	 * @author 23930
	 *
	 */
	public class Ability {
		public static final int WLAN = 1; // 设备支持接入无线局域网 
		public static final int AlarmPIR = 2;// 设备支持人体红外报警 
		public static final int AlarmMD = 4; // 设备支持动检报警 
		public static final int AudioTalk = 8; // 设备支持语音对讲
		public static final int VVP2P = 16; // 设备支持威威网络P2P服务 
		public static final int DHP2P = 32; // 设备支持大华P2P服务
		public static final int PTZ = 64; //设备支持云台操作
		public static final int HSEncrypt = 128; // 设备支持华视微讯码流加密
		public static final int CloudStorage = 256; // 设备支持华视微讯平台云存储
	}
	

	public int getAlarmStatus() {
		return alarmStatus;
	}
	public void setAlarmStatus(int alarmStatus) {
		this.alarmStatus = alarmStatus;
	}
	public int getCloudMealStates() {
		return cloudMealStates;
	}
	public void setCloudMealStates(int cloudMealStates) {
		this.cloudMealStates = cloudMealStates;
	}
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public ChannelState getState() {
		return state;
	}
	public void setState(ChannelState state) {
		this.state = state;
	}
	public String getBackgroudImgURL() {
		return backgroudImgURL;
	}
	public void setBackgroudImgURL(String backgroudImgURL) {
		this.backgroudImgURL = backgroudImgURL;
	}
	public String getDeviceCode() {
		return deviceCode;
	}
	public void setDeviceCode(String deviceCode) {
		this.deviceCode = deviceCode;
	}
	public String getDeviceModel() {
		return deviceModel;
	}
	public void setDeviceModel(String deviceModel) {
		this.deviceModel = deviceModel;
	}
	public int getEncrypt() {
		return encrypt;
	}
	public void setEncrypt(int encrypt) {
		this.encrypt = encrypt;
	}
	public String getUuid() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getAbility() {
		return ability;
	}
	public void setAbility(int ability) {
		this.ability = ability;
	}
	public int getEncryptMode() {
		return encryptMode;
	}
	public void setEncryptMode(int encryptMode) {
		this.encryptMode = encryptMode;
	}
	public String getEncryptKey() {
		return encryptKey;
	}
	public void setEncryptKey(String encryptKey) {
		this.encryptKey = encryptKey;
	}
}
