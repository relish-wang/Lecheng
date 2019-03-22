package com.lechange.demo.common;


import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lechange.demo.R;

/**
 * 工具类使用说明：左按钮，中间按钮，右按钮，均支持图片和文字背景二选一，左右按钮默认支持图片，中间按钮默认支持文字
 * xml使用注意：需要在xml文件中设置宽高和背景色
 * 
 */
public class CommonTitle extends RelativeLayout
{
    /**
     * 左侧按钮ID
     */
    public static final int ID_LEFT = 0;
    
    /**
     * 右侧按钮ID
     */
    public static final int ID_RIGHT = 1;

    /**
     * 中间按钮ID, 暂时不加监听器
     */
    public static final int ID_CENTER = 2;

    /**
     * 左侧按钮
     */
    private ImageView mTitleLeft;


    /**
     * 右侧按钮 
     */
    private ImageView mTitleRight;


    /**
     * 文字标题
     */
    private TextView mTitleCenter;

    /**
     * 点击监听
     */
    private OnTitleClickListener mListener;

    private View mBottom;

    /**
     * 默认隐藏左2和右2的按钮 ，创建一个新的实例CommonTitle.
     * @param context
     * @param attrs
     */
    public CommonTitle(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.widget_common_title, this);
        initView();
        setListeners();
    }

    private void initView() {
        mBottom = findViewById(R.id.bottom_line);
        mTitleLeft = (ImageView) findViewById(R.id.title_left);
        mTitleRight = (ImageView) findViewById(R.id.title_right);

        mTitleCenter = (TextView) findViewById(R.id.title_center);
        mTitleCenter.setTextColor(getResources().getColor(R.color.title_color_center));
        mTitleCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.text_size_large));
    }

    private void setListeners() {
        mTitleLeft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCommonTitleClick(ID_LEFT);
                }

            }
        });


        mTitleRight.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onCommonTitleClick(ID_RIGHT);
                }

            }
        });
    }

    /**
     * 初始化参数，按钮均可支持图片或者文字背景
     * 
     * @param leftResId   左按钮        
     * @param rightResId  右按钮      
     * @param centerResId 中间text
     *            
     */

    public void initView(int leftResId, int rightResId, int centerResId) {
    	setTitleLeftView(leftResId);
        setTitleRightView(rightResId);
        setTitleCenterView(centerResId, 0, 0);
    }


    public TextView getTextViewCenter() {
        return mTitleCenter;
    }


    /**
     * <p>
     * 设置按钮是否可用
     * </p>
     */
    public void setTitleEnabled(boolean enabled, int id) {
        View v = findView(id);
        if (v != null) {
            v.setEnabled(enabled);
        }
    }

    private View findView(int id) {
        switch (id) {
            case ID_LEFT:
                return mTitleLeft;
            case ID_RIGHT:
                return mTitleRight;
            case ID_CENTER:
                return mTitleCenter;
            default:
                return null;
        }
    }
  
    public void setTitleLeftView(int resId) {
        setTitleLeft(resId);
    }

    public void setTitleRightView(int resId) {
        setTitleRight(resId);
    }
    
    public void setTitleCenterView(int resId, int colorId, int textSizeDimenId) {
        setTitleCenter(resId);
        setTextColorCenter(colorId);
        setTextSizeCenter(textSizeDimenId);
    }

    
    /**
     * 设置左边按钮图片
     * @param leftResId
     */
    private void setTitleLeft(int leftResId) {
	    if (leftResId != 0) {
	        if (mTitleLeft != null && mTitleLeft.getVisibility() != View.VISIBLE)
	            mTitleLeft.setVisibility(VISIBLE);
	        mTitleLeft.setImageResource(leftResId);	     
        }else {
            if (mTitleLeft != null)
            	mTitleLeft.setVisibility(INVISIBLE);
        }
    }


    /**
     * 设置右边按钮图片
     * @param rightResId
     */
    private void setTitleRight(int rightResId) {
        if (rightResId != 0) {
            if (mTitleRight != null && mTitleRight.getVisibility() != View.VISIBLE)
                mTitleRight.setVisibility(VISIBLE);
            mTitleRight.setImageResource(rightResId);
        }else {
            if (mTitleRight != null)
            	mTitleRight.setVisibility(INVISIBLE);
        }
    }


    /**
     * 设置中间按钮文字
     * @param centerResId
     */
    private void setTitleCenter(int centerResId) {
        if (centerResId != 0) {
            if (mTitleCenter != null && mTitleCenter.getVisibility() != View.VISIBLE)
            	mTitleCenter.setVisibility(VISIBLE);
            mTitleCenter.setText(centerResId);
        }else {
            if (mTitleCenter != null)
            	mTitleCenter.setVisibility(INVISIBLE);
        }
    }

    public void setTextColorCenter(int colorId) {
        if (mTitleCenter != null) {
            mTitleCenter.setTextColor(colorId != 0 ? getResources().getColor(colorId) : getResources().getColor(
                    R.color.title_color_center));
        }
    }


    public void setTextSizeCenter(int textSizeDimenId) {
        if (mTitleCenter != null) {
            mTitleCenter.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    textSizeDimenId != 0 ? getResources().getDimensionPixelSize(textSizeDimenId) : getResources()
                            .getDimensionPixelSize(R.dimen.text_size_large));
        }
    }

    public void setVisibleLeft(int flag) {
        if (mTitleLeft != null) {
            mTitleLeft.setVisibility(flag);
        }
    }


    public void setVisibleRight(int flag) {
        if (mTitleRight != null) {
            mTitleRight.setVisibility(flag);
        }
    }


    public void setVisibleCenter(int flag) {
        if (mTitleCenter != null) {
        	mTitleCenter.setVisibility(flag);
        }
    }

    public void setVisibleBottom(int flag) {
        if (mBottom != null) {
            mBottom.setVisibility(flag);
        }
    }

    public void setOnTitleClickListener(OnTitleClickListener listener) {
        mListener = listener;
    }

    public interface OnTitleClickListener {
        public void onCommonTitleClick(int id);
    }
}
