package com.lechange.demo.login;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.lechange.demo.R;
import com.lechange.demo.business.Business;
import com.lechange.demo.common.CommonTitle;
import com.lechange.demo.common.CommonTitle.OnTitleClickListener;

public class BindUserActivity extends Activity{
	
	private CommonTitle mCommonTitle;

	private EditText mAuthCodeEdit ;
	private Button   mGetAuthCodeBtn;
	private Button   mBindBtn;
	private TextView mNoticeText;
	private String   mPhoneNumber;
	private int mTime = 0;
	private final Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			if(msg.what > 1)
			{
				sendEmptyMessageDelayed(mTime--, 1000);
				mGetAuthCodeBtn.setText("重新获取(" + mTime +")");
			}
			else if(msg.what <= 1)
			{
				mGetAuthCodeBtn.setText("重新获取");
				mGetAuthCodeBtn.setTextColor(Color.BLUE);
			}
			
		}
		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_user_bind);
		mPhoneNumber = getIntent().getStringExtra("phoneNumber");
		initView();
		initTitle();
		setListener();
	}
	
	public void initView()
	{
		mAuthCodeEdit = (EditText) findViewById(R.id.authCodeEdit);
		mGetAuthCodeBtn = (Button) findViewById(R.id.getAuthCode);
		mBindBtn = (Button) findViewById(R.id.bind);
		mNoticeText = (TextView) findViewById(R.id.authCodeNotice);
		mNoticeText.setText(this.getString(R.string.bind_user_notice) + mPhoneNumber);
	}
	
	public void initTitle()
	{
		//绘制标题
		mCommonTitle = (CommonTitle) findViewById(R.id.title);
		mCommonTitle.initView(R.drawable.title_btn_back, 0, R.string.user_bind);
		
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
		mBindBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View arg0) {
				checkSms();				
			}
		});
		mGetAuthCodeBtn.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View arg0) {
                //还在倒计时
				if(mTime > 0)
					return;
				getUserSms();
			}
		});
		
		mAuthCodeEdit.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int arg2, int arg3) {
				if(s.length() != 0){
					mBindBtn.setBackgroundColor(Color.rgb(0x4e, 0xa7, 0xf2));
				}
				else{
					mBindBtn.setBackgroundColor(Color.rgb(0x8a, 0xb9, 0xe1));
				}
				
			}
			
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {		
			}
			
			@Override
			public void afterTextChanged(Editable arg0) {
					
			}
		});
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}
	

	public void checkSms(){
		mNoticeText.setVisibility(View.INVISIBLE);
		mBindBtn.setClickable(false);
		String smsCode = mAuthCodeEdit.getText().toString().trim();
		if(smsCode.length() == 0)
		{
			Toast.makeText(getApplicationContext(), R.string.toast_userbind_no_idcode, Toast.LENGTH_LONG).show();
			mBindBtn.setClickable(true);
			return;
		}

		Business.getInstance().checkUserSms(mPhoneNumber, smsCode, new Handler(){		
			@Override
			public void handleMessage(Message msg) {
				if(0 == msg.what)
				{
					finish();					
				}
				else
				{
					mNoticeText.setText((String)msg.obj);
					mNoticeText.setVisibility(View.VISIBLE);
				}
				mBindBtn.setClickable(true);
			}
			
		});
	}
	
	/**
	 * 获取短信验证码
	 */
	public void getUserSms(){
		mNoticeText.setVisibility(View.INVISIBLE);
		Business.getInstance().getUserSms(mPhoneNumber, new Handler(){
			@Override
			public void handleMessage(Message msg) {
				if(0 == msg.what)
				{	
					mGetAuthCodeBtn.setTextColor(Color.rgb(195, 195, 200));
					mTime = 60;
					handler.obtainMessage(60).sendToTarget();
					
					mNoticeText.setText(getApplication().getResources().getString(R.string.bind_user_notice) + mPhoneNumber);
					mNoticeText.setVisibility(View.VISIBLE);						
				}
				else
				{
					String result = (String)msg.obj;
					mNoticeText.setText(result);
					mNoticeText.setVisibility(View.VISIBLE);
				}
			}
			
		});
	}
}
