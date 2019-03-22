/*  
 * 项目名: YYS 
 * 文件名: MediaPlayBackFragment.java  
 * 版权声明:
 *      本系统的所有内容，包括源码、页面设计，文字、图像以及其他任何信息，
 *      如未经特殊说明，其版权均属大华技术股份有限公司所有。
 *      Copyright (c) 2015 大华技术股份有限公司
 *      版权所有
 */
package com.lechange.demo.mediaplay.fragment;

import java.io.FileOutputStream;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.business.Business.CloudStorageCode;
import com.lechange.demo.business.Business.PlayerResultCode;
import com.lechange.demo.business.entity.ChannelInfo;
import com.lechange.demo.business.entity.RecordInfo;
import com.lechange.demo.business.util.TimeHelper;
import com.lechange.demo.common.ProgressDialog;
import com.lechange.demo.common.RecoderSeekBar;
import com.lechange.opensdk.listener.LCOpenSDK_EventListener;


/**
 * 描述：远程录像回放（目前包括录像回放和告警消息两个场景） 作者： lc
 */
public class MediaPlayBackFragment extends MediaPlayFragment implements OnClickListener{
	private final String tag = "MediaPlayBackFragment";
	private RecordInfo recordInfo;
	private ChannelInfo channelInfo;
	
	//SeekBar使用
	private int progress;
	private String beginTime;
	static private boolean sthls = false;
	private Queue<Long> mSeekQueue = new LinkedList<Long>();  //云录像时间记忆

	
	private LinearLayout mRecordMenu;
	private ImageView mRecordPlayPause;
	private TextView mRecordStartTime;
	private RecoderSeekBar mRecordSeekbar;
	private TextView mRecordEndTime;
	private ImageView mRecordScale;
	
	private enum PlayStatus{play_close, play_open, play_opening, play_pause};
	private PlayStatus mOpenPlay = PlayStatus.play_close; //语音对讲状态
	
	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#onCreate(android.os.Bundle)
	 *      描述：获取录像通道参数
	 * @param savedInstanceState
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle b = getArguments();
		if (b != null) {
			String id = b.getString("RESID");
			recordInfo = Business.getInstance().getRecord(id);
			channelInfo = Business.getInstance().getChannel(recordInfo.getChnlUuid());
		}
		if (recordInfo == null && channelInfo == null){
			toast(getString(R.string.toast_playback_no_records));
			getActivity().finish();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mView = inflater.inflate(R.layout.fragment_media_record, container, false);
		//必须赋值，父类需要使用到
		mSurfaceParentView = (ViewGroup) mView.findViewById(R.id.record_window);
		//初始化窗口大小
		LayoutParams mLayoutParams = (LayoutParams) mSurfaceParentView.getLayoutParams();
		DisplayMetrics metric = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
		mLayoutParams.width = metric.widthPixels; // 屏幕宽度（像素）
		mLayoutParams.height = metric.widthPixels * 9 / 16;
		mLayoutParams.setMargins(0, 10, 0, 0);
		mSurfaceParentView.setLayoutParams(mLayoutParams);
		
		mPlayWin.initPlayWindow(this.getActivity(), (ViewGroup) mView.findViewById(R.id.record_window_content),0);
		
		mProgressDialog = (ProgressDialog) mView.findViewById(R.id.record_play_load);
		mReplayTip = (TextView) mView.findViewById(R.id.record_play_pressed);
		
		mRecordMenu = (LinearLayout) mView.findViewById(R.id.record_menu);
		mRecordPlayPause = (ImageView) mView.findViewById(R.id.record_play_pause);
		mRecordStartTime = (TextView) mView.findViewById(R.id.record_startTime);
		mRecordSeekbar = (RecoderSeekBar) mView.findViewById(R.id.record_seekbar);
		mRecordEndTime = (TextView) mView.findViewById(R.id.record_endTime);
		mRecordScale = (ImageView) mView.findViewById(R.id.record_scale);
		
		mReplayTip.setOnClickListener(this);
		mRecordPlayPause.setOnClickListener(this);
		mRecordScale.setOnClickListener(this);
		return mView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//SeekBar监听
		setSeekBarListener();
		//禁止拖动
		setCanSeekChanged(false);
		
		listener = new MyBasePlayerEventListener();
		mPlayWin.setWindowListener(listener);
		mPlayWin.openTouchListener();
		
		//开启横竖屏切换
		startListener();
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		initSeekBarAndTime();
		play(-1);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		stop(-1); //无效资源号
	}
	
	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		mPlayWin.uninitPlayWindow();// 销毁底层资源
	}
	
	/**
	 * @see com.dahua.hsviewclientopendemo.mediaplay.MediaPlayFragment#resetViews(android.content.res.Configuration)
	 *      描述：实现个性化界面
	 * @param mConfiguration
	 */
	@Override
	protected void resetViews(Configuration mConfiguration) {
		super.resetViews(mConfiguration);
		if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 横屏
			mRecordScale.setTag("LANDSCAPE");
			mRecordScale.setImageResource(R.drawable.record_btn_smallscreen);
		} else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			mRecordScale.setTag("PORTRAIT");
			mRecordScale.setImageResource(R.drawable.record_btn_fullscreen);
		}
			
	}
	
	private void initSeekBarAndTime() {		
		String mStartTime = TimeHelper.getTimeHMS(recordInfo.getStartTime());
		String mEndTime = TimeHelper.getTimeHMS(recordInfo.getEndTime());	
		mRecordSeekbar.setMax((int) ((recordInfo.getEndTime() - recordInfo.getStartTime()) / 1000));
		mRecordSeekbar.setProgress(0);			
		mRecordStartTime.setText(mStartTime);
		mRecordEndTime.setText(mEndTime);
	}

	private void setSeekBarListener() {
		mRecordSeekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mRecordSeekbar.getMax() - MediaPlayBackFragment.this.progress <= 2) {
					toast(getString(R.string.toast_playback_no_iframe));
					seek(mRecordSeekbar.getMax() >= 2 ? mRecordSeekbar.getMax() - 2 : 0);
				} else {
					seek(MediaPlayBackFragment.this.progress);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean byUser) {
				if (byUser) {
					MediaPlayBackFragment.this.progress = progress;
				}
			}
		});
	}

	class MyBasePlayerEventListener extends LCOpenSDK_EventListener{
		@Override
		public void onPlayerResult(int index,String code, int type) {
			System.out.println("winIndex:"+"code:"+code+"type:"+type);
			if(type == Business.RESULT_SOURCE_OPENAPI){
				if (mHander != null) {
					mHander.post(new Runnable() {
						@Override
						public void run() {
							if (isAdded()) {
								System.out.println("save stop");
								stop(R.string.video_monitor_play_error);
							}
						}
					});
				}
			}else if (recordInfo != null && recordInfo.getType() == RecordInfo.RecordType.PublicCloud) {
				if (code.equals(CloudStorageCode.HLS_DOWNLOAD_FAILD) ||
						code.equals(CloudStorageCode.HLS_KEY_ERROR)) {
					if (mHander != null) {
						mHander.post(new Runnable() {
							@Override
							public void run() {
								if (isAdded()) {
									System.out.println("save stop");
									stop(R.string.video_monitor_play_error);
								}
							}
						});
					}
				}else if(code.equals(CloudStorageCode.HLS_SEEK_FAILD)){	
					if (mHander != null) {
						mHander.post(new Runnable() {
							@Override
							public void run() {
								if (isAdded()) {
									stop(R.string.video_monitor_seek_error);
								}
							}
						});
					}
				}
				return;
			}else if(code.equals(PlayerResultCode.STATE_RTSP_TEARDOWN_ERROR)) {
				if (mHander != null) {
					mHander.post(new Runnable() {
						@Override
						public void run() {
							if (isAdded()) {
								stop(R.string.video_monitor_online_fail_restart);
							}
						}
					});
				}
			}else if(code.equals(PlayerResultCode.STATE_PACKET_FRAME_ERROR) ||
					code.equals(PlayerResultCode.STATE_RTSP_AUTHORIZATION_FAIL) ||
					code.equals(PlayerResultCode.STATE_RTSP_KEY_MISMATCH)){
				if (mHander != null) {
					mHander.post(new Runnable() {
						public void run() {
							if (isAdded()) {
								stop(R.string.video_monitor_play_error);
							}
						}
					});
				}
			}
		}
		
		@Override
		public void onStreamCallback(int index, byte[] data, int len){
//			Log.d(tag, "LCOpenSDK_EventListener::onStreamCallback-size : " + len);
			try {
				String path = Environment.getExternalStorageDirectory().getPath() + "/streamCallback.ts";
				FileOutputStream fout = new FileOutputStream(path, true);
				fout.write(data);
				fout.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		@Override
		public void onPlayBegan(int index) {
			// TODO Auto-generated method stub
			if (recordInfo.getType() == RecordInfo.RecordType.PublicCloud){
				//if(code.equals(CloudStorageCode.HLS_DOWNLOAD_BEGIN)){
				// 云录像播放第二步
				System.out.println(tag+ "*****+play pulic cloud video from CloudStorageCode.HLS_DOWNLOAD_BEGIN");
				if (mSeekQueue.size() > 0) { // 从上次停止的地方播放
					long time = mSeekQueue.poll();
					System.out.println(tag+ "*****onPlay+play pulic cloud video from time:"+ time);
					mSeekQueue.clear();
					mPlayWin.seek(time);
				}					
//				// 显示码率
//				if (mHander != null) {
//					mHander.post(MediaPlayBackFragment.this);
//				}
				mOpenPlay = PlayStatus.play_open;
				openAudio();//默认打开声音
				//关闭播放加载控件
				mProgressDialog.setStop();
			}else{
				//if(code.equals(PlayerResultCode.STATE_RTSP_PLAY_READY)){
//				// 显示码率
//				if (mHander != null) {
//					mHander.post(MediaPlayBackFragment.this);
//				}
				mOpenPlay = PlayStatus.play_open;
				
				openAudio();//默认打开声音
				//关闭播放加载控件
				mProgressDialog.setStop();
			}
		}
		
		
		
		@Override
		public void onPlayFinished(int index) {
			// TODO Auto-generated method stub
			if (mHander != null) {
				mHander.post(new Runnable() {
					public void run() {
						if (isAdded()) {
							stop(R.string.video_monitor_online_restart);
						}
					}
				});
			}
		}
//		@Override
//		public void onReceiveData( int len) {
//			// 流量统计
//			mTotalFlow += len;
//		}		
		@Override
		public void onPlayerTime(int index,final long current) {
			System.out.println("timetest"+current);
			long startTime;
			if (sthls){
				startTime = 0;
			}else{
				startTime = recordInfo.getStartTime() / 1000;
			}
			MediaPlayBackFragment.this.progress = (int) (current - startTime);
			if (mHander != null) {
				mHander.post(new Runnable() {
					@Override
					public void run() {
						if(sthls){
							if (isAdded()) {
								mRecordSeekbar.setProgress(MediaPlayBackFragment.this.progress);
								MediaPlayBackFragment.this.beginTime = TimeHelper.getTimeHMS(current*1000);
								int bh = (int)current/3600;
								int bm = (int)current/60%60;
								int bs = (int)current%60;
								MediaPlayBackFragment.this.beginTime = String.format(Locale.US, "%02d:%02d:%02d", bh, bm, bs);
								System.out.println("onPlayerTimetest:"+MediaPlayBackFragment.this.beginTime);
								mRecordStartTime.setText(MediaPlayBackFragment.this.beginTime);
							}
						}else{
							if (isAdded()) {
								mRecordSeekbar.setProgress(MediaPlayBackFragment.this.progress);
								MediaPlayBackFragment.this.beginTime = TimeHelper.getTimeHMS(current*1000);
								mRecordStartTime.setText(MediaPlayBackFragment.this.beginTime);
							}
						}
					}
				});
			}
		}
		
		@Override
		public void onWindowDBClick(int index,float dx, float dy) {
			// TODO Auto-generated method stub
			switch(mRecordMenu.getVisibility()){
			case View.GONE:
				mRecordMenu.setVisibility(View.VISIBLE);
				break;
			case View.VISIBLE:
				mRecordMenu.setVisibility(View.GONE);
				break;
			default:
				break;
			}	
		}
	}

	/**
	 * 描述：继续播放
	 */
	public void resume() {
		mPlayWin.resumeAsync();
		mOpenPlay = PlayStatus.play_open;
		mRecordPlayPause.setImageResource(R.drawable.record_btn_pause);
	}

	/**
	 *  描述：暂停播放
	 */
	public void pause() {
		mPlayWin.pauseAsync();
		mOpenPlay = PlayStatus.play_pause;
		mRecordPlayPause.setImageResource(R.drawable.record_btn_play);
	}
	
	public boolean openAudio() {
		return mPlayWin.playAudio() == retOK;
	}

	public boolean closeAudio() {
		return mPlayWin.stopAudio() == retOK;
	}

	public void seek(int index) {
		System.out.println("index:"+index);
		
		long seekTime = recordInfo.getStartTime() / 1000 + index;
		//先暂存时间记录
		MediaPlayBackFragment.this.beginTime = TimeHelper.getTimeHMS(seekTime*1000);
		MediaPlayBackFragment.this.progress = index;
		mRecordSeekbar.setProgress(index);
		mRecordStartTime.setText(MediaPlayBackFragment.this.beginTime);
		
		mPlayWin.seek(index);
		
//		if (recordInfo != null&& (recordInfo.getType() == RecordInfo.RecordType.PublicCloud)) {
//			if (mOpenPlay == PlayStatus.play_pause) {
//				resume();
//			}
//			mPlayWin.seek(index);
//			System.out.println(tag + "tag-publicClound Seek to: " + index);
//		}else{
//			playSeek(seekTime * 1000, recordInfo.getEndTime());
//			System.out.println(tag + "tag-deviceRecord Seek to: " + index);
//		}
	}
	
//	private void playSeek(long startTime, long endTime) {
//		//打开加载进度栏
//		showLoading(R.string.common_loading);
//		if (mOpenPlay != PlayStatus.play_close) {
//			stopPlayWindow();
//		}
//		mOpenPlay = PlayStatus.play_opening;
//		mRecordPlayPause.setImageResource(R.drawable.record_btn_pause);
//		
//		mPlayWin.playRtspPlaybackByUtcTime(Business.getInstance().getToken(), 
//				channelInfo.getDeviceCode(), channelInfo.getEncryptKey() != null ? channelInfo.getEncryptKey() : channelInfo.getDeviceCode(), channelInfo.getIndex(), startTime, endTime);
//	}

	public void play(int strRes) {
		//if (mOpenPlay == PlayStatus.play_open) {
			stop(-1);
		//}
		if (strRes > 0) {
			showLoading(strRes);
		} else {
			showLoading(R.string.common_loading);
		}
		mOpenPlay = PlayStatus.play_opening;
		mRecordPlayPause.setImageResource(R.drawable.record_btn_pause);
//		mPlayWin.setStreamCallback(1); //启用标准流回调，测试代码
		if (recordInfo.getType() == RecordInfo.RecordType.PublicCloud){
			//播放起始时间默认设为0，开发者自行
			mPlayWin.playCloud(Business.getInstance().getToken(), channelInfo.getDeviceCode(), 
					String.valueOf(channelInfo.getIndex()), 
					channelInfo.getEncryptKey() != null ? channelInfo.getEncryptKey() : channelInfo.getDeviceCode(), 
							recordInfo.getRecordID(), 1000, 0, 24 * 3600);
		}else{
			mPlayWin.playRtspPlayback(Business.getInstance().getToken(), channelInfo.getDeviceCode(), channelInfo.getIndex(),
					channelInfo.getEncryptKey() != null ? channelInfo.getEncryptKey() : channelInfo.getDeviceCode(), recordInfo.getRecordID(),
					recordInfo.getStartTime(), recordInfo.getEndTime(), 0);
//			//按时间回放
//			mPlayWin.playRtspPlaybackByUtcTime(Business.getInstance().getToken(), channelInfo.getDeviceCode(), channelInfo.getEncryptKey() != null ? channelInfo.getEncryptKey() : channelInfo.getDeviceCode(),
//					channelInfo.getIndex(), recordInfo.getStartTime(), recordInfo.getEndTime());
			
			//按文件名回放，播放起始时间默认设为0，开发者自行修改
//			mPlayWin.playRtspPlaybackByFileName(Business.getInstance().getToken(), channelInfo.getDeviceCode(), 
//					channelInfo.getEncryptKey() != null ? channelInfo.getEncryptKey() : channelInfo.getDeviceCode(), recordInfo.getRecordPath(), 0);
		}
		
		//允许拖动
		setCanSeekChanged(true);
	}
	
	public void stop(final int res) {
		//关闭播放加载控件
		mProgressDialog.setStop();
		stopPlayWindow();
		mOpenPlay = PlayStatus.play_close;
		mRecordPlayPause.setImageResource(R.drawable.record_btn_play);
		if (mHander != null) {
			mHander.post(new Runnable() {
				@Override
				public void run() {
					if (isAdded()) {
						if (res > 0) showErrorTip(res);						
					}
				}
			});
		}
		//禁止拖动
		setCanSeekChanged(false);
	}
	
	/**
	 * 描述 :单独关闭播放 ！ 注意：组件要求必须要主线程调用
	 */
	public void stopPlayWindow() {
		closeAudio();// 关闭音频
		if(recordInfo.getType() == RecordInfo.RecordType.PublicCloud)
			mPlayWin.stopCloud();
		else
			mPlayWin.stopRtspPlayback();// 关闭视频
	}
	
	/**
	 * 描述：设置拖动进度条是否能使用
	 */
	public void setCanSeekChanged(boolean canSeek) {
		mRecordSeekbar.setCanTouchAble(canSeek);
	}

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		switch (view.getId()) {
		case R.id.record_play_pause:
			switch(mOpenPlay){
			case play_open:
				pause();
				break;
			case play_pause:
				resume();
				break;
			case play_opening:
				stop(-1);  //因为准备播放过程异步执行,这个状态里,无法精准的控制停止操作
				break;
			case play_close:
				initSeekBarAndTime();
				play(-1);
				break;
			default:
				break;
			}
			break;
		case R.id.record_scale:
			if("LANDSCAPE".equals(mRecordScale.getTag())){
				mOrientation = ORIENTATION.isPortRait;
				getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}else{
				mOrientation = ORIENTATION.isLandScape;
				getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			}
			break;
		case R.id.record_play_pressed:
			initSeekBarAndTime();
			play(-1);
			break;
		default:
			break;
		}
	}
}
