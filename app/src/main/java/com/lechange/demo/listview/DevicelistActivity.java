package com.lechange.demo.listview;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lechange.common.log.Logger;
import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.business.Business.RetObject;
import com.lechange.demo.business.entity.ChannelInfo;
import com.lechange.demo.business.util.ImageHelper;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;
import com.lechange.demo.common.ProgressDialog;
import com.lechange.demo.manager.DeviceAddActivity;
import com.lechange.demo.manager.DeviceSetActivity;
import com.lechange.demo.mediaplay.MediaPlayActivity;
import com.lechange.demo.message.AlarmMessageActivity;

public class DevicelistActivity extends Activity{
	private final String tag = "MainActivity";
	
	private ListView mListview;
	private CommonTitle mCommonTitle;
	private ProgressDialog mProgressDialog;  //请求加载使用
	private List<ChannelInfo> mChannelInfoList;
	private ChannelAdapter mChnlAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device_list);
		
		//绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, R.drawable.title_btn_deviceadd, R.string.devices_name);
		
		mCommonTitle.setOnTitleClickListener(new OnTitleClickListener() {		
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
					break;
				case CommonTitle.ID_RIGHT:
					Intent intent = new Intent(DevicelistActivity.this, DeviceAddActivity.class);
					Log.d(tag, "DeviceAddActivity");
					startActivityForResult(intent, 0);
					break;
				default:
					break;
				}
			}
		});
		
		//开启请求加载控件
		mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);	
		mProgressDialog.setStart(getString(R.string.common_loading));
		
		//绘制list
		mListview = (ListView)this.findViewById(R.id.list_devices);
		mChnlAdapter = new ChannelAdapter(this);
		mListview.setAdapter(mChnlAdapter);
	
		// 初始化数据
		loadChannelList();
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) { //resultCode为回传的标记
		case RESULT_OK:
		    loadChannelList();
		    mChnlAdapter.notifyDataSetChanged();
		    mProgressDialog.setStart(getString(R.string.common_loading));
		    //Toast.makeText(DevicelistActivity.this, "刷新完成", Toast.LENGTH_SHORT).show();
		    break;
		default:
		    break;
		}
	}
	
	private void loadChannelList() {
		
		mChannelInfoList = new ArrayList<ChannelInfo>();
		
		
		// 初始化数据
		Business.getInstance().getChannelList(new Handler()	{
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				mProgressDialog.setStop();  //关闭加载窗口
				Business.RetObject retObject = (RetObject) msg.obj;
				if (msg.what == 0) {
					mChannelInfoList.addAll((List<ChannelInfo>) retObject.resp);
				//	mChannelInfoList = (List<ChannelInfo>) retObject.resp;
					if(mChannelInfoList != null && mChannelInfoList.size() > 0){
						mChnlAdapter.notifyDataSetChanged();
//						addDevices();
					} else{
						Toast.makeText(DevicelistActivity.this, R.string.toast_device_no_devices, Toast.LENGTH_LONG).show();
					}
				}
				else {
					Toast.makeText(DevicelistActivity.this, retObject.mMsg, Toast.LENGTH_LONG).show();
					Logger.e(tag, retObject.mMsg);
				}
			}
		});
		
		//国内支持共享设备和授权设备
		if(!Business.getInstance().isOversea){
			Business.getInstance().getSharedDeviceList(new Handler()	{
				@SuppressWarnings("unchecked")
				@Override
				public void handleMessage(Message msg) {
					mProgressDialog.setStop();  //关闭加载窗口
					Business.RetObject retObject = (RetObject) msg.obj;
					if (msg.what == 0 && retObject.resp != null) {
						mChannelInfoList.addAll((List<ChannelInfo>) retObject.resp);
					//	mChannelInfoList = (List<ChannelInfo>) retObject.resp;
						if(mChannelInfoList != null && mChannelInfoList.size() > 0){
							mChnlAdapter.notifyDataSetChanged();
						} else{
							Toast.makeText(DevicelistActivity.this, R.string.devices_no_shared_device, Toast.LENGTH_SHORT).show();
						}
					}
					else {
						Toast.makeText(DevicelistActivity.this, retObject.mMsg, Toast.LENGTH_SHORT).show();
					}
				}
			});
			System.out.println("devices");
			Business.getInstance().getBeAuthDeviceList(new Handler()	{
				@SuppressWarnings("unchecked")
				@Override
				public void handleMessage(Message msg) {
					mProgressDialog.setStop();  //关闭加载窗口
					Business.RetObject retObject = (RetObject) msg.obj;
					if (msg.what == 0 && retObject.resp != null) {
						mChannelInfoList.addAll((List<ChannelInfo>) retObject.resp);
					//	mChannelInfoList = (List<ChannelInfo>) retObject.resp;
						if(mChannelInfoList != null && mChannelInfoList.size() > 0){
							mChnlAdapter.notifyDataSetChanged();
						} else{
							Toast.makeText(DevicelistActivity.this, R.string.devices_no_authorized_device, Toast.LENGTH_SHORT).show();
						}
					}
					else {
						Toast.makeText(DevicelistActivity.this, retObject.mMsg, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}
//		try {
//			Thread.sleep(50000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
//		addDevices();
	}
	
	private void addDevices(){
		for(int i=0; i<mChannelInfoList.size(); i++){
			StringBuilder devicesJson = new StringBuilder();
			if(mChannelInfoList.size()>0){
				ChannelInfo c = mChannelInfoList.get(i);
				devicesJson.append("[{\"Sn\":\""+c.getDeviceCode()+"\",");
				devicesJson.append("\"Port\":554,");
				devicesJson.append("\"User\":\"\",");
				devicesJson.append("\"Pwd\":\"\",");
				devicesJson.append("\"Type\":1}]");
				
				Business.getInstance().addDevices(devicesJson.toString(), new Handler(){
					@Override
					public void handleMessage(Message msg) {
						Toast.makeText(DevicelistActivity.this, (String)msg.obj, Toast.LENGTH_LONG).show();
					}
				});
			}
		}
		
	}
	
	private class ChannelAdapter extends BaseAdapter
	{
		private LayoutInflater mInflater;
		
		public ChannelAdapter(Context context){
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mChannelInfoList != null ? mChannelInfoList.size():0;
		}

		@Override
		public ChannelInfo getItem(int position) {
			return mChannelInfoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		private void unBindDevice(final ChannelInfo info){
			Business.getInstance().unBindDevice(info.getDeviceCode(), new Handler(){
				@Override
				public void handleMessage(Message msg) {
					RetObject retObject = (RetObject) msg.obj;
					if(msg.what == 0){
						Toast.makeText(getApplicationContext(), R.string.toast_device_delete_success, Toast.LENGTH_SHORT).show();
						mChannelInfoList.remove(info);
						mChnlAdapter.notifyDataSetChanged();
					}else{
						Toast.makeText(getApplicationContext(), retObject.mMsg, Toast.LENGTH_SHORT).show();
					}
						
				}
			});
		}
		
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.activity_device_list_item, null);
				holder = new ViewHolder();
				holder.mChannelName = (TextView) convertView.findViewById(R.id.list_channel_name);
				holder.mDelete = (ImageView)convertView.findViewById(R.id.list_device_delete);
				holder.mBgDevice = (RelativeLayout) convertView.findViewById(R.id.list_bg_device);
				holder.mLiveVideo = (ImageView)convertView.findViewById(R.id.list_device_livevideo);
				holder.mLocalVideo = (ImageView)convertView.findViewById(R.id.list_device_localvideo);
				holder.mCloudVideo = (ImageView) convertView.findViewById(R.id.list_device_cloudvideo);
				holder.mMessage = (ImageView)convertView.findViewById(R.id.list_device_message);
				holder.mSetting = (ImageView)convertView.findViewById(R.id.list_device_setting);
				holder.mListShade = (LinearLayout) convertView.findViewById(R.id.list_shade);
				
				//设置监听
				holder.mDelete.setOnClickListener(new OnClickListener() {		
					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub					
						DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){		  
							@Override  
							public void onClick(DialogInterface dialog, int which) {  
								switch(which){  
								case Dialog.BUTTON_POSITIVE:  
									unBindDevice(holder.mInfo);
									break;
								case Dialog.BUTTON_NEGATIVE:  
									break; 
								case Dialog.BUTTON_NEUTRAL:  
									break;
								}  
							}
						};  
						//dialog参数设置  
						AlertDialog.Builder builder=new AlertDialog.Builder(DevicelistActivity.this);  //先得到构造器  
						builder.setTitle(R.string.devices_delete_dialog_title); //设置标题  
						builder.setMessage(R.string.devices_delete_dialog_message); //设置内容  
						builder.setPositiveButton(R.string.dialog_positive,dialogOnclicListener);
						builder.setNegativeButton(R.string.dialog_nagative,dialogOnclicListener);
						builder.create().show();
					}
				});

				holder.mListShade.setVisibility(View.GONE);

				holder.mLiveVideo.setOnClickListener(new OnClickListener() {				
					@Override
					public void onClick(View arg0) {			
						// 启动实时视频
						final EditText et = new EditText(DevicelistActivity.this);
						if (holder.mInfo.getEncryptMode() == 1 && holder.mInfo.getEncryptKey() == null) {
							new AlertDialog.Builder(DevicelistActivity.this).setTitle(R.string.alarm_message_keyinput_dialog_title)
								.setIcon(android.R.drawable.ic_dialog_info).setView(et)
								.setPositiveButton(R.string.dialog_positive, new android.content.DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										holder.mInfo.setEncryptKey(et.getText().toString());
										Intent intent = new Intent(DevicelistActivity.this, MediaPlayActivity.class);
										intent.putExtra("UUID", holder.mInfo.getUuid());
										intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_ONLINE);
										intent.putExtra("MEDIA_TITLE", R.string.live_play_name);
										DevicelistActivity.this.startActivityForResult(intent, 0);
									}
								})
								.setNegativeButton(R.string.dialog_nagative, null).show();
						} else {
							Intent intent = new Intent(DevicelistActivity.this, MediaPlayActivity.class);
							intent.putExtra("UUID", holder.mInfo.getUuid());
							intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_ONLINE);
							intent.putExtra("MEDIA_TITLE", R.string.live_play_name);
							DevicelistActivity.this.startActivityForResult(intent, 0);
						}
					}
				});			
				holder.mLocalVideo.setOnClickListener(new OnClickListener() {		
					@Override
					public void onClick(View arg0) {
						// 启动本地录像
						final EditText et = new EditText(DevicelistActivity.this);
						if (holder.mInfo.getEncryptMode() == 1 && holder.mInfo.getEncryptKey() == null) {
							new AlertDialog.Builder(DevicelistActivity.this).setTitle(R.string.alarm_message_keyinput_dialog_title)
								.setIcon(android.R.drawable.ic_dialog_info).setView(et)
								.setPositiveButton(R.string.dialog_positive, new android.content.DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										holder.mInfo.setEncryptKey(et.getText().toString());
										Intent intent = new Intent(DevicelistActivity.this, RecordListActivity.class);
										intent.putExtra("UUID", holder.mInfo.getUuid());
										intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_RECORD);						
										intent.putExtra("MEDIA_TITLE", R.string.local_records_name);
										DevicelistActivity.this.startActivity(intent);
									}
								})
								.setNegativeButton(R.string.dialog_nagative, null).show();
						} else {
							Intent intent = new Intent(DevicelistActivity.this, RecordListActivity.class);
							intent.putExtra("UUID", holder.mInfo.getUuid());
							intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_RECORD);						
							intent.putExtra("MEDIA_TITLE", R.string.local_records_name);
							DevicelistActivity.this.startActivity(intent);
						}									
					}
				});
				holder.mCloudVideo.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						// 启动云录像
						final EditText et = new EditText(DevicelistActivity.this);
						if (holder.mInfo.getEncryptMode() == 1 && holder.mInfo.getEncryptKey() == null) {
							new AlertDialog.Builder(DevicelistActivity.this).setTitle(R.string.alarm_message_keyinput_dialog_title)
								.setIcon(android.R.drawable.ic_dialog_info).setView(et)
								.setPositiveButton(R.string.dialog_positive, new android.content.DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										holder.mInfo.setEncryptKey(et.getText().toString());
										Intent intent = new Intent(DevicelistActivity.this, RecordListActivity.class);
										intent.putExtra("UUID", holder.mInfo.getUuid());
										intent.putExtra("MEDIA_TITLE", R.string.cloud_records_name);
										intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD);
										DevicelistActivity.this.startActivity(intent);
									}
								})
								.setNegativeButton(R.string.dialog_nagative, null).show();
						} else {
							Intent intent = new Intent(DevicelistActivity.this, RecordListActivity.class);
							intent.putExtra("UUID", holder.mInfo.getUuid());
							intent.putExtra("MEDIA_TITLE", R.string.cloud_records_name);
							intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD);
							DevicelistActivity.this.startActivity(intent);
						}
					}
				});			
				holder.mMessage.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub				
						// 启动报警图片
						final EditText et = new EditText(DevicelistActivity.this);
						if (holder.mInfo.getEncryptMode() == 1 && holder.mInfo.getEncryptKey() == null) {
							new AlertDialog.Builder(DevicelistActivity.this).setTitle(R.string.alarm_message_keyinput_dialog_title)
								.setIcon(android.R.drawable.ic_dialog_info).setView(et)
								.setPositiveButton(R.string.dialog_positive, new android.content.DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										// TODO Auto-generated method stub
										holder.mInfo.setEncryptKey(et.getText().toString());
										Intent intent = new Intent(DevicelistActivity.this, AlarmMessageActivity.class);
										intent.putExtra("sn", holder.mInfo.getDeviceCode());
										intent.putExtra("UUID", holder.mInfo.getUuid());
										intent.putExtra("index", holder.mInfo.getIndex());
										DevicelistActivity.this.startActivity(intent);
										System.out.println("DeviceCode"+holder.mInfo.getDeviceCode());
									}
								})
								.setNegativeButton(R.string.dialog_nagative, null).show();
						} else {
							Intent intent = new Intent(DevicelistActivity.this, AlarmMessageActivity.class);
							intent.putExtra("sn", holder.mInfo.getDeviceCode());
							intent.putExtra("UUID", holder.mInfo.getUuid());
							intent.putExtra("index", holder.mInfo.getIndex());
							DevicelistActivity.this.startActivity(intent);
							System.out.println("DeviceCode"+holder.mInfo.getDeviceCode());
						}
					}
				});
				//设备操作
				holder.mSetting.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						// 启动设备管理
						Intent intent = new Intent(DevicelistActivity.this,DeviceSetActivity.class);

						intent.putExtra("UUID", holder.mInfo.getUuid());
						intent.putExtra("alarmPlanStatus", holder.mInfo.getAlarmStatus());
						intent.putExtra("cloudMealStatus", holder.mInfo.getCloudMealStates());


						DevicelistActivity.this.startActivity(intent);
						System.out.println("DeviceCode"+holder.mInfo.getDeviceCode());
					}
				});
				//屏蔽监听
				holder.mListShade.setOnTouchListener(new OnTouchListener() {			
					@Override
					public boolean onTouch(View view, MotionEvent motionevent) {
						// TODO Auto-generated method stub
						return true;
					}
				});
				
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder)convertView.getTag();
			}
			
			//修改数据信息(含加密标识符)
			holder.mInfo = getItem(position);
			//Log.d(tag, "index : " + position);
//			holder.mChannelName.setText(holder.mInfo.getDeviceCode() + "-" + holder.mInfo.getIndex());
			if (holder.mInfo.getEncryptMode() == 1) {
				holder.mChannelName.setText(holder.mInfo.getName() + " - Encrypt");
			} else {
				holder.mChannelName.setText(holder.mInfo.getName());
			}
		
			//加载背景图片		
			holder.mBgDevice.setBackgroundResource(R.drawable.list_bg_device);			
			if(holder.mInfo.getBackgroudImgURL() != null && holder.mInfo.getBackgroudImgURL().length() > 0){
				//下载 注：由于有些请求的图片是经过加密处理的，所以需要使用带有解密功能的接口，默认密码为设备序列号
				ImageHelper.loadCacheImage(holder.mInfo.getBackgroudImgURL(),holder.mInfo.getDeviceCode(),holder.mInfo.getDeviceCode(), new Handler() {
					@Override
					public void handleMessage(Message msg) {
						// TODO Auto-generated method stub
						super.handleMessage(msg);				
						if (holder.mInfo.getBackgroudImgURL().hashCode() == msg.what && msg.obj != null) {
							holder.mBgDevice.setBackgroundDrawable((Drawable)msg.obj);
						}
					}
				});
			}
			
			//"在线" : "离线"
			if (holder.mInfo.getState() == ChannelInfo.ChannelState.Online) {
				holder.mListShade.setVisibility(View.GONE);
			} else {
				holder.mListShade.setVisibility(View.VISIBLE);
			}
			
			//是否自定义加密,效率不高
			if (holder.mInfo.getEncryptMode() == 1 && holder.mInfo.getEncryptKey() == null) {
				holder.mLiveVideo.setAlpha(128);
				holder.mLocalVideo.setAlpha(128);
				holder.mCloudVideo.setAlpha(128);
				holder.mMessage.setAlpha(128);
			} else {
				holder.mLiveVideo.setAlpha(255);
				holder.mLocalVideo.setAlpha(255);
				holder.mCloudVideo.setAlpha(255);
				holder.mMessage.setAlpha(255);
			}
			
			return convertView;
		}
	}
	
	static class ViewHolder{
		TextView 	    mChannelName;
		ImageView 	    mDelete;
		RelativeLayout  mBgDevice;
		ImageView 	    mLiveVideo;
		ImageView	    mLocalVideo;
		ImageView       mCloudVideo;
		ImageView       mMessage;
		ImageView       mSetting;
		LinearLayout    mListShade;
		ChannelInfo     mInfo;
	}
}
