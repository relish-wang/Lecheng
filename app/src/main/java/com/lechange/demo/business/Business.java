package com.lechange.demo.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.R.integer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Keep;
import android.util.Log;

import com.company.NetSDK.FinalVar;
import com.lechange.common.log.Logger;
import com.lechange.demo.business.entity.AlarmMessageInfo;
import com.lechange.demo.business.entity.ChannelInfo;
import com.lechange.demo.business.entity.ChannelPTZInfo;
import com.lechange.demo.business.entity.ChannelPTZInfo.Direction;
import com.lechange.demo.business.entity.ChannelPTZInfo.Duration;
import com.lechange.demo.business.entity.ChannelPTZInfo.Operation;
import com.lechange.demo.business.entity.RecordInfo;
import com.lechange.demo.business.entity.RecordInfo.RecordEventType;
import com.lechange.demo.business.entity.RecordInfo.RecordType;
import com.lechange.demo.business.util.OpenApiHelper;
import com.lechange.demo.business.util.TaskPoolHelper;
import com.lechange.demo.business.util.TaskPoolHelper.RunnableTask;
import com.lechange.demo.business.util.TimeHelper;
import com.lechange.opensdk.api.LCOpenSDK_Api;
import com.lechange.opensdk.api.bean.AddRole;
import com.lechange.opensdk.api.bean.BeAuthDeviceList;
import com.lechange.opensdk.api.bean.BindDevice;
import com.lechange.opensdk.api.bean.BindDeviceInfo;
import com.lechange.opensdk.api.bean.CheckDeviceBindOrNot;
import com.lechange.opensdk.api.bean.ControlDeviceWifi;
import com.lechange.opensdk.api.bean.ControlPTZ;
import com.lechange.opensdk.api.bean.DeleteAlarmMessage;
import com.lechange.opensdk.api.bean.DeviceAlarmPlan;
import com.lechange.opensdk.api.bean.DeviceAlarmPlan.ResponseData.RulesElement;
import com.lechange.opensdk.api.bean.DeviceList;
import com.lechange.opensdk.api.bean.DeviceList.Response;
import com.lechange.opensdk.api.bean.DeviceList.ResponseData.DevicesElement;
import com.lechange.opensdk.api.bean.DeviceList.ResponseData.DevicesElement.ChannelsElement;
import com.lechange.opensdk.api.bean.DeviceOnline;
import com.lechange.opensdk.api.bean.GetAlarmMessage;
import com.lechange.opensdk.api.bean.GetAlarmMessage.ResponseData.AlarmsElement;
import com.lechange.opensdk.api.bean.GetAuthFunctions;
import com.lechange.opensdk.api.bean.ModifyDeviceAlarmPlan;
import com.lechange.opensdk.api.bean.ModifyDeviceAlarmStatus;
import com.lechange.opensdk.api.bean.ModifyDevicePwd;
import com.lechange.opensdk.api.bean.QueryCloudRecordNum;
import com.lechange.opensdk.api.bean.QueryCloudRecords;
import com.lechange.opensdk.api.bean.QueryCloudRecords.ResponseData.RecordsElement;
import com.lechange.opensdk.api.bean.QueryLocalRecordNum;
import com.lechange.opensdk.api.bean.QueryLocalRecords;
import com.lechange.opensdk.api.bean.SetStorageStrategy;
import com.lechange.opensdk.api.bean.SetUserAuthTager;import com.lechange.opensdk.api.bean.ShareDeviceList;
import com.lechange.opensdk.api.bean.UnBindDevice;
import com.lechange.opensdk.api.bean.UnBindDeviceInfo;
import com.lechange.opensdk.api.bean.UpgradeDevice;
import com.lechange.opensdk.api.bean.WifiAround;
import com.lechange.opensdk.api.client.BaseRequest;
import com.lechange.opensdk.api.client.BaseResponse;
import com.lechange.opensdk.device.LCOpenSDK_DeviceInit;
import com.lechange.opensdk.media.DeviceInitInfo;
import com.lechange.opensdk.media.LCOpenSDK_LoginManager;

public class Business {

	private static class Instance {
		static Business instance = new Business();
	}

	public static Business getInstance() {
		return Instance.instance;
	}
	
	public static final boolean isOversea = false;

	// hls,rtsp等相关底层库的错误码解析
	public static final int RESULT_SOURCE_OPENAPI = 99; // 在播放过程中rest请求回调类型

	public static class CloudStorageCode {
		public static final String HLS_DOWNLOAD_FAILD = "0"; // 下载失败
		public static final String HLS_DOWNLOAD_BEGIN = "1"; // 开始下载
		public static final String HLS_DOWNLOAD_END = "2"; // 下载结束
		public static final String HLS_SEEK_SUCCESS = "3"; // 定位成功
		public static final String HLS_SEEK_FAILD = "4"; // 定位失败
		// public static final String HLS_ABORT_DONE = "5";
		// public static final String HLS_RESUME_DONE = "6";
		public static final String HLS_KEY_ERROR = "11"; // 秘钥错误
	}
	
	public static class LocalDownloadCode{
		public static final String RTSP_DOWNLOAD_FRAME_ERROR = "0";
		public static final String RTSP_DOWNLOAD_TEARDOWN = "1";
		public static final String RTSP_DOWNLOAD_AUTHORIZATION_FAIL = "3";
		public static final String RTSP_DOWNLOAD_BEGIN = "4";
		public static final String RTSP_DOWNLOAD_OVER = "5";
		public static final String RTSP_DOWNLOAD_PAUSE = "6";
		public static final String RTSP_DOWNLOAD_KEY_MISMATH = "7";
	}

	public static class PlayerResultCode {
		public static final String STATE_PACKET_FRAME_ERROR = "0"; // 组帧失败
		public static final String STATE_RTSP_TEARDOWN_ERROR = "1"; // 内部要求关闭,如连接断开等
		public static final String STATE_RTSP_DESCRIBE_READY = "2"; // 会话已经收到Describe响应
		public static final String STATE_RTSP_AUTHORIZATION_FAIL = "3"; // RTSP鉴权失败
		public static final String STATE_RTSP_PLAY_READY = "4"; // 收到PLAY响应
		// public static final String STATE_RTSP_FILE_PLAY_OVER = "5"; //
		// 录像文件回放正常结束
		public static final String STATE_RTSP_KEY_MISMATCH = "7";
	}

	public final static String tag = "Business";
	// 码流超时时间
	public final static int DMS_TIMEOUT = 60 * 1000;

	private List<ChannelInfo> mChannelInfoList = new ArrayList<ChannelInfo>(); // 由于demo，不考虑多线程操作问题
	private List<ChannelInfo> mSharedChannelInfoList = new ArrayList<ChannelInfo>(); // 由于demo，不考虑多线程操作问题
	private List<ChannelInfo> mBeAuthChannelInfoList = new ArrayList<ChannelInfo>(); // 由于demo，不考虑多线程操作问题
	private List<RecordInfo> mRecordInfoList = new ArrayList<RecordInfo>(); // 由于demo，不考虑多线程操作问题
	private List<AlarmMessageInfo> mAlarmMessageInfoList = new ArrayList<AlarmMessageInfo>();
	private String mHost;
	private String mAppId;
	private String mAppSecret;

	// private boolean bInit = false;

	public final class HttpCode {
		public static final int SC_OK = 200;// OK
											// （API调用成功，但是具体返回结果，由content中的code和desc描述）
		public static final int Bad_Request = 400;// Bad Request （API格式错误，无返回内容）
		public static final int Unauthorized = 401;// Unauthorized
													// （用户名密码认证失败，无返回内容）
		public static final int Forbidden = 403;// Forbidden （认证成功但是无权限，无返回内容）
		public static final int Not_Found = 404;// Not Found （请求的URI错误，无返回内容）
		public static final int Precondition_Failed = 412;// Precondition Failed
															// （先决条件失败，无返回内容。通常是由于客户端所带的x-hs-date时间与服务器时间相差过大。）
		public static final int Internal_Server_Error = 500;// Internal Server
															// Error
															// （服务器内部错误，无返回内容）
		public static final int Service_Unavailable = 503;// Service Unavailable
															// （服务不可用，无返回内容。这种情况一般是因为接口调用超出频率限制。）
	}

	private String mToken = ""; // userToken或accessToken

	public static class RetObject {
		public int mErrorCode = 0; // 错误码表示符 -1:返回体为null，0：成功，1：http错误， 2：业务错误
		public String mMsg;
		public Object resp;
	}

	/**
	 * 初始化client对象，配置client参数
	 * 
	 * @return
	 */
	public boolean init(String appid, String appsecret, String host) {
		// 设置乐橙服务地址
		LCOpenSDK_Api.setHost(host);
		mAppId = appid;
		mAppSecret = appsecret;
		mHost = host;

		// bInit = true;
		Log.d(tag, "Init OK");
		// }
		return true;
	}

	/**
	 * 发送网络请求，并对请求结果的错误码进行处理
	 * 
	 * @param req
	 * @return
	 */
	private RetObject request(BaseRequest req) {
		return request(req, 15 * 1000);
	}

	/**
	 * 发送网络请求，并对请求结果的错误码进行处理
	 * 
	 * @param req
	 * @param timeOut
	 *            访问dms接口时，超时时间设置长一点
	 * @return
	 * @throws Exception
	 */
	private RetObject request(BaseRequest req, int timeOut) {
		// T t = LCOpenSDK_Api.request(req, timeOut);
		RetObject retObject = new RetObject();
		BaseResponse t = null;
		try {
			t = LCOpenSDK_Api.request(req, timeOut);
			// Log.d(tag, req.getBody());

			if (t.getCode() == HttpCode.SC_OK) {
				// 请求成功，则看服务器处理错误
				if (!t.getApiRetCode().equals("0"))
					retObject.mErrorCode = 2; // 业务错误
				retObject.mMsg = "business errorcode: " + t.getApiRetCode()
						+ ", error msg: " + t.getApiRetMsg();
			} else {
				retObject.mErrorCode = 1; // http错误
				retObject.mMsg = "HTTP errorcode: " + t.getCode()
						+ ", error msg: " + t.getDesc();
			}
		} catch (Exception e) {
			// if timeout,return;
			e.printStackTrace();
			retObject.mErrorCode = -1000;
			retObject.mMsg = "inner errorcode : -1000, error msg: "
					+ e.getMessage();
			Log.d(tag, req.getBody() + retObject.mMsg);
		}
		retObject.resp = t;
		return retObject;
	}

	public String getHost() {
		return mHost;
	}

	public void setToken(String mToken) {
		this.mToken = mToken;
	}

	public String getToken() {
		return mToken;
	}

	/**
	 * 管理员登陆设备
	 */
	public void adminlogin(final Handler handler) {
		// 请求AccessToken 以开发者手机号
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
//				 AccessToken req = new AccessToken();
//				 req.data.phone = ""; // 唯一标记一类app
//				 //AccessToken.Response resp= null;
//				 RetObject retObject = null;
//				 retObject = request(req);
//				 //将标示符和返回体发送给handle处理
//				 //retobject.resp = resp;
//				 if(retObject.resp != null && ((AccessToken.Response)retObject.resp).data != null)
//					 handler.obtainMessage(retObject.mErrorCode, ((AccessToken.Response)retObject.resp).data.accessToken).sendToTarget();

				// 实现一个443连接供上层参考
				// 3.0.1不需要管理员账号，在此屏蔽
				OpenApiHelper.getAccessToken(mHost, "", mAppId, mAppSecret, handler);
			}
		});
	}

	/**
	 * 获取验证码
	 * 
	 * @param phoneNumber
	 * @param mAppid
	 * @param mAppSecret
	 * @param handler
	 * @param context
	 */
	public void getUserSms(final String phoneNumber, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// 实现一个443连接供上层参考
				OpenApiHelper.userBindSms(mHost, phoneNumber, mAppId,
						mAppSecret, handler);
			}
		});

	}

	/**
	 * 验证验证码
	 * 
	 * @param phoneNumber
	 * @param smsCode
	 * @param handler
	 * @param context
	 */
	public void checkUserSms(final String phoneNumber, final String smsCode,
			final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// 实现一个443连接供上层参考
				OpenApiHelper.userBind(mHost, phoneNumber, mAppId, mAppSecret,
						smsCode, handler);
			}
		});
	}

	/**
	 * 用户登陆设备
	 */
	public void userlogin(final String phoneNumber, final Handler handler) {
		// 请求usersToken 以开发者手机号
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// 实现一个443连接供上层参考
				OpenApiHelper.getUserToken(mHost, phoneNumber, mAppId,
						mAppSecret, handler);
			}
		});
	}

	/**
	 * 从检索出的设备列表中查找制定设备通道
	 * 
	 * @param id
	 * @return
	 */
	public ChannelInfo getChannel(String id) {

		for (ChannelInfo chl : mChannelInfoList) {
			if (chl.getUuid().equals(id)) {
				return chl;
			}
		}
		for (ChannelInfo chl : mSharedChannelInfoList) {
			if (chl.getUuid().equals(id)) {
				return chl;
			}
		}
		for (ChannelInfo chl : mBeAuthChannelInfoList) {
			if (chl.getUuid().equals(id)) {
				return chl;
			}
		}
		return null;
	}

	/**
	 * 获取用户下面所有设备通道列表
	 * 
	 * @param handler
	 */
	public void getChannelList(final Handler handler) {
		mChannelInfoList.clear(); // 清数据
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				DeviceList req = new DeviceList();
				// req.data.filter = new ArrayList<String>(); //
				// 过滤条件，可指定获取特定设备序列号的设备信息，我们这获取用户所有设备，无过滤条件。
				req.data.token = mToken; // token
				req.data.queryRange = "1-10";
				DeviceList.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);
				resp = (Response) retObject.resp;
				mChannelInfoList.clear(); // 防止list崩溃
				if (resp != null && resp.data != null
						&& resp.data.devices != null)
					for (DevicesElement devElement : resp.data.devices) {
						List<ChannelInfo> list = devicesElementToResult(
								devElement, null);
						mChannelInfoList.addAll(list);
					}
				retObject.resp = mChannelInfoList;
				// 解析设备列表信息
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面
			}
		});
	}

	/**
	 * 获取分享设备列表
	 * 
	 * @param handler
	 */
	public void getSharedDeviceList(final Handler handler) {

		mSharedChannelInfoList.clear(); // 清数据
		TaskPoolHelper.addTask(new RunnableTask("Shared") {
			@Override
			public void run() {
				// TODO Auto-generated method stub

				ShareDeviceList req = new ShareDeviceList();
				req.data.queryRange = "1-10";
				req.data.token = mToken;

				ShareDeviceList.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);
				resp = (ShareDeviceList.Response) retObject.resp;
				mSharedChannelInfoList.clear(); // 防止list崩溃
				if (resp != null && resp.data != null
						&& resp.data.devices != null) {
					for (ShareDeviceList.ResponseData.DevicesElement devElement : resp.data.devices) {
						List<ChannelInfo> list = devicesElementToResult(null,
								devElement);
						mSharedChannelInfoList.addAll(list);
					}

				}

				retObject.resp = mSharedChannelInfoList;

				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面

			}
		});
	}

	/**
	 * 获取授权设备列表
	 * 
	 * @param handler
	 */
	public void getBeAuthDeviceList(final Handler handler) {

		mBeAuthChannelInfoList.clear(); // 清数据
		TaskPoolHelper.addTask(new RunnableTask("BeAuth") {
			@Override
			public void run() {
				// TODO Auto-generated method stub

				BeAuthDeviceList req = new BeAuthDeviceList();
				req.data.queryRange = "1-10";
				req.data.token = mToken;

				BeAuthDeviceList.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);
				resp = (BeAuthDeviceList.Response) retObject.resp;
				mBeAuthChannelInfoList.clear(); // 防止list崩溃
				// list = null;
				if (resp != null && resp.data != null
						&& resp.data.devices != null) {

					// list = new ArrayList<ChannelInfo>();
					for (BeAuthDeviceList.ResponseData.DevicesElement devElement : resp.data.devices) {
						ChannelInfo channelInfo = devicesElementToResult(devElement);
						// List<ChannelInfo> list.add(channelInfo);
						mBeAuthChannelInfoList.add(channelInfo);
					}
				}
				retObject.resp = mBeAuthChannelInfoList;

				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面

			}
		});
	}

	/**
	 * 获取用户设备信息
	 * 
	 * @param handler
	 */
	public void getDeviceInfo(final String chnlId, final Handler handler) {

		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				BindDeviceInfo req = new BindDeviceInfo();

				req.data.token = mToken; // token
				req.data.deviceId = chl.getDeviceCode();
				BindDeviceInfo.Response resp = null;

				RetObject retObject = null;
				retObject = request(req);
				resp = (BindDeviceInfo.Response) retObject.resp;

				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "getDeviceInfo faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null || resp.data.channels == null) {
						Log.d(tag, "getDeviceInfo success data is null");
					}
					Bundle bundle = new Bundle();
					bundle.putInt("alarmStatus",
							resp.data.channels.get(0).alarmStatus);
					// bundle.putInt("alarmStatus", 0);
					bundle.putInt("cloudStatus",
							resp.data.channels.get(0).csStatus);
					bundle.putBoolean("canBeUpgrade", resp.data.canBeUpgrade);
					// todo:其他信息解析请见OpenAPI 平台sdk文档
					handler.obtainMessage(0, bundle).sendToTarget(); // 发送成功消息到界面
				}
			}

		});
	}

	private ChannelInfo devicesElementToResult(
			BeAuthDeviceList.ResponseData.DevicesElement beAuthDevElement) {
		ChannelInfo chnlInfo = new ChannelInfo();
		// 解析分享设备通道列表
		if (beAuthDevElement != null) {

			chnlInfo.setDeviceCode(beAuthDevElement.deviceId);
			chnlInfo.setDeviceModel(beAuthDevElement.deviceModel);
			chnlInfo.setEncryptMode(beAuthDevElement.encryptMode);
			chnlInfo.setIndex(beAuthDevElement.channelId);
			chnlInfo.setName(beAuthDevElement.channelName + "[beAuth]");
			chnlInfo.setBackgroudImgURL(beAuthDevElement.channelPicUrl);
			chnlInfo.setCloudMealStates(beAuthDevElement.csStatus);
			chnlInfo.setAlarmStatus(beAuthDevElement.alarmStatus);
			// // 是否为云台设备
			// if ((chnlElement.channelAbility != null
			// && chnlElement.channelAbility.contains("PTZ"))
			// || devElement.ability.contains("PTZ")) {
			// chnlInfo.setType(ChannelType.PtzCamera);
			// }
			// else {
			// chnlInfo.setType(ChannelType.Camera);
			// }
			// 是否支持加密
			chnlInfo.setEncryptMode(beAuthDevElement.encryptMode);
			// 设置设备能力集
			chnlInfo.setAbility(StringToAbility(beAuthDevElement.ability));

			// 设备与通道同时在线，通道才算在线
			if (beAuthDevElement.channelOnline)
				switch (beAuthDevElement.status) {
				case 0:
					chnlInfo.setState(ChannelInfo.ChannelState.Offline);
					break;
				case 1:
					chnlInfo.setState(ChannelInfo.ChannelState.Online);
					break;
				case 3:
					chnlInfo.setState(ChannelInfo.ChannelState.Upgrade);
					break;
				}

		}

		return chnlInfo;

	}

	/**
	 * 一个设备下面有多个通道，目前demo不需要太多设备信息，故没有解析设备信息，但设备信息解析可参照下面代码，或者参照OpenAPI sdk文档。
	 * 
	 * @param devElement
	 * @return
	 */
	private List<ChannelInfo> devicesElementToResult(
			DeviceList.ResponseData.DevicesElement devElement,
			ShareDeviceList.ResponseData.DevicesElement shareDevElement) {

		List<ChannelInfo> channelList = new ArrayList<ChannelInfo>();

		// 解析绑定通道列表
		if (devElement != null && devElement.channels != null) {
			for (DeviceList.ResponseData.DevicesElement.ChannelsElement chnlElement : devElement.channels) {
				ChannelInfo chnlInfo = new ChannelInfo();
				chnlInfo.setDeviceCode(devElement.deviceId);
				chnlInfo.setDeviceModel(devElement.deviceModel);
				chnlInfo.setEncryptMode(devElement.encryptMode);
				chnlInfo.setIndex(chnlElement.channelId);
				chnlInfo.setName(chnlElement.channelName);
				chnlInfo.setBackgroudImgURL(chnlElement.channelPicUrl);
				chnlInfo.setCloudMealStates(chnlElement.csStatus);
				chnlInfo.setAlarmStatus(chnlElement.alarmStatus);
				// // 是否为云台设备
				// if ((chnlElement.channelAbility != null
				// && chnlElement.channelAbility.contains("PTZ"))
				// || devElement.ability.contains("PTZ")) {
				// chnlInfo.setType(ChannelType.PtzCamera);
				// }
				// else {
				// chnlInfo.setType(ChannelType.Camera);
				// }
				// 是否支持加密
				if (devElement.ability.contains("HSEncrypt")) {
					chnlInfo.setEncrypt(1);
				} else {
					chnlInfo.setEncrypt(0);
				}
				// 设置设备能力集
				chnlInfo.setAbility(StringToAbility(devElement.ability) | StringToAbility(chnlElement.channelAbility));

				// 设备与通道同时在线，通道才算在线
				if (chnlElement.channelOnline)
					switch (devElement.status) {
					case 0:
						chnlInfo.setState(ChannelInfo.ChannelState.Offline);
						break;
					case 1:
						chnlInfo.setState(ChannelInfo.ChannelState.Online);
						break;
					case 3:
						chnlInfo.setState(ChannelInfo.ChannelState.Upgrade);
						break;
					}
				channelList.add(chnlInfo);
			}
		}

		// 解析分享设备通道列表
		if (shareDevElement != null && shareDevElement.channels != null) {
			for (ShareDeviceList.ResponseData.DevicesElement.ChannelsElement chnlElement : shareDevElement.channels) {
				ChannelInfo chnlInfo = new ChannelInfo();
				chnlInfo.setDeviceCode(shareDevElement.deviceId);
				chnlInfo.setDeviceModel(shareDevElement.deviceModel);
				chnlInfo.setEncryptMode(shareDevElement.encryptMode);
				chnlInfo.setIndex(chnlElement.channelId);
				chnlInfo.setName(chnlElement.channelName + "[shared]");
				chnlInfo.setBackgroudImgURL(chnlElement.channelPicUrl);
				chnlInfo.setAlarmStatus(chnlElement.alarmStatus);
				// // 是否为云台设备
				// if ((chnlElement.channelAbility != null
				// && chnlElement.channelAbility.contains("PTZ"))
				// || devElement.ability.contains("PTZ")) {
				// chnlInfo.setType(ChannelType.PtzCamera);
				// }
				// else {
				// chnlInfo.setType(ChannelType.Camera);
				// }
				// 是否支持加密
				if (shareDevElement.ability.contains("HSEncrypt")) {
					chnlInfo.setEncrypt(1);
				} else {
					chnlInfo.setEncrypt(0);
				}
				// 设置设备能力集
				chnlInfo.setAbility(StringToAbility(shareDevElement.ability));

				// 设备与通道同时在线，通道才算在线
				if (chnlElement.channelOnline)
					switch (shareDevElement.status) {
					case 0:
						chnlInfo.setState(ChannelInfo.ChannelState.Offline);
						break;
					case 1:
						chnlInfo.setState(ChannelInfo.ChannelState.Online);
						break;
					case 3:
						chnlInfo.setState(ChannelInfo.ChannelState.Upgrade);
						break;
					}
				channelList.add(chnlInfo);
			}
		}
		return channelList;
	}

	/**
	 * 设备能力集转换
	 * 
	 * @param strAbility
	 * @return
	 */
	public static int StringToAbility(String strAbility) {
		int ability = 0;
		if (strAbility == null) {
			return ability;
		}

		if (strAbility.contains("WLAN")) {
			ability |= ChannelInfo.Ability.WLAN;
		}
		if (strAbility.contains("AlarmPIR")) {
			ability |= ChannelInfo.Ability.AlarmPIR;
		}
		if (strAbility.contains("AudioTalk")) {
			ability |= ChannelInfo.Ability.AudioTalk;
		}
		if (strAbility.contains("VVP2P")) {
			ability |= ChannelInfo.Ability.VVP2P;
		}
		if (strAbility.contains("PTZ")) {
			ability |= ChannelInfo.Ability.PTZ;
		}
		if (strAbility.contains("HSEncrypt")) {
			ability |= ChannelInfo.Ability.HSEncrypt;
		}
		if (strAbility.contains("CloudStorage")) {
			ability |= ChannelInfo.Ability.CloudStorage;
		}
		return ability;
	}

	public void checkBindOrNot(final String deviceId, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				CheckDeviceBindOrNot req = new CheckDeviceBindOrNot();
				req.data.token = mToken;
				req.data.deviceId = deviceId; // 设备id。
				// CheckDeviceBindOrNot.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);

				// 将标示符和返回体发送给handle处理
				// retobject.resp = resp;
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget();
			}
		});
	}

	/**
	 * 设备在线状态
	 * 
	 * @param deviceId
	 * @param handler
	 */
	public void checkOnline(final String deviceId, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				DeviceOnline req = new DeviceOnline();
				req.data.token = mToken;
				req.data.deviceId = deviceId; // 设备id。
				RetObject retObject = null;
				retObject = request(req);

				// 将标示符和返回体发送给handle处理
				// retobject.resp = resp;
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget();
			}
		});
	}

	public void unBindDeviceInfo(final String deviceID, final Handler handler){
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				UnBindDeviceInfo req = new UnBindDeviceInfo();
				req.data.token = mToken;
				req.data.deviceId = deviceID; // 设备id。
				RetObject retObject = null;
				retObject = request(req);
				UnBindDeviceInfo.Response resp = (UnBindDeviceInfo.Response)retObject.resp;
				if(resp.data == null || resp.data.ability == null){
					Log.e(tag,"unBindDeviceInfo response data is null");
					handler.obtainMessage(1000, "unBindDeviceInfo response data is null").sendToTarget();
					return;
				}
					
				retObject.resp = resp.data.ability;
				// 将标示符和返回体发送给handle处理
				handler.obtainMessage(retObject.mErrorCode, retObject.resp).sendToTarget();
				
			}
		});
	}
	/**
	 * 为账号绑定设备
	 * 
	 * @param deviceID
	 *            序列号
	 * @param handler
	 */
	public void bindDevice(final String deviceID, final String key,
			final Handler handler) {
		bindDevice(deviceID, key, key, handler);
	}

	public void bindDevice(final String deviceID, final String code,
			final String key, final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				BindDevice req = new BindDevice();
				req.data.token = mToken;
				req.data.deviceId = deviceID;
				req.data.code = code;

				// BindDevice.Response resp = null;
				RetObject retObject = null;
				retObject = request(req, 5 * 1000);
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget();
			}
		});
	}

	/**
	 * 为账号解绑设备
	 * 
	 * @param deleteCloudRecords
	 *            是否删除云录像
	 * @param deviceID
	 *            序列号
	 * @param handler
	 */
	public void unBindDevice(final String deviceID, final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				UnBindDevice req = new UnBindDevice();
				req.data.token = mToken;
				req.data.deviceId = deviceID;
				// UnBindDevice.Response resp = null;
				RetObject retObject = null;
				retObject = request(req, 15 * 1000);
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget();
			}
		});
	}

	// 网络周边
	public void getWifiStatus(final String ssid, final String deviceID,
			final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				WifiAround req = new WifiAround();
				req.data.token = mToken;
				req.data.deviceId = deviceID;
				// WifiAround.Response resp = null;
				RetObject retObject = null;
				retObject = request(req, 45 * 1000);
				System.out.println("SSID:" + ssid);

				// 解析数据
				if (retObject.mErrorCode == 0) {
					// get device wifilist
					for (WifiAround.ResponseData.WLanElement element : ((WifiAround.Response) retObject.resp).data.wLan) {
						if (element.ssid.equals(ssid)) {
							System.out.println("obtainMessage:" + element.ssid);
							retObject.resp = element.bssid;
						}
					}
				}
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送消息到界面
			}
		});
	}

	// 连接网络
	public void connectWifi(final String token, final String BSSID, final String ssid,
			final String password, final String deviceID, final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				ControlDeviceWifi req = new ControlDeviceWifi();
				req.data.token = token;
				req.data.password = password;
				req.data.linkEnable = true;
				req.data.ssid = ssid;
				req.data.deviceId = deviceID;
				req.data.bssid = BSSID;
				// ControlDeviceWifi.Response resp = null;
				System.out.println(ssid + "--" + password + "--" + deviceID + "--" + token);
				RetObject retObject = null;
				retObject = request(req, 45 * 1000); // dms timeout
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送消息到界面
			}
		});
	}

	public void setAlarmPlanConfig(final String deviceID,
			final String optional, final List<RulesElement> rules,
			final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ModifyDeviceAlarmPlan req = new ModifyDeviceAlarmPlan();
				req.data.deviceId = deviceID;
				req.data.channelId = optional;

				// System.out.println("channels.get(0).rules.size:"+channels.get(0).rules.size());
				for (RulesElement ele : rules) {
					System.out.println(ele.beginTime);
					System.out.println(ele.endTime);
					System.out.println(ele.period);
				}
				RetObject retObject = null;
				retObject = request(req);
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面
			}
		});

	}

	/**
	 * 查询报警计划配置
	 * 
	 * @param deviceID
	 *            设备id
	 * @param optional
	 *            通道id
	 * @param handler
	 *            回调
	 */
	public void getAlarmPlanConfig(final String deviceID,
			final String optional, final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				DeviceAlarmPlan req = new DeviceAlarmPlan();
				req.data.deviceId = deviceID; // 过滤条件，可指定获取特定设备序列号的设备信息。
				req.data.channelId = optional; // 通道id
				DeviceAlarmPlan.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);

				// 网络请求失败
				if (resp == null || resp.data == null) {
					// throw new
					// BusinessException(BusinessErrorCode.BEC_COMMON_NULL_POINT);
				}

				resp = (DeviceAlarmPlan.Response) retObject.resp;
				// 如果设备在线
				if (resp.data != null) {
					retObject.resp = resp.data.rules;
				}
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送消息到界面
			}
		});

	}

	public void AsynControlPtz(String id, ChannelPTZInfo ptz,
			final Handler handler) {
		// TODO Auto-generated method stub
		String operation = "";
		String duration = null;
		int v = 0;
		int h = 0;
		double z = 1.0d;
		if (ptz.getOperation() == Operation.Move) {
			operation = "move";
			if (ptz.getDuration() == Duration.Forever) {
				duration = "last";
			} else if (ptz.getDuration() == Duration.Long) {
				duration = "last";
			} else if (ptz.getDuration() == Duration.Generral) {
				duration = "500";
			} else if (ptz.getDuration() == Duration.Short) {
				duration = "200";
			}

			if (ptz.getDirection() == Direction.Left) {
				h = -5;
			} else if (ptz.getDirection() == Direction.Right) {
				h = 5;
			} else if (ptz.getDirection() == Direction.Up) {
				v = 5;
			} else if (ptz.getDirection() == Direction.Down) {
				v = -5;
			} else if (ptz.getDirection() == Direction.ZoomIn) {
				z = 0.5d;
			} else if (ptz.getDirection() == Direction.ZoomOut) {
				z = 1.5d;
			}

		} else if (ptz.getOperation() == Operation.Locate) {
			operation = "locate";
		} else if (ptz.getOperation() == Operation.Stop) {
			operation = "move"; // stop 为自定义事件，协议为 operation设置move vhz的值设置0,0,1
			duration = "100"; // 随意填写 以防设备出错
			v = 0;
			h = 0;
			z = 1.0d;
		}

		final ChannelInfo chl = getChannel(id);
		if (chl == null) {
			Log.d(tag, "chl is null");
			return;
		}

		final ControlPTZ req = new ControlPTZ();
		req.data.token = mToken;
		req.data.operation = operation;
		req.data.v = v;
		req.data.duration = duration;
		req.data.h = h;
		req.data.z = z;
		req.data.channelId = String.valueOf(chl.getIndex());
		req.data.deviceId = chl.getDeviceCode();

		Log.d(tag, "id=" + id + "devId=" + chl.getDeviceCode());

		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// ControlPTZ.Response resp = null;
				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送消息到界面
			}
		});

	}

	/**
	 * 查询通道下的录像总数
	 * 
	 * @param chnlId
	 * @param chnl
	 * @param startTime
	 * @param endTime
	 * @param startIndex
	 * @param endIndex
	 * @param handler
	 *            -
	 */
	public void queryRecordNum(String chnlId, final String startTime,
			final String endTime, final Handler handler) {
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				QueryLocalRecordNum req = new QueryLocalRecordNum();
				req.data.token = mToken;
				req.data.beginTime = startTime;
				// req.data.type = "All"; 默认为all
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.endTime = endTime;
				req.data.deviceId = chl.getDeviceCode();
				Log.d(tag, "strStartTime:" + startTime + "strEndTime:"
						+ endTime);

				RetObject retObject = null;
				retObject = request(req, DMS_TIMEOUT);
				QueryLocalRecordNum.Response resp = (QueryLocalRecordNum.Response) retObject.resp;

				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "QueryLocalRecordNum faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null) {
						Log.d(tag, "QueryLocalRecordNum success data is null");
					}
					// todo:其他信息解析请见OpenAPI 平台sdk文档
					handler.obtainMessage(
							0,
							resp.data.recordNum,
							resp.data.recordNum > 10 ? resp.data.recordNum - 9
									: 1).sendToTarget(); // 发送成功消息到界面
				}
			}
		});
	}

	/**
	 * 查询通道下的录像列表
	 * 
	 * @param chnlId
	 * @param chnl
	 * @param startTime
	 * @param endTime
	 * @param startIndex
	 * @param endIndex
	 * @param handler
	 */
	public void queryRecordList(String chnlId, final String startTime,
			final String endTime, int startIndex, int endIndex,
			final Handler handler) {
		mRecordInfoList.clear(); // 清数据

		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}

		final String strNneed = String.valueOf(startIndex) + "-"
				+ String.valueOf(endIndex); // 需要录像的条数，从那条记录到那一条，demo只读取前面5条，不查询完所有录像

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {

				QueryLocalRecords req = new QueryLocalRecords();
				req.data.token = mToken;
				req.data.beginTime = startTime;
				// req.data.type = "All"; 默认为all
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.queryRange = strNneed;
				req.data.endTime = endTime;
				req.data.deviceId = chl.getDeviceCode();
				Log.d(tag, "strStartTime:" + startTime + "strEndTime:"
						+ endTime);

				RetObject retObject = null;
				retObject = request(req);
				QueryLocalRecords.Response resp = (QueryLocalRecords.Response) retObject.resp;

				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "QueryRecordList faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null || resp.data.records == null) {
						Log.d(tag, "queryRecordList success data is null");
						handler.obtainMessage(-1,"respone data is null").sendToTarget();
					}

					for (QueryLocalRecords.ResponseData.RecordsElement element : resp.data.records) {
						RecordInfo info = new RecordInfo();
						info.setDeviceId(chl.getDeviceCode());
						info.setChnlUuid(chl.getUuid());
						long dateStart = TimeHelper.getTimeStamp(element.beginTime);
						info.setStartTime(dateStart);
						long dateEnd = TimeHelper.getTimeStamp(element.endTime);
						info.setEndTime(dateEnd);
						info.setDeviceKey(chl.getEncryptKey());
						info.setRecordID(element.recordId);
						info.setFileLength(element.fileLength);
						if(element.type.equals("Event"))
							info.setEventType(RecordEventType.Event);
						else if(element.type.equals("All"))
							info.setEventType(RecordEventType.All);
						else if(element.type.equals("Manual"))
							info.setEventType(RecordEventType.Manual);
						mRecordInfoList.add(info);
					}
					Collections.reverse(mRecordInfoList);// 反序 临时使用下
					// todo:其他信息解析请见OpenAPI 平台sdk文档
					handler.obtainMessage(0, mRecordInfoList).sendToTarget(); // 发送成功消息到界面
				}
			}
		});
	}

	/**
	 * 查询通道下的云录像总数
	 * 
	 * @param chnlId
	 * @param startTime
	 * @param endTime
	 * @param startIndex
	 * @param endIndex
	 * @param handler
	 */
	public void queryCloudRecordNum(String chnlId, final String startTime,
			final String endTime, final Handler handler) {

		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		//
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				QueryCloudRecordNum req = new QueryCloudRecordNum();
				req.data.token = mToken;
				req.data.beginTime = startTime;
				req.data.endTime = endTime;
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.deviceId = chl.getDeviceCode();
				Log.d(tag, "startTime:" + req.data.beginTime + "endTime:"
						+ req.data.endTime + "channelId:" + req.data.channelId
						+ "deviceId:" + chl.getDeviceCode());

				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				QueryCloudRecordNum.Response resp = (QueryCloudRecordNum.Response) retObject.resp;
				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "QueryCloudRecordNum faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null) {
						Log.d(tag, "QueryCloudRecordNum success data is null");
					}
					// todo:其他信息解析请见OpenAPI 平台sdk文档
					handler.obtainMessage(
							0,
							resp.data.recordNum,
							resp.data.recordNum > 10 ? resp.data.recordNum - 9
									: 1).sendToTarget(); // 发送成功消息到界面
				}
			}
		});
	}

	/**
	 * 查询通道下的云录像列表
	 * 
	 * @param chnlId
	 * @param startTime
	 * @param endTime
	 * @param startIndex
	 * @param endIndex
	 * @param handler
	 */
	public void queryCloudRecordList(String chnlId, final String startTime,
			final String endTime, int startIndex, int endIndex,
			final Handler handler) {
		mRecordInfoList.clear(); // 清数据
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}

		final String strNneed = String.valueOf(startIndex) + "-"
				+ String.valueOf(endIndex); // 需要录像的条数，从那条记录到那一条，demo只读取前面10条，不查询完所有录像
		//
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				QueryCloudRecords req = new QueryCloudRecords();
				req.data.token = mToken;
				req.data.beginTime = startTime;
				req.data.endTime = endTime;
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.queryRange = strNneed;
				req.data.deviceId = chl.getDeviceCode();
				Log.d(tag, "startTime:" + req.data.beginTime + "endTime:"
						+ req.data.endTime + "channelId:" + req.data.channelId
						+ "deviceId:" + chl.getDeviceCode() + "strNneed:"
						+ strNneed);

				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				QueryCloudRecords.Response resp = (QueryCloudRecords.Response) retObject.resp;
				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "queryCloudRecordList faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null || resp.data.records == null) {
						Log.d(tag, "queryRecordList success data is null");
					}

					for (RecordsElement element : resp.data.records) {
						RecordInfo info = new RecordInfo();
						info.setChnlUuid(chl.getUuid());

						String dateStart = element.beginTime;
						// dateStart *= 1000;
						info.setStartTime(TimeHelper.getTimeStamp(dateStart));
						String dateEnd = element.endTime;
						// dateEnd *= 1000;
						info.setEndTime(TimeHelper.getTimeStamp(dateEnd));
						info.setDeviceKey(chl.getEncryptKey());
						info.setRecordID(element.recordId);
						info.setBackgroudImgUrl(element.thumbUrl);
						info.setDeviceId(element.deviceId);
						info.setType(RecordInfo.RecordType.PublicCloud);
						if (element.size.length() > 0) {
							info.setFileLength(Long.parseLong(element.size));
						} else { // easy4ip设备size字段为“”
							info.setFileLength(0);
						}
						mRecordInfoList.add(info);
					}
					Collections.reverse(mRecordInfoList);// 反序 临时使用下
					// todo:其他信息解析请见OpenAPI 平台sdk文档
					handler.obtainMessage(0, mRecordInfoList).sendToTarget(); // 发送成功消息到界面
				}
			}
		});
	}

	/**
	 * 获取设备动检计划
	 * 
	 * @param chnlId
	 * @param handler
	 */
	public void getAlarmStatus(final String chnlId, final Handler handler) {

		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				DeviceAlarmPlan req = new DeviceAlarmPlan();
				req.data.token = mToken;
				req.data.deviceId = chl.getDeviceCode();
				req.data.channelId = String.valueOf(chl.getIndex());

				DeviceAlarmPlan.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);
				resp = (DeviceAlarmPlan.Response) retObject.resp;

				if (retObject.mErrorCode != 0) {
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					int arg1 = resp.data.rules.get(0).enable == true ? 1 : 0;
					handler.obtainMessage(0, arg1, 0).sendToTarget();

				}

			}
		});
	}

	/**
	 * 改变设备的动检开关
	 * 
	 * @param enable
	 * @param channelId
	 * @param deviceId
	 */
	public void modifyAlarmStatus(final boolean enable, final String chnlId,
			final Handler handler) {
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ModifyDeviceAlarmStatus req = new ModifyDeviceAlarmStatus();
				req.data.token = mToken;
				req.data.deviceId = chl.getDeviceCode();
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.enable = enable;

				ModifyDeviceAlarmStatus.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);

				// 网络请求失败
				handler.obtainMessage(retObject.mErrorCode).sendToTarget();

			}
		});
	}

	public void setStorageStartegy(final String enable, final String chnlId,
			final Handler handler) {
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				SetStorageStrategy req = new SetStorageStrategy();
				req.data.token = mToken;
				req.data.deviceId = chl.getDeviceCode();
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.status = enable;

				SetStorageStrategy.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);

				// 网络请求失败
				handler.obtainMessage(retObject.mErrorCode).sendToTarget();

			}
		});
	}

	public void upgradeDevice(final String chnlId, final Handler handler) {
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				UpgradeDevice req = new UpgradeDevice();
				req.data.token = mToken;
				req.data.deviceId = chl.getDeviceCode();

				UpgradeDevice.Response resp = null;
				RetObject retObject = null;
				retObject = request(req);

				// 网络请求失败
				handler.obtainMessage(retObject.mErrorCode).sendToTarget();

			}
		});
	}

	/**
	 * 查询报警消息列表
	 * 
	 * @param chnlId
	 * @param deviceId
	 * @param startTime
	 * @param endTime
	 * @param count
	 * @param handler
	 */
	public void queryAlarmMessageList(String chnlId, final String startTime,
			final String endTime, final int count, final Handler handler) {
		mAlarmMessageInfoList.clear();
		final ChannelInfo chl = getChannel(chnlId);
		if (chl == null) {
			return;
		}

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				GetAlarmMessage req = new GetAlarmMessage();
				req.data.token = mToken;
				req.data.beginTime = startTime;
				req.data.endTime = endTime;
				req.data.channelId = String.valueOf(chl.getIndex());
				req.data.count = "" + count;
				req.data.deviceId = chl.getDeviceCode();
				Log.d(tag, "startTime:" + req.data.beginTime + "endTime:"
						+ req.data.endTime + "channelId:" + req.data.channelId
						+ "deviceId:" + chl.getDeviceCode());

				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				GetAlarmMessage.Response resp = (GetAlarmMessage.Response) retObject.resp;
				// 网络请求失败
				if (retObject.mErrorCode != 0) {
					Log.d(tag, "queryAlarmMessageList faied " + retObject.mMsg);
					handler.obtainMessage(retObject.mErrorCode).sendToTarget();
				} else {
					if (resp.data == null || resp.data.alarms == null) {
						Log.d(tag, "query Alarm Message success data is null");
					}

					for (AlarmsElement element : resp.data.alarms) {
						AlarmMessageInfo info = new AlarmMessageInfo();
						info.setChnlUuid(chl.getUuid());
						info.setAlarmId(element.alarmId);
						info.setType(element.type);
						info.setDeviceId(element.deviceId);
						info.setDeviceKey(chl.getEncryptKey());
						info.setName(element.name);
						// 暂不支持缩略图
						if (element.picurlArray.size() > 0)
							info.setPicUrl(element.picurlArray.get(0));
						info.setThumbUrl(element.thumbUrl);
						info.setLocalDate(element.localDate);

						info.setTime(element.time);
						mAlarmMessageInfoList.add(info);
					}

					// todo:其他信息解析请见RestAPI 平台sdk文档
					handler.obtainMessage(0, mAlarmMessageInfoList)
							.sendToTarget(); // 发送成功消息到界面
				}
			}
		});
	}

	/**
	 * 删除报警消息
	 * 
	 * @param indexId
	 * @param handler
	 */
	public void deleteAlarmMessage(final AlarmMessageInfo info,
			final Handler handler) {

		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
				DeleteAlarmMessage req = new DeleteAlarmMessage();
				req.data.token = mToken;
				req.data.indexId = info.getAlarmId();
				Log.d(tag, "indexId" + info.getAlarmId());

				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				DeleteAlarmMessage.Response resp = (DeleteAlarmMessage.Response) retObject.resp;

				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面
			}

		});
	}

	/**
	 * 检索已查询出来的录像对象
	 * 
	 * @param id
	 * @return
	 */
	public RecordInfo getRecord(String id) {
		// TODO Auto-generated method stub
		for (RecordInfo red : mRecordInfoList) {
			if (red.getId().equals(id)) {
				return red;
			}
		}
		return null;
	}

	/**
	 * 设备密码校验
	 * 
	 * @param deviceId
	 * @param pwd
	 * @return
	 */
	public int checkPwdValidity(String deviceId, String pwd) {
		return LCOpenSDK_DeviceInit.getInstance().checkPwdValidity(mToken, deviceId, pwd);
	}

	/**
	 * 设备初始化
	 * 
	 * @param deviceId
	 * @param pwd
	 * @param timeout
	 * @return
	 */
	public void searchDevice(final String deviceId, final int timeout, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				
				int ret = -1;
				String msg = "";
				DeviceInitInfo deviceInitInfo = LCOpenSDK_DeviceInit.getInstance().searchDeviceInitInfo(deviceId, timeout);
				if (deviceInitInfo != null) {
					if (deviceInitInfo.mStatus == 0) { // 不支持初始化操作
						ret = 0;
						msg = "device not support init";
					} else if (deviceInitInfo.mStatus == 1) { // 未初始化
						ret = 1;
						msg = "device not init yet";
					} else if(deviceInitInfo.mStatus == 2){ // 已初始化
						ret = 2;
						msg = "device already init";
					}else {
						ret = -2;
						msg = "device not found";
					}
				} else {
					ret = -1;
					msg = "StartSearchDevices failed";
				}

				handler.obtainMessage(ret, deviceInitInfo).sendToTarget();

			}
		});

	}

	/**
	 * 修改设备密码
	 * 
	 * @param deviceId
	 * @param oldPwd
	 * @param newPwd
	 * @param handler
	 */
	public void modifyDevicePwd(final String deviceId, final String oldPwd,
			final String newPwd, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {

			@Override
			public void run() {
//				if (0 != checkPwdValidity(deviceId, oldPwd)) {
//					RetObject retObject = new RetObject();
//					retObject.mErrorCode = -1;
//					retObject.mMsg = "checkPwdValidity failed";
//					handler.obtainMessage(retObject.mErrorCode, retObject)
//							.sendToTarget(); // 发送成功消息到界面
//					return;
//				}

				ModifyDevicePwd req = new ModifyDevicePwd();
				req.data.token = mToken;
				req.data.deviceId = deviceId;
				req.data.oldPwd = oldPwd;
				req.data.newPwd = newPwd;

				RetObject retObject = null;
				retObject = request(req); // 码流请求超时时间长一些
				ModifyDevicePwd.Response resp = (ModifyDevicePwd.Response) retObject.resp;

				handler.obtainMessage(retObject.mErrorCode, retObject)
						.sendToTarget(); // 发送成功消息到界面
			}

		});
	}
	
	public void initDevice(final String mac, final String key, final Handler handler){
		TaskPoolHelper.addTask(new RunnableTask("real"){
			@Override
			public void run() {
				int ret = LCOpenSDK_DeviceInit.getInstance().initDevice(mac, key);
				String msg = "";
				if(ret == 0){
					msg = "Init success";
				}
				else if(ret == -1){
					msg = "input param is empty";
				}
				else{//ret = error code
					msg = "InitDevAccount failed";
				}
				
				handler.obtainMessage(ret, msg).sendToTarget();
			}
		});
	}
	
	public void initDeviceByIP(final String mac, final String ip, final String key, final Handler handler){
		TaskPoolHelper.addTask(new RunnableTask("real"){
			@Override
			public void run() {
				int ret = LCOpenSDK_DeviceInit.getInstance().initDeviceByIP(mac, ip, key);
				String msg = "";
				if(ret == 0){
					msg = "Init success";
				}
				else if(ret == -1){
					msg = "input param is empty";
				}
				else{//ret = error code
					msg = "InitDevAccountByIP failed";
				}
				
				handler.obtainMessage(ret, msg).sendToTarget();
			}
		});
	}
	
	public void addDevices(final String devicesJsonStr, final Handler handler){
		TaskPoolHelper.addTask(new RunnableTask("real"){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				int ret = LCOpenSDK_LoginManager.addDevices(mToken, devicesJsonStr);
				String msg = "";
				if(ret == 1)
					msg = "addDevices success";
				else if(ret == -1)
					msg = "init server failed";
				else
					msg = "addDevices failed";
				
				handler.obtainMessage(ret,msg).sendToTarget();
			}
		});
	}

}
