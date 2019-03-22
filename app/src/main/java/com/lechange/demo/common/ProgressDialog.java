package com.lechange.demo.common;

import java.util.concurrent.locks.LockSupport;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


/**
 * 多个ProgressDialog映射一个线程，通过mThreadDialog字段标识
 * @author 31554
 *
 */
public class ProgressDialog extends TextView{
	
	private static Thread mUIThread;
	private static ProgressDialog mThreadDialog;
	
	private static StringBuilder mMsg;
	private static int mMsgIndex;
	private static Handler mHandler;
	static {
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				//Log.d("ProgressDialog", "======" + mMsg.toString());
				if (mThreadDialog != null) {
					mThreadDialog.setText(mMsg.toString());
				}
			}
		};
		mUIThread = new Thread() {
			@Override
			public void run() {
				super.run();
				while (true) {
					if (mThreadDialog != null) {		
						if(mMsgIndex >= 4){
							mMsgIndex = 0;
							mMsg.setLength(mMsg.length() - 8);
						}
						mMsg.append(" .");
						mMsgIndex++;
						mHandler.obtainMessage().sendToTarget();
						try {					
							Thread.sleep(1000);
						} catch (Exception e) {
							// InterruptedException
							e.printStackTrace();
						}
					} else
						LockSupport.park();
				}
			}
		};
		mUIThread.start();
	}
	
	//构造
	public ProgressDialog(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public ProgressDialog(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public ProgressDialog(Context context) {
		super(context);
	}
	
	public void setStart(String msg){
		//Log.d("111", this + "=====start " + mThreadDialog + " : " + this);
		mMsgIndex = 0;
		mMsg = new StringBuilder(msg);		
		if (mThreadDialog == null || mThreadDialog.hashCode() != this.hashCode()) {
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					setVisibility(View.VISIBLE);
					setText(mMsg);
				}
			});
			mThreadDialog = this;
			LockSupport.unpark(mUIThread);
		}
	}
	
	public void setStop(){
		//Log.d("111", "=====stop  " + mThreadDialog + " : " + this);
		mHandler.post(new Runnable() {		
			@Override
			public void run() {
				setVisibility(View.GONE);
			}
		});
		if (mThreadDialog != null && mThreadDialog.hashCode() == this.hashCode()) {
			mThreadDialog = null;
		}
	}
	
}


//mUIThread.interrupt();
//try {
//	mUIThread.join();
//} catch (InterruptedException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}