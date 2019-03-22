package com.lechange.demo.login;




import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;
import com.lechange.demo.listview.DevicelistActivity;

public class UserLoginActivity extends Activity{

	private String tag = "UserLoginActivity";
	private EditText mZoomEdit;
	private EditText mPhoneEdit;
	private Button mBindUserBtn;
	private Button mDeviceListBtn;
	private TextView notice;
	private CommonTitle mCommonTitle;
	private SharedPreferences sp; //固化数据

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_user_login);
		sp = this.getSharedPreferences("OpenSDK", MODE_PRIVATE);
		initView();
		initTitle();
		setListener();
	}

	public void initView()
	{
		mZoomEdit = (EditText) findViewById(R.id.zoneView);
		mZoomEdit.setText(sp.getString("zone", "+86"));
		mPhoneEdit = (EditText) findViewById(R.id.phoneEdit);
		mPhoneEdit.setText(sp.getString("userphonenumber", ""));
		
		mBindUserBtn = (Button) findViewById(R.id.bindUser);
		mDeviceListBtn = (Button) findViewById(R.id.deviceList);
		notice = (TextView) findViewById(R.id.notice);
	}
	public void initTitle()
	{
		//绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, 0, R.string.user_login_name);
		
		mCommonTitle.setOnTitleClickListener(new OnTitleClickListener() {		
			@Override
			public void onCommonTitleClick(int id) {
				// TODO Auto-generated method stub
				switch (id) {
				case CommonTitle.ID_LEFT:
					finish();
				}
			}
		});
	}
	public void setListener()
	{
		mBindUserBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View arg0) {
				userLogin(mBindUserBtn.getId());
			}
		});
		
		mDeviceListBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {			
				userLogin(mDeviceListBtn.getId());
			}
		});
		
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	

	/**
	 * 获取usertoken
	 */
	public void userLogin(final int id){
		notice.setVisibility(View.INVISIBLE);
		String zone = mZoomEdit.getText().toString().trim();
		String phoneNumber = mPhoneEdit.getText().toString().trim();
		
		if(phoneNumber.length() != 11)
		{
			notice.setText(R.string.user_no_input);
			notice.setVisibility(View.VISIBLE);
			return;
		}	
		

		
		
		mBindUserBtn.setClickable(false);
		mDeviceListBtn.setClickable(false);
		
		Editor editor=sp.edit();
		editor.putString("userphonenumber", phoneNumber);
		editor.putString("zone", zone);
		editor.commit();
		
		if(!zone.endsWith("86") && !zone.equals("") ) {
			phoneNumber = zone + phoneNumber;
		}
		
		
		Business.getInstance().userlogin(phoneNumber, new Handler(){
			@Override
			public void handleMessage(Message msg) {				
				if(0 == msg.what)
				{
					switch(id){
					case R.id.bindUser:
						notice.setText(R.string.user_bind_err);
						notice.setVisibility(View.VISIBLE);
						break;
					case R.id.deviceList:
						String userToken = (String) msg.obj;
						Log.d(tag,"userToken" + userToken);
						Business.getInstance().setToken(userToken);
						startActivity();
						break;
					}							
				}
				else
				{
					switch(id){
					case R.id.bindUser:
						if(1 == msg.what)
							startBindUserActivity();
						else{
							String result = (String)msg.obj;
							notice.setText(result);
							notice.setVisibility(View.VISIBLE);
						}
						break;
					case R.id.deviceList:
						if(1 != msg.what){
							String result = (String)msg.obj;
							notice.setText(result);
						}else{
							notice.setText(R.string.user_nobind_err);
						}
						
						notice.setVisibility(View.VISIBLE);
						break;
					}
				}
				mBindUserBtn.setClickable(true);
				mDeviceListBtn.setClickable(true);
			}
			
		});
	}
	
	public void startBindUserActivity()
	{
		Intent intent = new Intent(UserLoginActivity.this, BindUserActivity.class );
		intent.putExtra("phoneNumber", mPhoneEdit.getText().toString());
		startActivity(intent);
	}

	/**
	 * 跳转到主页
	 */
	public void startActivity(){
		Intent mIntent = new Intent(this, DevicelistActivity.class);
		Bundle b = getIntent().getExtras();
		if (b != null) {
			mIntent.putExtras(getIntent().getExtras());
		}
		startActivity(mIntent);
		finish();
	}
}
