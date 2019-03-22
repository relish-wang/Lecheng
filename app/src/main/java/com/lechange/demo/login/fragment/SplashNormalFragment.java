package com.lechange.demo.login.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.listview.DevicelistActivity;
import com.lechange.demo.login.SplashActivity;
import com.lechange.demo.login.UserLoginActivity;
import com.lechange.opensdk.api.LCOpenSDK_Api;

public class SplashNormalFragment extends Fragment{
	
	private EditText appId = null;
	private EditText appSecret = null;
	private EditText appUrl = null;
	private ImageView adminBtn = null;
	private ImageView userBtn = null;
	private SharedPreferences sp; //固化数据
	private Activity mActivity;
	
	
	public String getAppId() {
		return appId.getText().toString();
	}



	public String getAppSecret() {
		return appSecret.getText().toString();
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		sp =  getActivity().getSharedPreferences("OpenSDK", SplashActivity.MODE_PRIVATE);	
		return inflater.inflate(R.layout.fragment_splash_normal, container,false);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		initView();
		setListener();
		
//		inner online
		appId.setText(sp.getString("appid", ""));
		appSecret.setText(sp.getString("appsecret", ""));
		appUrl.setText(sp.getString("appurl", Business.getInstance().isOversea ? "openapi.easy4ip.com:443" : "openapi.lechange.cn:443"));
		appUrl.setVisibility(View.INVISIBLE);
		
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	
	private void initView()
	{
		mActivity = getActivity();
		
		appId = (EditText) getView().findViewById(R.id.editAppId);
		appSecret = (EditText) getView().findViewById(R.id.editAppSectet);
		appUrl = (EditText) getView().findViewById(R.id.editappurl);
		adminBtn = (ImageView) getView().findViewById(R.id.adminButton);
		userBtn = (ImageView) getView().findViewById(R.id.userButton);
		
		if(!getResources().getConfiguration().locale.getLanguage().endsWith("zh")){
			userBtn.setVisibility(View.GONE);
		}
	}
	
	private void setListener()
	{
		adminBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(!validCheck())
				{
					SplashActivity splashActivity = (SplashActivity) getActivity();
					splashActivity.changeFragment();
					return;
				}
				//按钮不能点击，防止连击开启多个activity；也可以修改启动模式
				adminBtn.setClickable(false);
				
				//初始化需要的数据
				Business.getInstance().init(appId.getText().toString(), appSecret.getText().toString(), appUrl.getText().toString());
				
				Editor editor=sp.edit();
				editor.putString("appid", appId.getText().toString());
				editor.putString("appsecret", appSecret.getText().toString());
				editor.putString("appurl", appUrl.getText().toString());
				editor.commit();
				
				adminLogin();
			}
	
		});

		userBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if(!validCheck())
				{
				SplashActivity splashActivity = (SplashActivity) getActivity();
				splashActivity.changeFragment();
				return;
				}
				
				//初始化需要的数据
				Business.getInstance().init(appId.getText().toString(), appSecret.getText().toString(), appUrl.getText().toString());
				
				Editor editor=sp.edit();
				editor.putString("appid", appId.getText().toString());
				editor.putString("appsecret", appSecret.getText().toString());
				editor.putString("appurl", appUrl.getText().toString());
				editor.commit();
				
				Intent intent = new Intent(getActivity(),UserLoginActivity.class );				
				getActivity().startActivity(intent);
				
			}

		});
	}
	
	private void  adminLogin() {
		Business.getInstance().adminlogin(new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(0 == msg.what)
				{
					String accessToken = (String) msg.obj;
					Business.getInstance().setToken(accessToken);

					//恢复可点击
					adminBtn.setClickable(true);
					startActivity();				
				}
		       else{
		    	   if(1 == msg.what){
		    		   Toast.makeText(mActivity, "getToken failed", Toast.LENGTH_SHORT).show();
		    	   }else{
		    		   String result = (String)msg.obj;
		    		   Toast.makeText(mActivity, result, Toast.LENGTH_SHORT).show();
		    	   }	
		    	   //恢复可点击
		    	   adminBtn.setClickable(true);
				}
			}
			
		});
	}
	
	private void startActivity() {
		Intent mIntent = new Intent(this.getActivity(), DevicelistActivity.class);
		Bundle b = mActivity.getIntent().getExtras();
		if (b != null) {
			mIntent.putExtras(mActivity.getIntent().getExtras());
		}
		startActivity(mIntent);
	}
	
	private boolean validCheck()
	{
		
		if(appId.getText().length()==0 || appSecret.getText().length() == 0)
		{
			return false;
		}
		return true;
	}

}
