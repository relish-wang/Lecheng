package com.lechange.demo.business.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.LockSupport;

import android.util.Log;

public class TaskPoolHelper {
	private final static String TAG = "TaskPoolHelper";
	
	/**
	 * 构造具备过滤的task
	 */
	public static abstract class RunnableTask implements Runnable{
		public String mTaskID;
		
		public RunnableTask(String taskID){
			this.mTaskID = taskID;
		}
	}
	
	//private static ExecutorService mPool = Executors.newFixedThreadPool(3);
	//队列属性的task
	private static ArrayBlockingQueue<RunnableTask> mQueueTask = new ArrayBlockingQueue<RunnableTask>(50);
	private static List<String> mFilteTask = new ArrayList<String>();
	private static Thread mQueueThread;
	
	//实时属性的task
	private static RunnableTask mRealTask; //=null
	private static Thread mRealThread;
	
	static {
		mQueueThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				while (true) {	
					try {
						//自带阻塞光环
						RunnableTask task = mQueueTask.take();
						task.run();
						mFilteTask.remove(task.mTaskID);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		mQueueThread.start();
		
		mRealThread = new Thread() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				super.run();
				while (true) {
					if (mRealTask == null) {
						LockSupport.park();
					} else {
						RunnableTask task = mRealTask;
						mRealTask = null;
						task.run();
					}				
				}
			}
		};
		mRealThread.start();
	}
	
	
	public static void addTask(RunnableTask task) {
		//过滤
		if (task.mTaskID.equals("real")) {
			mRealTask = task;
			LockSupport.unpark(mRealThread);
			return;
		}
		if (mFilteTask.contains(task.mTaskID)) {
			return;
		}
		
		try {
			mQueueTask.add(task);
			mFilteTask.add(task.mTaskID);
		} catch(IllegalStateException e) {
			Log.w(TAG, e.getMessage());
			mQueueTask.clear();
			mFilteTask.clear();
		}
	}
	
	
	public static void clearTask(){
		mQueueTask.clear();
		mFilteTask.clear();
	}
	
}
