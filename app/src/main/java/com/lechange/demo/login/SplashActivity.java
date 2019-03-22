/*  
 * 项目名: YYS 
 * 文件名: YysApp.java  
 * 版权声明:
 *      本系统的所有内容，包括源码、页面设计，文字、图像以及其他任何信息，
 *      如未经特殊说明，其版权均属大华技术股份有限公司所有。
 *      Copyright (c) 2015 大华技术股份有限公司
 *      版权所有
 */
package com.lechange.demo.login;


import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.lechange.demo.R;
import com.lechange.demo.login.fragment.SplashErrFragment;
import com.lechange.demo.login.fragment.SplashNormalFragment;

/**
 * 
 * 描述：启动界面
 * 
 * @author：fuwl
 */

public class SplashActivity extends Activity {
	public static final String tag = "LoginActivity";
	public SplashNormalFragment mFragemnt;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setContentView(R.layout.activity_splash);
		FragmentManager fragementManager = getFragmentManager();
		FragmentTransaction fragmentTansaction = fragementManager.beginTransaction(); 
		mFragemnt = new SplashNormalFragment();
	
		fragmentTansaction.add(R.id.frame_content, mFragemnt);
		fragmentTansaction.commit();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	public void changeFragment()
	{
		FragmentManager fragementManager = getFragmentManager();
		FragmentTransaction fragmentTansaction = fragementManager.beginTransaction(); 
		SplashErrFragment errorFragment = new SplashErrFragment();
		Bundle bundle = new Bundle();
		bundle.putString("appId", mFragemnt.getAppId());
		bundle.putString("appSecret", mFragemnt.getAppSecret());
		errorFragment.setArguments(bundle);
		fragmentTansaction.replace(R.id.frame_content, errorFragment);
		fragmentTansaction.addToBackStack(null);
		fragmentTansaction.commit();
	}

}
