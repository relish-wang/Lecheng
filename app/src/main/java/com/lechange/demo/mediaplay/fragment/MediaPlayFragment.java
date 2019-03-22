/*  
 * 项目名: YYS 
 * 文件名: MediaPlayFragment.java  
 * 版权声明:
 *      本系统的所有内容，包括源码、页面设计，文字、图像以及其他任何信息，
 *      如未经特殊说明，其版权均属大华技术股份有限公司所有。
 *      Copyright (c) 2015 大华技术股份有限公司
 *      版权所有
 */
package com.lechange.demo.mediaplay.fragment;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.lechange.demo.business.util.MediaPlayHelper;
import com.lechange.demo.common.ProgressDialog;
import com.lechange.demo.mediaplay.MediaPlayActivity;
import com.lechange.opensdk.listener.LCOpenSDK_EventListener;
import com.lechange.opensdk.media.LCOpenSDK_PlayWindow;



/**
 * 描述：视频播放组件基类 作者： lc
 */
public class MediaPlayFragment extends Fragment{
	private final static String tag = "MediaPlayFragment";
	//底层返回值
	protected static final int retOK = 0;
	protected static final int retNG = -1;
	
	public interface BackHandlerInterface {  
	    public void setSelectedFragment(MediaPlayFragment backHandledFragment);  
	} 
	protected LCOpenSDK_EventListener listener;
	protected LCOpenSDK_PlayWindow mPlayWin = new LCOpenSDK_PlayWindow();
	protected Handler mHander = new Handler();
	// 屏幕方向改变监听器
	private OrientationEventListener mOrientationListener;
	
	// 统计当前累计总流量，单位：字节
	//protected long mTotalFlow;
	protected ViewGroup mSurfaceParentView;	
	protected ProgressDialog mProgressDialog;  //播放加载使用
	protected TextView mReplayTip;
	
	public enum ORIENTATION {isPortRait, isLandScape, isNone}	
	protected ORIENTATION mOrientation = ORIENTATION.isNone;   		 //禁止横竖屏标志

	protected static final String AUDIO_TALK_ERROR = "-1000";        //实时对讲初始化失败
	
	
	//吐司类工具
	protected void toast(int resId) {
		if (getActivity() != null && !getActivity().isFinishing()) {
			Toast.makeText(getActivity(), resId, Toast.LENGTH_SHORT).show();
		}
	}
	protected void toast(String content) {
		if (getActivity() != null && !getActivity().isFinishing()) {
			Toast.makeText(getActivity(), content, Toast.LENGTH_SHORT).show();
		}
	}	
	public void toastWithImg(String content, int imgId) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            Toast toast = Toast.makeText(getActivity(), content, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            LinearLayout toastView = (LinearLayout) toast.getView();
            ImageView imageCodeProject = new ImageView(getActivity());
            imageCodeProject.setImageResource(imgId);
            toastView.addView(imageCodeProject, 0);
            toast.show();
        }
    }
	
	//播放失败提示器
	protected void showErrorTip(int resId){
		mReplayTip.setVisibility(View.VISIBLE);
		mReplayTip.setText(resId);
	}
	protected void showErrorTip(String res){
		mReplayTip.setVisibility(View.VISIBLE);
		mReplayTip.setText(res);
	}
	
	//播放缓冲提示器
	protected void showLoading(int res) {
		mReplayTip.setVisibility(View.GONE);
		mProgressDialog.setStart(getString(res));  //开启播放加载控件
	}
	protected void showLoading(String res) {
		mReplayTip.setVisibility(View.GONE);
		mProgressDialog.setStart(res);  //开启播放加载控件
	}
	
	@Override  
    public void onStart() {  
        super.onStart();  
        //将自己的实例传出去 
		if (!(getActivity() instanceof BackHandlerInterface)) {  
	        throw new ClassCastException("Hosting activity must implement BackHandlerInterface");  
	    } else {  
	        ((BackHandlerInterface)getActivity()).setSelectedFragment(this);  
	    }
    }
	
	@Override
    public void onDestroyView()
    {
        super.onDestroyView();
        mOrientationListener.disable();
        mOrientationListener = null;
    }
    
	/**
	 * 描述：开启屏幕方向监听
	 */
	protected final void startListener() {
		if (getActivity() == null || getActivity().isFinishing()) {
			return;
		}
		mOrientationListener = new OrientationEventListener(getActivity(), SensorManager.SENSOR_DELAY_NORMAL) {
			@Override
			public void onOrientationChanged(int rotation) {
				// 设置竖屏
				if (getActivity() == null || getActivity().isFinishing()) {
					return;
				}
				requestedOrientation(rotation);
			}
		};
		if (mOrientationListener.canDetectOrientation()) {
			mOrientationListener.enable();
		} else {
			mOrientationListener.disable();
		}

	}
	/**
	 * 描述：改变屏幕方向
	 */
	private void requestedOrientation(int rotation) {
		if (rotation < 10 || rotation > 350) {// 手机顶部向上
			setPortOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		} else if (rotation < 100 && rotation > 80) {// 手机右边向上
			setLandOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		} else if (rotation < 190 && rotation > 170) {// 手机低边向上
			setPortOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
		} else if (rotation < 280 && rotation > 260) {// 手机左边向上
			setLandOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
	}

	private void setPortOrientation(int type) {
		switch(mOrientation){
		case isNone:
			getActivity().setRequestedOrientation(type);
			break;
		case isPortRait:
			mOrientation = ORIENTATION.isNone;
			break;
		default:
			break;
		}	
	}

	private void setLandOrientation(int type) {
		switch(mOrientation){
		case isNone:
			getActivity().setRequestedOrientation(type);
			break;
		case isLandScape:
			mOrientation = ORIENTATION.isNone;
			break;
		default:
			break;
		}	
	}
	
	/**
	 * @see android.support.v4.app.Fragment#onConfigurationChanged(android.content.res.Configuration)
	 *      描述：屏幕方向改变时重新绘制界面
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		initWindow(newConfig);
		initSurFace(newConfig);
		resetViews(newConfig);
	}	

	/**
	 * 描述：初始化playWindow
	 */
	protected void initWindow(Configuration mConfiguration) {
		LayoutParams mLayoutParams = (LayoutParams) mSurfaceParentView.getLayoutParams();
		if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 横屏
			DisplayMetrics metric = new DisplayMetrics();
			getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
			mLayoutParams.width = metric.widthPixels; // 屏幕宽度（像素）
			mLayoutParams.height = metric.heightPixels; // 屏幕高度（像素）
			mLayoutParams.setMargins(0, 0, 0, 0);
		} else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			DisplayMetrics metric = new DisplayMetrics();
			getActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
			mLayoutParams.width = metric.widthPixels; // 屏幕宽度（像素）
			mLayoutParams.height = metric.widthPixels * 9 / 16;
			mLayoutParams.setMargins(0, 10, 0, 0);
		}
		mSurfaceParentView.setLayoutParams(mLayoutParams);
	}
	
	/**
	 * 描述：初始化全屏或非全屏
	 */
	private void initSurFace(Configuration mConfiguration) {
		if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // 横屏
			if (getActivity() instanceof MediaPlayActivity) {
				((MediaPlayActivity) getActivity()).toggleTitle(false);
			}
			MediaPlayHelper.setFullScreen(getActivity());
		} else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
			if (getActivity() instanceof MediaPlayActivity) {
				((MediaPlayActivity) getActivity()).toggleTitle(true);
			}
			MediaPlayHelper.quitFullScreen(getActivity());
		}
	}

	/**
	 * 重置View,子类实现
	 */
	protected void resetViews(Configuration mConfiguration) {}

    public boolean onBackPressed() {
        return false;
    }
}

