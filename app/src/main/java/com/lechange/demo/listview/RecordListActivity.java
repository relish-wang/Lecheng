package com.lechange.demo.listview;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.lechange.common.crypt.Encrypter;
import com.lechange.common.log.Logger;
import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.business.Business.CloudStorageCode;
import com.lechange.demo.business.Business.LocalDownloadCode;
import com.lechange.demo.business.entity.RecordInfo;
import com.lechange.demo.business.util.ImageHelper;
import com.lechange.demo.business.util.MediaPlayHelper;
import com.lechange.demo.business.util.TaskPoolHelper;
import com.lechange.demo.business.util.TimeHelper;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;
import com.lechange.demo.common.DatePicker;
import com.lechange.demo.common.DatePicker.OnTimeClickListener;
import com.lechange.demo.common.ProgressDialog;
import com.lechange.demo.mediaplay.MediaPlayActivity;
import com.lechange.opensdk.listener.LCOpenSDK_DownloadListener;
import com.lechange.opensdk.media.LCOpenSDK_Download;
import com.lechange.opensdk.utils.MD5Utils;

import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordListActivity extends FragmentActivity {
	private final String tag = "RecordList";
	
	private ListView mListview = null;
	private CommonTitle mCommonTitle;
	private DatePicker mDatePicker;
	private LinearLayout mViewContainer; 	   // 放置DatePicker的容器
	private ProgressDialog mProgressDialog;    //请求加载使用
	private RecrodListAdapter mRecordListAdapt;
	private List<RecordInfo> mRecordList;
	private String mChannelUUID = null;
	private int mType;
	private int mIndex = -1;				   //当前正在下载的索引号,目前仅支持单个下载，日后会扩展多个下载
	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			//不显示进度
			int firstIndex = mListview.getFirstVisiblePosition();
			int lastIndex = mListview.getLastVisiblePosition();
			for (int i = firstIndex; i <= lastIndex; i++) {
				ViewHolder holder = (ViewHolder)mListview.getChildAt(i - firstIndex).getTag();
				//渲染下载更新
				android.view.ViewGroup.LayoutParams params = holder.mDownload_bg.getLayoutParams();
				if (holder.mInfo.isDownload()) {
					if (holder.mInfo.getDownLength() > 0) {
						if(holder.mInfo.getFileLength() > 0)
							params.width = (int)(holder.mDownload_icon.getWidth() / (holder.mInfo.getFileLength() / holder.mInfo.getDownLength()));
						else
							params.width = 0;
					}
				} else {
					params.width = 0;
					holder.mDownload_icon.setText(R.string.download);
				}
				holder.mDownload_bg.setLayoutParams(params);
			}
			sendEmptyMessageDelayed(0, 1000);
		};
	}; //定时器，每500ms刷新一次，
	
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_record_list);
		Intent intent = getIntent();
		mChannelUUID = intent.getStringExtra("UUID");
		mType = intent.getIntExtra("TYPE", 0);
		Log.d(tag, mType + "::mChannelUUID:"+mChannelUUID);
		
		//绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, R.drawable.title_btn_search, intent.getIntExtra("MEDIA_TITLE", 0));
		
		mCommonTitle.setOnTitleClickListener(new OnTitleClickListener() {		
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
					break;
				case CommonTitle.ID_RIGHT:
					//添加时间选择控件
					Log.d("Business","Business" + mViewContainer.getChildCount());
					if(mViewContainer.getChildCount()>0){	
						return;
					}

					// undo 添加datepicker
					if (mDatePicker == null) {
						mDatePicker = new DatePicker(getApplicationContext());
						initDatePicker();
					}
					
					LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.MATCH_PARENT);
					mViewContainer.addView(mDatePicker, lp);
					break;
				default:
					break;
				}
			}
		});
		
		//日期控件
		mViewContainer = (LinearLayout) findViewById(R.id.timerContainer);
		
		//开启请求加载控件
		mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);	
		mProgressDialog.setStart(getString(R.string.common_loading));
			
		//绘制list
		mListview = (ListView)this.findViewById(R.id.list_records);	
		mRecordListAdapt = new RecrodListAdapter(this);
		mListview.setAdapter(mRecordListAdapt);
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		String startTime = df.format(new Date()) + " 00:00:00";
		String endTime = df.format(new Date()) + " 23:59:59";
		//初始化数据
		loadRecrodList(startTime,endTime);
		//设置监听
		setItemClick();
		
		//设置云录像下载监听
		if (mType == MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD) {
			LCOpenSDK_Download.setListener(new CloudDownloadListener());
		}
		else{
			LCOpenSDK_Download.setListener(new LocalDownloadListener());
		}
		//开启定时刷新
		mHandler.obtainMessage().sendToTarget();

	}
	
	public void initDatePicker() {
		if (mDatePicker == null) {
			return;
		}

		mDatePicker.setOnTimeClickListener(new OnTimeClickListener() {
			@Override
			public void onCommonTimeClick(int id) {
				if (id == DatePicker.ID_LEFT) { // 点击左边
					mViewContainer.removeView(mDatePicker);
				} else { // 点击右边
					
					if (mIndex != -1) {
						LCOpenSDK_Download.stopDownload(mIndex); //重新加载前停止下载
						MediaPlayHelper.deleteDownloadVideo(mRecordList.get(mIndex).getRecordID(), mRecordList.get(mIndex).getStartTime());
						//屏蔽操作
						mIndex = -1;
					}
					
					Date time = mDatePicker.getSelectedDate();
					DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					String date = df.format(time);
					
					String startTime = date + " 00:00:00";
					String endTime   = date + " 23:59:59";
					
					mViewContainer.removeView(mDatePicker);
					loadRecrodList(startTime, endTime);
					//清空屏幕
					if(mRecordList != null)
						mRecordList.clear(); //清数据 临时使用
					mRecordListAdapt.notifyDataSetChanged();
					
					RecordListActivity.this.findViewById(R.id.list_records_novideo).setVisibility(View.GONE);
					mProgressDialog.setStart(getString(R.string.common_loading));
				}

			}
		});
	}
	
	public void setItemClick(){				
		//单个录像监听
		mListview.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> adapterview, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				if (mIndex != -1) {
					Toast.makeText(RecordListActivity.this, R.string.toast_recordlist_onlyone, Toast.LENGTH_SHORT).show();
					return;
				}
				Log.d(tag, "click:"+ position+"-" + id + "ID:" + view.getId());
				Intent intent = new Intent(RecordListActivity.this, MediaPlayActivity.class);
				switch(mType){
				case MediaPlayActivity.IS_VIDEO_REMOTE_RECORD:
					intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_RECORD);
					break;
				case MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD:
	                intent.putExtra("TYPE", MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD);
	                break;
	            default:
	            	break;
				}
				intent.putExtra("ID", mRecordList.get(position).getId());
                intent.putExtra("MEDIA_TITLE", R.string.record_play_name);
                RecordListActivity.this.startActivity(intent);
			}
			
		});
	}
	
	
	public void loadRecrodList(final String startTime,final String endTime){
		switch(mType){
		case MediaPlayActivity.IS_VIDEO_REMOTE_RECORD:
			//查询1天之内的后10条录像
			Business.getInstance().queryRecordNum(mChannelUUID, startTime, 
					endTime, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what != 0) {
						mProgressDialog.setStop();  //关闭加载窗口
						Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_query_failed) + msg.what, Toast.LENGTH_SHORT).show();
					} else if (msg.what == 0) {
						if(msg.arg1 > 0){
							Business.getInstance().queryRecordList(mChannelUUID, startTime, 
									endTime, msg.arg2, msg.arg1, new Handler() {
								@SuppressWarnings("unchecked")
								@Override
								public void handleMessage(Message msg) {
									mProgressDialog.setStop();  //关闭加载窗口
									if (msg.what != 0) {
										Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_query_failed) + msg.obj, Toast.LENGTH_SHORT).show();
									} else {
										mRecordList = (List<RecordInfo>) msg.obj;				
										if (mRecordList != null && mRecordList.size() > 0) {
											Log.d(tag,"loadRecrodList mRecordList.size:"+mRecordList.size());
											mRecordListAdapt.notifyDataSetChanged();
										}else {
											RecordListActivity.this.findViewById(R.id.list_records_novideo).setVisibility(View.VISIBLE);
											//Toast.makeText(RecordListActivity.this, "没有录像", Toast.LENGTH_SHORT).show();
										}
									}
								}
							});
						} else {
							mProgressDialog.setStop();  //关闭加载窗口
							RecordListActivity.this.findViewById(R.id.list_records_novideo).setVisibility(View.VISIBLE);
						}
					}
				}
			});
			break;
		case MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD:
			//查询1天之内的前10条录像
			Business.getInstance().queryCloudRecordNum(mChannelUUID, startTime, 
					endTime, new Handler() {
				@Override
				public void handleMessage(Message msg) {
					if (msg.what != 0) {
						mProgressDialog.setStop();  //关闭加载窗口
						Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_query_failed) + msg.what, Toast.LENGTH_SHORT).show();
					} else if (msg.what == 0) {
						if(msg.arg1 > 0){
							Business.getInstance().queryCloudRecordList(mChannelUUID, startTime, 
									endTime, msg.arg2, msg.arg1, new Handler() {
								@Override
								public void handleMessage(Message msg) {
									mProgressDialog.setStop(); //关闭加载窗口
									if (msg.what != 0) {
										Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_query_failed) + msg.arg1, Toast.LENGTH_SHORT).show();
									} else {
										mRecordList = (List<RecordInfo>) msg.obj;				
										if (mRecordList != null && mRecordList.size() > 0) {
											Log.d(tag,"loadRecrodList mRecordList.size:"+mRecordList.size());
											mRecordListAdapt.notifyDataSetChanged();
										}else {
											RecordListActivity.this.findViewById(R.id.list_records_novideo).setVisibility(View.VISIBLE);
											//Toast.makeText(RecordListActivity.this, "没有录像", Toast.LENGTH_SHORT).show();
										}
									}
								}
							});
						}else {
							mProgressDialog.setStop();  //关闭加载窗口
							RecordListActivity.this.findViewById(R.id.list_records_novideo).setVisibility(View.VISIBLE);
						}
					}
				}
			});
			break;
		default:
			break;	
		}
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		if (mIndex != -1) {
			LCOpenSDK_Download.stopDownload(mIndex);
			MediaPlayHelper.deleteDownloadVideo(mRecordList.get(mIndex).getRecordID(), mRecordList.get(mIndex).getStartTime());
			mRecordList.get(mIndex).setDownLength(-1);
			//屏蔽操作
			mIndex = -1;
		}
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TaskPoolHelper.clearTask();		
	}
	
	private class RecrodListAdapter extends BaseAdapter
	{
		private LayoutInflater mInflater;
		public RecrodListAdapter(Context context){
			mInflater = LayoutInflater.from(context);
		}
		@Override
		public int getCount() {
			return mRecordList != null ? mRecordList.size():0;
		}

		@Override
		public RecordInfo getItem(int position) {
			return mRecordList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			
			if(convertView == null){
				convertView = mInflater.inflate(R.layout.activity_record_list_item, null);
				holder = new ViewHolder();
				holder.mBgVideo = (RelativeLayout) convertView.findViewById(R.id.list_bg_video);
				holder.mRecordTime = (TextView) convertView.findViewById(R.id.list_record_time);
				//云录像加载下载按钮
//				if (mType == MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD) {
					holder.mDownload = (FrameLayout) convertView.findViewById(R.id.list_record_download);
					holder.mDownload_bg = convertView.findViewById(R.id.record_download_bg);
					holder.mDownload_icon = (TextView) convertView.findViewById(R.id.record_download_icon);
					holder.mDownload.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View arg0) {
							// TODO Auto-generated method stub
							if (mIndex == -1 || mIndex == holder.mPosition)
								if (holder.mDownload_icon.getText().toString().equals(getString(R.string.download))) {
									//置为可以取消状态
									holder.mDownload_icon.setText(R.string.cancel);
									holder.mInfo.setDownLength(0);
									
									String encryptKey = holder.mInfo.getDeviceKey() != null ? holder.mInfo.getDeviceKey() : holder.mInfo.getDeviceId();
									if(mType == MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD){
										LCOpenSDK_Download.startDownload(holder.mPosition, 
											MediaPlayHelper.getDownloadVideoPath(0, String.valueOf(holder.mPosition), holder.mInfo.getStartTime()), 
											Business.getInstance().getToken(),
											holder.mInfo.getRecordID(), 
											holder.mInfo.getDeviceId(), 
											String.valueOf(0), 
											encryptKey, 
											1000, 
											5000);
									}else {
										LCOpenSDK_Download.startDownload(Business.getInstance().getToken(), 
												holder.mInfo.getDeviceId(), 
												holder.mPosition, 
												MediaPlayHelper.getDownloadVideoPath(1, String.valueOf(holder.mPosition), holder.mInfo.getStartTime()),
												holder.mInfo.getRecordID(),
												encryptKey, 
												0, //默认偏移为0
												0, //mp4格式
												16.0f);
									}
									//屏蔽操作
									mIndex = holder.mPosition;
								} else {
									//置为可以下载状态
									android.view.ViewGroup.LayoutParams params = holder.mDownload_bg.getLayoutParams();
									params.width = 0;
									holder.mDownload_bg.setLayoutParams(params);
									holder.mDownload_icon.setText(R.string.download);
									holder.mInfo.setDownLength(-1);
									LCOpenSDK_Download.stopDownload(holder.mPosition);
									
									//删除文件
									MediaPlayHelper.deleteDownloadVideo(String.valueOf(holder.mPosition), holder.mInfo.getStartTime());
									//屏蔽操作
									mIndex = -1;
								}
							else
								Toast.makeText(RecordListActivity.this, R.string.toast_recordlist_onlyone, Toast.LENGTH_SHORT).show();
						}
					});
//				}
				
				convertView.setTag(holder);
			}else{
				holder = (ViewHolder)convertView.getTag();
			}
			
			//修改数据信息
			holder.mPosition = position;
			holder.mInfo = getItem(position);
			//Log.d(tag, "index : " + position);
			
			android.view.ViewGroup.LayoutParams params = holder.mDownload_bg.getLayoutParams();
			switch (mType) {
			case MediaPlayActivity.IS_VIDEO_REMOTE_RECORD:
				// 屏蔽平台返回的录像大小=0情况（easy4ip设备返回录像大小为0）
				if (holder.mInfo.getFileLength() > 0) {
					holder.mDownload.setVisibility(View.VISIBLE);
				} else {
					holder.mDownload.setVisibility(View.INVISIBLE);
				}
				
				//渲染下载更新
				if (holder.mInfo.isDownload()) {
					holder.mDownload_icon.setText(R.string.cancel);
					if (holder.mInfo.getDownLength() > 0) {		
						params.width = (int)(holder.mDownload_icon.getWidth() / (holder.mInfo.getFileLength() / holder.mInfo.getDownLength()));			
					} else {
						params.width = 0;
					}
				} else {
					holder.mDownload_icon.setText(R.string.download);
					params.width = 0;
				}
				holder.mDownload_bg.setLayoutParams(params);
				
				//加载背景图片,设备录像，不用解密
				holder.mBgVideo.setBackgroundResource(R.drawable.list_bg_video);
				if(holder.mInfo.getBackgroudImgUrl() != null && holder.mInfo.getBackgroudImgUrl().length() > 0){
					//下载
					ImageHelper.loadCacheImage(holder.mInfo.getBackgroudImgUrl(), new Handler(){
						@Override
						public void handleMessage(Message msg) {
							// TODO Auto-generated method stub
							super.handleMessage(msg);				
							if(holder.mInfo.getBackgroudImgUrl().hashCode() == msg.what && msg.obj != null){
									holder.mBgVideo.setBackgroundDrawable((Drawable)msg.obj);
							}
						}
					});
				}
				break;
			case MediaPlayActivity.IS_VIDEO_REMOTE_CLOUD_RECORD:
				// 屏蔽平台返回的录像大小=0情况（easy4ip设备返回录像大小为0）
				if (holder.mInfo.getFileLength() > 0) {
					holder.mDownload.setVisibility(View.VISIBLE);
				} else {
					holder.mDownload.setVisibility(View.INVISIBLE);
				}

				//渲染下载更新
				if (holder.mInfo.isDownload()) {
					holder.mDownload_icon.setText(R.string.cancel);
					if (holder.mInfo.getDownLength() > 0) {		
						params.width = (int)(holder.mDownload_icon.getWidth() / (holder.mInfo.getFileLength() / holder.mInfo.getDownLength()));			
					} else {
						params.width = 0;
					}
				} else {
					holder.mDownload_icon.setText(R.string.download);
					params.width = 0;
				}
				holder.mDownload_bg.setLayoutParams(params);
				//加载背景图片,云录像,要解密
				holder.mBgVideo.setBackgroundResource(R.drawable.list_bg_device);			
				if(holder.mInfo.getBackgroudImgUrl() != null && holder.mInfo.getBackgroudImgUrl().length() > 0){
					//下载
					ImageHelper.loadCacheImage(holder.mInfo.getBackgroudImgUrl(),holder.mInfo.getDeviceId(), holder.mInfo.getDeviceKey() != null ? holder.mInfo.getDeviceKey() : holder.mInfo.getDeviceId(), new Handler(){
						@Override
						public void handleMessage(Message msg) {
							// TODO Auto-generated method stub
							super.handleMessage(msg);
								if(holder.mInfo.getBackgroudImgUrl().hashCode() == msg.what && msg.obj != null){
									holder.mBgVideo.setBackgroundDrawable((Drawable)msg.obj);	
								}
							}
					});
				}
				break;
			default:
				break;
			}
			
			//时间	
			holder.mRecordTime.setText(TimeHelper.getDateHMS(holder.mInfo.getStartTime())+"--"+TimeHelper.getDateHMS(holder.mInfo.getEndTime()));
		
			return convertView;
		}
	}
	static class ViewHolder{
		int				mPosition;
		RelativeLayout 	mBgVideo;
		TextView		mRecordTime;
		RecordInfo      mInfo;
		FrameLayout		mDownload;
		View			mDownload_bg;
		TextView		mDownload_icon;
	}
	
	private class CloudDownloadListener extends LCOpenSDK_DownloadListener{
		@Override
		public void onDownloadReceiveData(int index, int dataLen) {
			// TODO Auto-generated method stub
			if (mRecordList.size() != 0) {
				RecordInfo info = mRecordList.get((int)index);
				info.setDownLength(info.getDownLength() + dataLen);
				Logger.d(tag, "downLen:"+info.getDownLength());
			}
		}
		
		@Override
		public void onDownloadState(final int index, String code, int Type) {
			// TODO Auto-generated method stub
			if (Type == Business.RESULT_SOURCE_OPENAPI
					|| code.equals(CloudStorageCode.HLS_DOWNLOAD_FAILD)
					|| code.equals(CloudStorageCode.HLS_SEEK_FAILD)
					|| code.equals(CloudStorageCode.HLS_KEY_ERROR)) {
				//重置为可以下载状态
				mRecordList.get((int) index).setDownLength(-1);
				if (mHandler != null) {
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_failed) + ",index : " + index, Toast.LENGTH_SHORT).show();
						}
					});
				}
				//删除下载到一半的文件
				MediaPlayHelper.deleteDownloadVideo(String.valueOf(index), mRecordList.get(index).getStartTime());
				//屏蔽操作
				mIndex = -1;
				
			}
			if (code.equals(CloudStorageCode.HLS_DOWNLOAD_END)) {
				Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_end) + ",index : " + index, Toast.LENGTH_SHORT).show();
				//重置为可以下载状态
				mRecordList.get((int) index).setDownLength(-1);
				if (mHandler != null) {
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_end) + ",index : " + index, Toast.LENGTH_SHORT).show();
						}
					});
				}
				//通知图库刷新
				MediaScannerConnection.scanFile(RecordListActivity.this, 
						new String[] { MediaPlayHelper.getDownloadVideoPath(0, String.valueOf(index), mRecordList.get(index).getStartTime()) }, null, null);
				//屏蔽操作
				mIndex = -1;
			}
		}
	}
	
	private class LocalDownloadListener extends LCOpenSDK_DownloadListener{
		@Override
		public void onDownloadReceiveData(int index, int dataLen) {
			// TODO Auto-generated method stub
			if (mRecordList.size() != 0) {
				RecordInfo info = mRecordList.get((int)index);
				info.setDownLength(info.getDownLength() + dataLen);
				
				Logger.d(tag, "downLen:"+info.getDownLength());
			}
		}
		
		@Override
		public void onDownloadState(final int index, String code, int Type) {
			// TODO Auto-generated method stub
			if (Type == Business.RESULT_SOURCE_OPENAPI
					|| code.equals(LocalDownloadCode.RTSP_DOWNLOAD_FRAME_ERROR)
					|| code.equals(LocalDownloadCode.RTSP_DOWNLOAD_TEARDOWN)
					|| code.equals(LocalDownloadCode.RTSP_DOWNLOAD_AUTHORIZATION_FAIL)
					|| code.equals(LocalDownloadCode.RTSP_DOWNLOAD_KEY_MISMATH)) {
				//重置为可以下载状态
				mRecordList.get((int) index).setDownLength(-1);
				if (mHandler != null) {
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_failed) + ",index : " + index, Toast.LENGTH_SHORT).show();
						}
					});
				}
				//删除下载到一半的文件
				MediaPlayHelper.deleteDownloadVideo(String.valueOf(index), mRecordList.get(index).getStartTime());
				//屏蔽操作
				mIndex = -1;
				
			}
			if (code.equals(LocalDownloadCode.RTSP_DOWNLOAD_OVER)) {
				Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_end) + ",index : " + index, Toast.LENGTH_SHORT).show();
				//重置为可以下载状态
				mRecordList.get((int) index).setDownLength(-1);
				LCOpenSDK_Download.stopDownload(index);
				if (mHandler != null) {
					mHandler.post(new Runnable() {
						public void run() {
							Toast.makeText(RecordListActivity.this, getString(R.string.toast_recordlist_download_end) + ",index : " + index, Toast.LENGTH_SHORT).show();
						}
					});
				}
				//通知图库刷新
				MediaScannerConnection.scanFile(RecordListActivity.this, 
						new String[] { MediaPlayHelper.getDownloadVideoPath(1, String.valueOf(index), mRecordList.get(index).getStartTime()) }, null, null);
				//屏蔽操作
				mIndex = -1;
			}
		}
	}

}
