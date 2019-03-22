package com.lechange.demo.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class RecoderSeekBar extends SeekBar{
	
	private boolean canTouchAble = true;

	public RecoderSeekBar(Context context,AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	public RecoderSeekBar(Context context,
			AttributeSet attrs) {
		super(context, attrs);
	}
	public RecoderSeekBar(Context context) {
		super(context);
	}
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(!canTouchAble){
			return true;
		}
		return super.onTouchEvent(event);
	}

	public void setCanTouchAble(boolean canTouchAble){
		this.canTouchAble = canTouchAble;
	}
}
