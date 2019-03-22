package com.lechange.demo.message;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.business.entity.AlarmMessageInfo;
import com.lechange.demo.business.util.ImageHelper;
import com.lechange.demo.business.util.TaskPoolHelper;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;
import com.lechange.demo.common.DatePicker;
import com.lechange.demo.common.DatePicker.OnTimeClickListener;
import com.lechange.demo.common.ProgressDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class AlarmMessageActivity extends Activity {

	private String tag="AlarmMessageActivity";
	private String mChannelUUID = null;
	
	private DatePicker mDatePicker;
	private LinearLayout mDatePickerContainer; //放置DatePicker的容器
	private ListView mAlarmMsgListView;        //放置报警信息的listview
	private CommonTitle mCommonTitle;
	private ProgressDialog mProgressDialog;  //播放加载使用
	private AlarmMsgListAdapter mAlarmMsgAdapt;
	private List<AlarmMessageInfo> mAlarmMsgInfoList; // 保存报警信息
	
	private ImageView  mNoMsgImageView;               //无报警消息
	private ImageView mShowBigImageView;              //显示大图
	
	private String mStartTime;       //开始时间
	private String mEndTime;		 //结束时间

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_alarm_message);
		
		mChannelUUID = getIntent().getStringExtra("UUID");  //获取通道的UUID
		initTitle();
		//开启播放加载控件
		mProgressDialog = (ProgressDialog) this.findViewById(R.id.query_load);	
		mProgressDialog.setStart(getString(R.string.common_loading));
		
		initAlarmMsgListView();
		initBigImageView();


	}
	
	/**
	 * 初始化大图，并添加点击事件
	 * @note 当大图显示中，界面只显示此控件。点击此控件，该控件隐藏，mAlarmMsgListView与mCommonTitle显示
	 */
	public void initBigImageView(){
		mShowBigImageView = (ImageView) findViewById(R.id.showBigPhoto);
		mShowBigImageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				mProgressDialog.setStop();  //关闭加载窗口
				mShowBigImageView.setTag(0);
			
				mDatePickerContainer.setVisibility(View.VISIBLE);
				mAlarmMsgListView.setVisibility(View.VISIBLE);
				mCommonTitle.setVisibility(View.VISIBLE);
				mShowBigImageView.setVisibility(View.GONE);
			}
		});
	}

	/**
	 * 初始化标题栏，管理mDatePickerContainer
	 * @note 点击右边按钮，会弹出时间选择器
	 */
	public void initTitle() {
		mDatePickerContainer = (LinearLayout) findViewById(R.id.timerContainer);
		// 绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back,
				R.drawable.title_btn_search, R.string.alarm_message_name);

		mCommonTitle.setOnTitleClickListener(new OnTitleClickListener() {
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
					break;
				case CommonTitle.ID_RIGHT:
					// 添加时间选择控件
					Log.d(tag,tag + mDatePickerContainer.getChildCount());
					if (mDatePickerContainer.getChildCount() > 0) {
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
					mDatePickerContainer.addView(mDatePicker, lp);
					break;
				default:
					break;
				}
			}
		});
	}
	
	/**
	 * 初始化时间选择控件，主要是添加事件响应函数
	 */

	public void initDatePicker() {
		if (mDatePicker == null) {
			return;
		}

		mDatePicker.setOnTimeClickListener(new OnTimeClickListener() {

			@Override
			public void onCommonTimeClick(int id) {
				if (id == DatePicker.ID_LEFT) { // 点击左边
					mDatePickerContainer.removeView(mDatePicker);
				} else { // 点击右边
					Date time = mDatePicker.getSelectedDate();

					DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					String date = df.format(time);
					
					mStartTime = date + " 00:00:00";
					mEndTime   = date + " 23:59:59";
					
					mDatePickerContainer.removeView(mDatePicker);
					loadAlarmMsgList(mStartTime, mEndTime);
					//清空屏幕
					mAlarmMsgAdapt.notifyDataSetChanged();
					mNoMsgImageView.setVisibility(View.GONE);
					mProgressDialog.setStart(getString(R.string.common_loading));
				}

			}
		});
	}

	/**
	 * 初始化报警消息列表
	 */
	public void initAlarmMsgListView(){
		
		mAlarmMsgListView = (ListView) findViewById(R.id.messageList);
		mNoMsgImageView = (ImageView) findViewById(R.id.noMsgImg);
		
		mAlarmMsgAdapt = new AlarmMsgListAdapter(this);
		mAlarmMsgListView.setAdapter(mAlarmMsgAdapt);
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		mStartTime = df.format(new Date()) + " 00:00:00";
		mEndTime = df.format(new Date()) + " 23:59:59";
		
		loadAlarmMsgList(mStartTime,mEndTime);
	}
	
	/**
	 * 更新报警消息
	 * @param startTime  报警开始时间段
	 * @param endTime    报警结束时间段
	 */

	public void loadAlarmMsgList(String startTime, String endTime) {
		//查10条
		Business.getInstance().queryAlarmMessageList(mChannelUUID, startTime, endTime, 10, new Handler() {
			@SuppressWarnings("unchecked")
			@Override
			public void handleMessage(Message msg) {
				mProgressDialog.setStop();  //关闭加载窗口
				if (msg.what != 0) {
					Toast.makeText(AlarmMessageActivity.this, getString(R.string.toast_alarmmsg_query_failed) + msg.arg1, Toast.LENGTH_SHORT).show();
				} else if (msg.what == 0) {
					mAlarmMsgInfoList = (List<AlarmMessageInfo>) msg.obj;
					if (mAlarmMsgInfoList != null && mAlarmMsgInfoList.size() > 0) {
						mAlarmMsgAdapt.notifyDataSetChanged();
					} else {
						mNoMsgImageView.setVisibility(View.VISIBLE);
						//Toast.makeText(AlarmMessageActivity.this, "没有报警消息", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		TaskPoolHelper.clearTask();
	}

	class AlarmMsgListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;

		public AlarmMsgListAdapter(Context context) {
			this.mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mAlarmMsgInfoList != null ? mAlarmMsgInfoList.size():0;
		}

		@Override
		public AlarmMessageInfo getItem(int position) {
			return mAlarmMsgInfoList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		/**
		 * 删除某条报警信息
		 * @param info  
		 */
		private void deleteAlarmMsg(final AlarmMessageInfo info) {
			Business.getInstance().deleteAlarmMessage(info, new Handler() {

				@Override
				public void handleMessage(Message msg) {
					if (msg.what != 0) {
						Toast.makeText(AlarmMessageActivity.this, R.string.toast_alarmmsg_delete_failed,
								Toast.LENGTH_LONG).show();
					} else {
	 					//mAlarmMsgInfoList.remove(info);
						//mAlarmMsgAdapt.notifyDataSetChanged();
						
						loadAlarmMsgList(mStartTime,mEndTime);
						//清空屏幕
						mAlarmMsgAdapt.notifyDataSetChanged();
						mNoMsgImageView.setVisibility(View.GONE);
						mProgressDialog.setStart(getString(R.string.common_loading));
					}
				}
			});
		}
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(
						R.layout.activity_alarm_message_item, null);
				holder.photo = (ImageView) convertView.findViewById(R.id.photo);
				holder.timeText = (TextView) convertView
						.findViewById(R.id.alarmTime);
				holder.deleteButton = (ImageView) convertView
						.findViewById(R.id.delete);
				
				// 删除按钮添加响应事件
				holder.deleteButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						DialogInterface.OnClickListener dialogOnclicListener=new DialogInterface.OnClickListener(){		  
							@Override  
							public void onClick(DialogInterface dialog, int which) {  
								switch(which){  
								case Dialog.BUTTON_POSITIVE:  
									deleteAlarmMsg(holder.info);
									break;  
								case Dialog.BUTTON_NEGATIVE:  
									break;  
								case Dialog.BUTTON_NEUTRAL:  
									break;  
								}  
							}
						};  
						//dialog参数设置  
						AlertDialog.Builder builder=new AlertDialog.Builder(AlarmMessageActivity.this);  //先得到构造器  
						builder.setTitle(R.string.alarm_message_delete_dialog_title); //设置标题  
						builder.setMessage(R.string.alarm_message_delete_dialog_message); //设置内容  
						builder.setPositiveButton(R.string.dialog_positive,dialogOnclicListener);
						builder.setNegativeButton(R.string.dialog_nagative,dialogOnclicListener);
						builder.create().show();
					}
				});

				holder.photo.setOnClickListener(new OnClickListener() {			
					@Override
					public void onClick(View arg0) {
						mAlarmMsgListView.setVisibility(View.GONE);
						mCommonTitle.setVisibility(View.GONE);
						mDatePickerContainer.setVisibility(View.GONE);
						
						mProgressDialog.setStart(getString(R.string.common_loading));
						mShowBigImageView.setTag(holder.info.getPicUrl().hashCode());
						mShowBigImageView.setImageDrawable(null);
						mShowBigImageView.setVisibility(View.VISIBLE);
						if(holder.info.getPicUrl() != null && holder.info.getPicUrl().length() > 0)
							ImageHelper.loadRealImage(holder.info.getPicUrl(), holder.info.getDeviceId(),holder.info.getDeviceKey() != null ? holder.info.getDeviceKey() : holder.info.getDeviceId(), new Handler(){
								@Override
								public void handleMessage(Message msg) {							
									super.handleMessage(msg);
									if ((Integer)mShowBigImageView.getTag() == msg.what) {
										mProgressDialog.setStop();  //关闭加载窗口
										if (msg.obj != null) {
											mShowBigImageView.setImageDrawable((Drawable)msg.obj);
										} else {
											Toast.makeText(AlarmMessageActivity.this, R.string.toast_alarmmsg_load_failed, Toast.LENGTH_SHORT).show();
										}
									}
								}
							});		
					}
				});

				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			//修改数据信息
			holder.info = getItem(position);
			Log.d("", "index : " + position);
			
			//背景\holder.photo图片清空
			holder.photo.setBackgroundResource(R.drawable.list_bg_device);
			if(holder.info.getThumbUrl() != null && holder.info.getThumbUrl().length() > 0)
				ImageHelper.loadCacheImage(holder.info.getThumbUrl(),holder.info.getDeviceId(),holder.info.getDeviceKey() != null ? holder.info.getDeviceKey() : holder.info.getDeviceId(), new Handler(){
					@Override
					public void handleMessage(Message msg) {							
						super.handleMessage(msg);
						if(holder.info.getThumbUrl().hashCode() == msg.what && msg.obj != null){
							//Log.d(tag, msg.obj.toString());
							holder.photo.setBackgroundDrawable((Drawable)msg.obj);
						}
					}
				});
			// 报警时间更新
			holder.timeText.setText(holder.info.getLocalDate());


			return convertView;
		}


	}
	
	 static class ViewHolder {
		public ImageView photo;
		public TextView timeText;
		public ImageView deleteButton;
		public AlarmMessageInfo info;
	}

}
