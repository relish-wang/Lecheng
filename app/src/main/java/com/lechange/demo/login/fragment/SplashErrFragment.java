package com.lechange.demo.login.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.lechange.demo.R;

public class SplashErrFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		return inflater.inflate(R.layout.fragment_splash_err, container, false);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		EditText appId = (EditText) getView().findViewById(R.id.editAppId);
		EditText appSecret = (EditText) getView().findViewById(
				R.id.editAppSectet);
		appId.setText(getArguments().getString("appId"));
		appSecret.setText(getArguments().getString("appSecret"));
		LinearLayout layout = (LinearLayout) getView().findViewById(
				R.id.container);
		layout.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				getFragmentManager().popBackStack();

			}

		});
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}



}
