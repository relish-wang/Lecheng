package com.lechange.demo.manager;

import java.util.List;

import android.Manifest;
import android.R.bool;
import android.R.integer;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;

import com.lechange.common.log.Logger;
import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.business.Business.RetObject;
import com.lechange.demo.business.util.TaskPoolHelper;
import com.lechange.demo.business.util.TaskPoolHelper.RunnableTask;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;
import com.lechange.demo.common.ProgressDialog;
import com.lechange.opensdk.api.bean.CheckDeviceBindOrNot;
import com.lechange.opensdk.api.bean.CheckDeviceBindOrNot.Response;
import com.lechange.opensdk.api.bean.DeviceOnline;
import com.lechange.opensdk.api.bean.UnBindDeviceInfo.ResponseData;
import com.lechange.opensdk.configwifi.LCOpenSDK_ConfigWifi;
import com.lechange.opensdk.device.LCOpenSDK_DeviceInit;
import com.lechange.opensdk.media.DeviceInitInfo;

public class DeviceAddActivity extends Activity implements OnClickListener,
		ActivityCompat.OnRequestPermissionsResultCallback {
	public final static String tag = "AddDeviceActivity";

	private final int startPolling = 0x10;
	private final int successOnline = 0x11;
	private final int asynWaitOnlineTimeOut = 0x12;
	private final int successAddDevice = 0x13;
	// private final int successOffline = 0x14;
	// private final int addDeviceTimeOut = 0x15;
	// private final int failedAddDevice = 0x16;
	// private final int successOnlineEx = 0x17;
	private final int deviceInitSuccess = 0x18;
	private final int deviceInitFailed = 0x19;
	private final int deviceInitByIPFailed = 0x1A;
	private final int deviceSearchSuccess = 0x1B;
	private final int deviceSearchFailed = 0x1C;
	
	private final int INITMODE_UNICAST = 0;
	private final int INITMODE_MULTICAST = 1;
	private int curInitMode = INITMODE_MULTICAST;

	private CommonTitle mCommonTitle;
	private ProgressDialog mProgressDialog; // 播放加载使用
	private WifiInfo mWifiInfo;
	private TextView mSsidText;
	private EditText mSnText;
	private EditText mPwdText;
	private ImageView mWirelessButton;
	private ImageView mWiredButton;
	
	private String key = "";

	private Handler mHandler;

	private boolean mIsOffline = true; 
	private boolean mIsDeviceSearched = false; //设备初始化标志，保证设备初始化接口只调用一次
	private boolean mIsDeviceInitSuccess = false;

	private enum ConfigStatus {
		query, wifipair, wired
	}; // 配对方式

	private ConfigStatus mConfigStatus = ConfigStatus.query; // 默认为轮询状态

	// 无线配置参数
	private static final int PROGRESS_TIMEOUT_TIME = 60*1000;
	private static final int PROGRESS_DELAY_TIME = 10*1000;
	private int time = 25;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_add);

		// 绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, 0,
				R.string.devices_add_name);

		mCommonTitle.setOnTitleClickListener(new OnTitleClickListener() {
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
					break;
				}
			}
		});

		// 绘制画面
		mSnText = (EditText) findViewById(R.id.deviceSN);
		mPwdText = (EditText) findViewById(R.id.wifiPasswd);
		mSsidText = (TextView) findViewById(R.id.wifiName);
		
		mWirelessButton = (ImageView) findViewById(R.id.wirelessAdd);
		mWirelessButton.setOnClickListener(this);
		mWiredButton = (ImageView) findViewById(R.id.wiredAdd);
		mWiredButton.setOnClickListener(this);
		// load组件
		mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);

		WifiManager mWifiManager = (WifiManager) getSystemService(Activity.WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();
		if (mWifiInfo != null) {
			mSsidText.setText("SSID:"
					+ mWifiInfo.getSSID().replaceAll("\"", ""));
		}
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				super.handleMessage(msg);
				Log.d(tag, "msg.what" + msg.what);
				switch (msg.what) {
				// 无线配对消息回调
				case LCOpenSDK_ConfigWifi.ConfigWifi_Event_Success:
					Log.d(tag, "smartConfig success");
					mConfigStatus = ConfigStatus.wifipair;
					toast("smartConfig success");
					stopConfig();
					mHandler.removeCallbacks(progressPoll);
					if (!mIsDeviceSearched){
						mIsDeviceSearched = true;
						searchDevice();
					}
					break;
				case deviceSearchSuccess:
					Log.d(tag,"deviceSearchSuccess");
					initDevice((DeviceInitInfo)msg.obj, curInitMode);
					break;
				case deviceSearchFailed:
					Log.d(tag,"deviceSearchFailed:"+(String)msg.obj);
					toast("deviceSearchFailed:"+(String)msg.obj);
					break;
				case deviceInitSuccess:
					Log.d(tag,"deviceInitSuccess");
					toast("deviceInitSuccess!");
					mIsDeviceInitSuccess = true;
					if(mIsOffline)
						checkOnline();
					else
						mHandler.obtainMessage(successOnline).sendToTarget();
					break;
				case deviceInitFailed:
					toast("deviceInitFailed: "+(String)msg.obj);
					//组播失败后走单播
					curInitMode = INITMODE_UNICAST;
					searchDevice();
					break;
				case deviceInitByIPFailed:
					toast("deviceInitByIPFailed: "+(String)msg.obj);
					curInitMode = INITMODE_MULTICAST;
					time = 0;
					mProgressDialog.setStop();
					break;
				case startPolling:
					checkOnline();
					break;
				// 校验消息回调
				case asynWaitOnlineTimeOut:
					Log.d(tag, "checkIsOnlineTimeOut");
					break;
				case successOnline:
					Log.d(tag, "successOnline");
					stopConfig();
					if(!mIsDeviceSearched){
						mIsDeviceSearched = true;
						searchDevice();
					}
					else if(mIsDeviceInitSuccess){//设备初始化成功且在线，则绑定
						unBindDeviceInfo();
					}
					break;
				case successAddDevice:
					// DeviceInfo device = (DeviceInfo) msg.obj;
					// success(device.getUuid());
					Log.d(tag, "SuccessAddDevice");
					toast("SuccessAddDevice");
					// 只有这么一种情况
					setResult(RESULT_OK);
					finish();
					break;
				default:
					break;
				}
			}
		};
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		time = 0;
		stopConfig();
	}

	@Override
	public void onClick(View v) {
		mIsDeviceSearched = false;
		mIsOffline = true;
		mIsDeviceInitSuccess = false;
		
		switch (v.getId()) {
		case R.id.wirelessAdd:
			if (TextUtils.isEmpty(mPwdText.getText().toString())
					|| TextUtils.isEmpty(mSnText.getText().toString())) {
				toast(getString(R.string.toast_adddevice_no_sn_or_psw));

			} else {
				checkOnBindandremind();
			}
			break;
		case R.id.wiredAdd:
			if (TextUtils.isEmpty(mSnText.getText().toString())) {
				toast(getString(R.string.toast_adddevice_no_sn));
			} else {
				mConfigStatus = ConfigStatus.wired;
				checkOnBindandline();
			}
			break;
		default:
			break;
		}

	}

	/**
	 * 校验在线
	 */
	private void checkOnline() {
		// 隔两秒轮询.....
		Business.getInstance().checkOnline(mSnText.getText().toString(),
				new Handler() {
					@Override
					public void handleMessage(Message msg) {
						if (!mIsOffline)
							return;

						RetObject retObject = (RetObject) msg.obj;
						switch (msg.what) {
						case 0:
							if (((DeviceOnline.Response) retObject.resp).data.onLine.equals("1")) {
								switch (mConfigStatus) {
								case wired:
									Log.d(tag, "有线配对....");
									break;
								case query:
									Log.d(tag, "轮询....");
									stopConfig();
								case wifipair:
									mProgressDialog.setStop();
								}
								toast("Online");
								mIsOffline = false;
								mHandler.obtainMessage(successOnline).sendToTarget();
							} else {
								if (mConfigStatus == ConfigStatus.wired) {
									Log.d(tag, "offline..... wired");
									toast("offline");
								} else if (time > 0) {
									Log.d(tag, "offline..... try again checkOnline");
									try {
										Thread.sleep(2000);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									time--;
									mHandler.obtainMessage(startPolling)
											.sendToTarget();
								} else {
									Log.d(tag, "offline..... try again max");
									mProgressDialog.setStop();
									time = 25;
									toast("offline");
								}
							}
							break;
						case -1000:
							if (time > 0) {
								Log.d(tag,
										"code:-1000..... try again checkOnline");
								try {
									Thread.sleep(2000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								time--;
								mHandler.obtainMessage(startPolling)
										.sendToTarget();
							}
							break;
						default:
							switch (mConfigStatus) {
							case wired:
								Log.d(tag, "有线配对失败....");
								break;
							case query:
								Log.d(tag, "轮询失败....");
								stopConfig();
							case wifipair:
								mProgressDialog.setStop();
							}
							toast(retObject.mMsg);
							break;
						}
					}
				});
		
	}

	/**
	 * 绑定
	 */
	private void bindDevice() {
		//设备绑定
		Business.getInstance().bindDevice(mSnText.getText().toString(), key,
				new Handler() {
					@Override
					public void handleMessage(Message msg) {
						RetObject retObject = (RetObject) msg.obj;
						if (msg.what == 0) {
							mHandler.obtainMessage(successAddDevice).sendToTarget();
						} else {
							toast("addDevice failed");
							Log.d(tag, retObject.mMsg);
						}
					}
				});
	}

	private void unBindDeviceInfo(){
		if(!Business.getInstance().isOversea){
			Business.getInstance().unBindDeviceInfo(mSnText.getText().toString(), new Handler(){
				public void handleMessage(Message msg) {
					String message = (String) msg.obj;
	//				Log.d(tag, "unBindDeviceInfo,"+message);
					if (msg.what == 0) {
						if(message.contains("Auth")){
							bindDevice();
						}
						else if(message.contains("RegCode")){
							final EditText et = new EditText(DeviceAddActivity.this);
							final AlertDialog dialog = new AlertDialog.Builder(DeviceAddActivity.this)
							.setTitle(R.string.alarm_message_keyinput_dialog_title)
							.setIcon(android.R.drawable.ic_dialog_info)
							.setView(et)
							.setPositiveButton(R.string.dialog_positive,null)
							.setNegativeButton(R.string.dialog_nagative, null)
							.create();
							dialog.setCanceledOnTouchOutside(false);
							dialog.show();
							
							dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new OnClickListener() {
								public void onClick(View v) {
									if(TextUtils.isEmpty(et.getText())){
										toast("Input can't be empty");
										return;
									}
									key = et.getText().toString();
									bindDevice();
									dialog.dismiss();
								}
							});
						}
						else{
							key = "";
							bindDevice();
						}
					} else {
						toast("unBindDeviceInfo failed");
						Log.d(tag, message);
					}
				}
			});
		}
		else{ //oversea
			bindDevice();
		}
		
	}
	
	/**
	 * 有线配对校验
	 */
	private void checkOnBindandline() {
		Business.getInstance().checkBindOrNot(mSnText.getText().toString(),
				new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// TODO Auto-generated method stub
						super.handleMessage(msg);
						RetObject retObject = (RetObject) msg.obj;
						if (msg.what == 0) {
							CheckDeviceBindOrNot.Response resp = (Response) retObject.resp;
							if (!resp.data.isBind){
								mIsDeviceSearched = true;
								searchDevice();
							}
							else if (resp.data.isBind && resp.data.isMine)
								toast(getString(R.string.toast_adddevice_already_binded_by_self));
							else
								toast(getString(R.string.toast_adddevice_already_binded_by_others));
						} else {
							toast(retObject.mMsg);
						}
					}
				});
	}

	/**
	 * 无线配对校验
	 */
	public void checkOnBindandremind() {
		Business.getInstance().checkBindOrNot(mSnText.getText().toString(),
				new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// TODO Auto-generated method stub
						super.handleMessage(msg);
						RetObject retObject = (RetObject) msg.obj;
						if (msg.what == 0) {
							CheckDeviceBindOrNot.Response resp = (Response) retObject.resp;
							if (!resp.data.isBind) {
								showWifiConfig();
							} else if (resp.data.isBind && resp.data.isMine)
								toast(getString(R.string.toast_adddevice_already_binded_by_self));
							else
								toast(getString(R.string.toast_adddevice_already_binded_by_others));
						} else {
							toast(retObject.mMsg);
						}
					}
				});
	}

	/**
	 * 启动无线配对
	 */
	private void startConfig() {
		// 开启播放加载控件
		mProgressDialog.setStart(getString(R.string.wifi_config_loading));

		String ssid = mWifiInfo.getSSID().replaceAll("\"", "");
		String ssid_pwd = mPwdText.getText().toString();
		String code = mSnText.getText().toString().toUpperCase();

		String mCapabilities = getWifiCapabilities(ssid);
		// 无线超时任务
		mHandler.postDelayed(progressRun, PROGRESS_TIMEOUT_TIME);
		// 10s开启轮询
		mHandler.postDelayed(progressPoll, PROGRESS_DELAY_TIME);
		// 调用接口，开始通过smartConfig匹配
		// System.out.println("mLinkIPCProxy.start");
		LCOpenSDK_ConfigWifi.configWifiStart(code, ssid, ssid_pwd,
				mCapabilities, mHandler);
	}

	/**
	 * 关闭无线配对
	 */
	private void stopConfig() {
		mHandler.removeCallbacks(progressRun);
		LCOpenSDK_ConfigWifi.configWifiStop();// 调用smartConfig停止接口
	}

	/**
	 * 无线配对超时任务
	 */
	private Runnable progressRun = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			toast("超时配置失败");
			stopConfig();
		}
	};

	/**
	 * 轮询定时启动任务
	 */
	private Runnable progressPoll = new Runnable() {
		@Override
		public void run() {
			// TODO Auto-generated method stub
			mHandler.obtainMessage(startPolling).sendToTarget();
		}
	};

	/**
	 * 获取wifi加密信息
	 */
	private String getWifiCapabilities(String ssid) {
		String mCapabilities = null;
		ScanResult mScanResult = null;
		WifiManager mWifiManager = (WifiManager) getSystemService(Activity.WIFI_SERVICE);
		if (mWifiManager != null) {
			WifiInfo mWifi = mWifiManager.getConnectionInfo();
			if (mWifi != null) {
				// 判断SSID是否�?��
				if (mWifi.getSSID() != null
						&& mWifi.getSSID().replaceAll("\"", "").equals(ssid)) {
					List<ScanResult> mList = mWifiManager.getScanResults();
					if (mList != null) {
						for (ScanResult s : mList) {
							if (s.SSID.replaceAll("\"", "").equals(ssid)) {
								mScanResult = s;
								break;
							}
						}
					}
				}
			}
		}
		mCapabilities = mScanResult != null ? mScanResult.capabilities : null;
		return mCapabilities;
	}

	private void toast(String content) {
		Toast.makeText(getApplicationContext(), content, Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
			@NonNull String[] permissions, @NonNull int[] grantResults) {
		// TODO Auto-generated method stub
		if (grantResults.length == 1
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			// Camera permission has been granted, preview can be displayed
			showPairDescription();

		} else {
			toast(getString(R.string.toast_permission_location_forbidden));

		}

	}

	/**
	 * 开启无线配网流程（权限检查，配对说明）
	 */
	public void showWifiConfig() {
		boolean isMinSDKM = Build.VERSION.SDK_INT < 23;
		boolean isGranted = ActivityCompat.checkSelfPermission(this,
				Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
		if (isMinSDKM || isGranted) {

			showPairDescription();
			// 开启无线配对
			return;
		}

		requestLocationPermission();
	}

	/**
	 * 显示配对说明
	 */
	private void showPairDescription() {
		DialogInterface.OnClickListener dialogOnclicListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case Dialog.BUTTON_POSITIVE:
					startConfig();
					break;
				case Dialog.BUTTON_NEGATIVE:
					break;
				case Dialog.BUTTON_NEUTRAL:
					break;
				}
			}
		};
		// dialog参数设置
		AlertDialog.Builder builder = new AlertDialog.Builder(
				DeviceAddActivity.this); // 先得到构造器
		builder.setTitle(R.string.devices_config_dialog_title); // 设置标题
		builder.setMessage(R.string.devices_config_dialog_message); // 设置内容
		builder.setPositiveButton(R.string.dialog_positive,
				dialogOnclicListener);
		builder.setNegativeButton(R.string.dialog_nagative,
				dialogOnclicListener);
		builder.create().show();
	}

	/**
	 * 请求相关权限
	 */
	private void requestLocationPermission() {
		if (ActivityCompat.shouldShowRequestPermissionRationale(this,
				Manifest.permission.ACCESS_FINE_LOCATION)) {

			Log.d("Uriah", "Uriah + shouldShowRequestPermission true");
			ActivityCompat.requestPermissions(this,
					new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
					1);
		} else {
			ActivityCompat.requestPermissions(this,
					new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
					1);
		}
	}
	
	private void searchDevice(){
		final String deviceId = mSnText.getText().toString();
		Business.getInstance().searchDevice(deviceId, 15000, new Handler(){
			public void handleMessage(final Message msg) {
				if (msg.what < 0) {
					if(msg.what == -2)
						mHandler.obtainMessage(deviceSearchFailed, "device not found").sendToTarget();
					else
						mHandler.obtainMessage(deviceSearchFailed, "StartSearchDevices failed").sendToTarget();
					return;
				}
				
				mHandler.obtainMessage(deviceSearchSuccess, msg.obj).sendToTarget();
			}
		});
	}
	
	private void searchDeviceEx(){
		final EditText et = new EditText(DeviceAddActivity.this);
		final String deviceId = mSnText.getText().toString();
		
		//设备初始化
		Business.getInstance().searchDevice(deviceId, 15000, new Handler(){
			public void handleMessage(final Message msg) {
				if (msg.what < 0) {
					if(msg.what == -2)
						mHandler.obtainMessage(deviceInitFailed, "device not found").sendToTarget();
					else
						mHandler.obtainMessage(deviceInitFailed, "StartSearchDevices failed").sendToTarget();
					return;
				}
				
				final DeviceInitInfo deviceInitInfo = (DeviceInitInfo)msg.obj;
				final int status = deviceInitInfo.mStatus;
				
				//not support init
				if(status == 0 && !Business.getInstance().isOversea){
					key = "";
					mHandler.obtainMessage(deviceInitSuccess, "inner, go bind without key").sendToTarget();
				}
				else{
					if (status == 1){
						et.setHint(getString(R.string.toast_adddevice_input_device_key_to_init));
					}
					else{
						et.setHint(getString(R.string.toast_adddevice_input_device_key_after_init));
					}
					
					AlertDialog dialog = new AlertDialog.Builder(DeviceAddActivity.this)
					.setTitle(R.string.alarm_message_keyinput_dialog_title)
					.setIcon(android.R.drawable.ic_dialog_info)
					.setView(et)
					.setPositiveButton(R.string.dialog_positive,
							new android.content.DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									key = et.getText().toString();
									if(status == 0 || status == 2){
										if(Business.getInstance().isOversea)
											checkPwdValidity(deviceId, key, mHandler);
										else
											mHandler.obtainMessage(deviceInitSuccess, "Inner, go bind with key").sendToTarget();
									}
									else if(status == 1){
										Business.getInstance().initDevice(deviceInitInfo.mMac, key, new Handler(){
											public void handleMessage(Message msg) {
												String message = (String) msg.obj;
												if(msg.what == 0){
													mHandler.obtainMessage(deviceInitSuccess, message).sendToTarget();
												}else{
													mHandler.obtainMessage(deviceInitFailed, message).sendToTarget();
												}
											};
										});
									}
								}
							})
					.setNegativeButton(R.string.dialog_nagative, 
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mHandler.obtainMessage(deviceInitFailed, "Init has been cancelled").sendToTarget();
							}
						})
					.create();
					dialog.setCanceledOnTouchOutside(false);
					dialog.show();
				}
			}
		});

	}
	
	public void initDevice(final DeviceInitInfo deviceInitInfo, int initMode){
		//1.使用组播进行初始化（initMode=INITMODE_MULTICAST）,走else流程
		//2.组播失败后再使用单播（initMode=INITMODE_UNICAST），此时直接使用组播时输入的秘钥进行初始化
		if(initMode == INITMODE_UNICAST){
			Business.getInstance().initDeviceByIP(deviceInitInfo.mMac, deviceInitInfo.mIp, key, new Handler(){
				public void handleMessage(Message msg) {
					String message = (String) msg.obj;
					if(msg.what == 0){
						mHandler.obtainMessage(deviceInitSuccess, message).sendToTarget();
					}else{
						mHandler.obtainMessage(deviceInitByIPFailed, message).sendToTarget();
					}
				};
			});
		}
		else{
		
			final EditText et = new EditText(DeviceAddActivity.this);
			final String deviceId = mSnText.getText().toString();
			
			final int status = deviceInitInfo.mStatus;
			
			//not support init
			if(status == 0 && !Business.getInstance().isOversea){
				key = "";
				mHandler.obtainMessage(deviceInitSuccess, "inner, go bind without key").sendToTarget();
			}
			else{
				if (status == 1){
					et.setHint(getString(R.string.toast_adddevice_input_device_key_to_init));
				}
				else{
					et.setHint(getString(R.string.toast_adddevice_input_device_key_after_init));
				}
				
				AlertDialog dialog = new AlertDialog.Builder(DeviceAddActivity.this)
				.setTitle(R.string.alarm_message_keyinput_dialog_title)
				.setIcon(android.R.drawable.ic_dialog_info)
				.setView(et)
				.setPositiveButton(R.string.dialog_positive,
						new android.content.DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								key = et.getText().toString();
								if(status == 0 || status == 2){
									if(Business.getInstance().isOversea)
										checkPwdValidity(deviceId, key, mHandler);
									else
										mHandler.obtainMessage(deviceInitSuccess, "Inner, go bind with key").sendToTarget();
								}
								else if(status == 1){
									Business.getInstance().initDevice(deviceInitInfo.mMac, key, new Handler(){
										public void handleMessage(Message msg) {
											String message = (String) msg.obj;
											if(msg.what == 0){
												mHandler.obtainMessage(deviceInitSuccess, message).sendToTarget();
											}else{
												mHandler.obtainMessage(deviceInitFailed, message).sendToTarget();
											}
										};
									});
								}
							}
						})
				.setNegativeButton(R.string.dialog_nagative, 
					new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							mHandler.obtainMessage(deviceInitByIPFailed, "Init has been cancelled").sendToTarget();
						}
					})
				.create();
				dialog.setCanceledOnTouchOutside(false);
				dialog.show();
			}
		}
	}
	
	public void checkPwdValidity(final String deviceId, final String key, final Handler handler) {
		TaskPoolHelper.addTask(new RunnableTask("real") {
			@Override
			public void run() {
				if(0 == Business.getInstance().checkPwdValidity(deviceId, key)){
					handler.obtainMessage(deviceInitSuccess, "checkPwdValidity success").sendToTarget();
				}else {
					handler.obtainMessage(deviceInitByIPFailed, "checkPwdValidity failed").sendToTarget();
				}
			}
		});
	}
}
