package com.lechange.demo.common;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lechange.demo.R;
import com.lechange.demo.common.datepicker.AbstractWheel;
import com.lechange.demo.common.datepicker.OnWheelChangedListener;
import com.lechange.demo.common.datepicker.adapters.NumericWheelAdapter;



@SuppressLint("SimpleDateFormat")
public class DatePicker extends LinearLayout  {
    private static final String TAG = "LCDatePicker";
    
    public static final int ID_LEFT = 0;
    public static final int ID_RIGHT = 1;
    
    private OnTimeClickListener mListener;
    
    private AbstractWheel mYearWheel;               //滚轮-年
    private AbstractWheel mMonthWheel;              //滚轮-月
    private AbstractWheel mDayWheel;               //滚轮-日
    
    private Button   mCancel;                       //取消按钮（左边按钮）
    private TextView mNotice;                       //提示信息（中间文本）
    private Button   mSearch;                       //搜索按钮（右边按钮）
    
    private Date    mMinDate;                        //拾取器的最小日期 ，默认为当前时间往前50年
    private Date    mMaxDate;                        //拾取器的最大日期，默认为当前时间往后50年
    private String  mCurrentYear;                     //拾取器当前选中的年-yyyy
    private String  mCurrentMonth;                   //拾取器当前选中的月-MM
    private String  mCurrentDay;                    //拾取器当前选中的日-dd
    
    public DatePicker(Context context){
    	super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_common_datepicker, this);
        init();
        resetWheels();
    	
    }
    
    public DatePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.widget_common_datepicker, this);
        init();
        resetWheels();
    }

    /**
     * 设置时间拾取器可选时间范围
     * <p></p>
     * @author 16552 2016年2月29日 上午9:44:34
     * @param minDate
     * @param maxDate
     */
    public void setMinMaxDate(Date minDate, Date maxDate) {
        this.mMinDate = minDate;
        this.mMaxDate = maxDate;
        resetWheels();
    }
    
    
    /**
     * 设置按钮文本
     * @param leftText
     * @param centerText
     * @param rightText
     */
    public void setText(String leftText,String centerText,String rightText){
    	
    	mCancel.setText(leftText);
    	mNotice.setText(centerText);
    	mSearch.setText(rightText);
    }
    /**
     * 获取时间拾取器当前选中的时间
     * <p></p>
     * @author 16552 2016年2月29日 上午9:44:18
     * @return
     */
    public Date getSelectedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Log.d("Business",mCurrentYear + mCurrentMonth + mCurrentDay);
        Date selectedDate = null;
        try {
            selectedDate = dateFormat.parse(mCurrentYear + mCurrentMonth + mCurrentDay );
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "getEndTime parse daile  -> " + mCurrentYear + mCurrentMonth + mCurrentDay );
        }
        return selectedDate == null ? new Date() : selectedDate;
    }
    
    private void init() {
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.YEAR, 50);
        mMaxDate = calendar.getTime();
        calendar.add(Calendar.YEAR, -100);
        mMinDate = calendar.getTime();
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateString = sdf.format(currentDate);
        mCurrentYear = dateString.substring(0, 4);
        mCurrentMonth = dateString.substring(4, 6);
        mCurrentDay = dateString.substring(6, 8);
 
        
        mYearWheel  = (AbstractWheel) findViewById(R.id.year_wheel);
        mMonthWheel = (AbstractWheel) findViewById(R.id.month_wheel);
        mDayWheel   = (AbstractWheel) findViewById(R.id.day_wheel);
        
        mCancel = (Button) findViewById(R.id.cancel);
        mNotice = (TextView) findViewById(R.id.notice);
        mSearch = (Button) findViewById(R.id.search);
        
        mCancel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(mListener != null) {
					mListener.onCommonTimeClick(ID_LEFT);
				}
				
			}
		});
        
        mSearch.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(mListener != null) {
					mListener.onCommonTimeClick(ID_RIGHT);
				}
				
				
			}
		});
        
    }
    
    private void resetWheels() {
        

        
   
    	if(mYearWheel == null)
    	{
    		Log.d("tag","mYearWheel is null");
    	}
        mYearWheel.setViewAdapter(new NumericWheelAdapter(getContext(), mMinDate.getYear() + 1900, mMaxDate.getYear() + 1900, "%04d"));
        mYearWheel.setCyclic(false);
        mYearWheel.setInterpolator(new AnticipateOvershootInterpolator());
        mYearWheel.addChangingListener(new OnWheelChangedListener() {
            
            @Override
            public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
                String format = "%04d";
                mCurrentYear = String.format(format, newValue +  (mMinDate.getYear() + 1900));
                resetDayWheelAdapter();
            }
        });
        mYearWheel.setCurrentItem(Integer.valueOf(mCurrentYear) - (mMinDate.getYear() + 1900));

        int thisMonth = Integer.valueOf(mCurrentMonth);
        mMonthWheel.setViewAdapter(new NumericWheelAdapter(getContext(), 1,12, "%02d"));
        mMonthWheel.setCyclic(true);
        mMonthWheel.setInterpolator(new AnticipateOvershootInterpolator());
        mMonthWheel.addChangingListener(new OnWheelChangedListener() {
            
            @Override
            public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
                String format = "%02d";
                mCurrentMonth = String.format(format, newValue + 1);
                resetDayWheelAdapter();
            }
        });
        mMonthWheel.setCurrentItem(thisMonth - 1);
        
        resetDayWheelAdapter();

    }
    
    
    /**
     * 使用 mCurrentYear 和 mCurrentMonth 计算并重设 mDayWheel
     */
    private void resetDayWheelAdapter() {
        
        int thisDay = Integer.valueOf(mCurrentDay);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
        Calendar cal = Calendar.getInstance();
        Date month = null;
        try {
            month = dateFormat.parse(mCurrentYear + mCurrentMonth);
        } catch (ParseException e) {
            e.printStackTrace();
            Log.e(TAG, "dateFormat.parse daile  -> " + mCurrentYear + mCurrentMonth);
        }
        if (month != null){
            cal.setTime(month);
        }else {
            cal.setTime(new Date());
        }
        int maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH); 

        mDayWheel.setViewAdapter(new NumericWheelAdapter(getContext(), 1, maxDays, "%02d"));
        mDayWheel.setCyclic(true);
        mDayWheel.setInterpolator(new AnticipateOvershootInterpolator());
        mDayWheel.addChangingListener(new OnWheelChangedListener() {
            
            @Override
            public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
                String format = "%02d";
                mCurrentDay = String.format(format, newValue + 1);
            }
        });
        
        if(thisDay > maxDays){
            mDayWheel.setCurrentItem(maxDays - 1);
        }else {
            mDayWheel.setCurrentItem(thisDay - 1);
        }
        
    }
    
    public void setOnTimeClickListener(OnTimeClickListener listener){
    	
    	mListener = listener;
    }
    
    public interface OnTimeClickListener{
    	
    	public void onCommonTimeClick(int id);
    }
}
