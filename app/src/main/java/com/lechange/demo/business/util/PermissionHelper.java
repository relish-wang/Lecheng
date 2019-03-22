package com.lechange.demo.business.util;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

/**
 * @author 31833
 * @version Version    Time                 Description<br>
 *          1.0        2017/4/27 15:42      运行时权限申请帮助类
 */

public class PermissionHelper  {

    private static boolean isHasPermission(Context context,  String permission){

       if(Build.VERSION.SDK_INT < 23) {
            return true;
        }

       return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
     
    }


    public static void requestPermission(Activity activity, String[] permissions) {
    	boolean flag = true;
    	
    	for (String permission : permissions) {
    		if(isHasPermission(activity,permission)){
    			flag = false;
    			break;
    		}
    		continue;
		}
    	
        if(flag) {
            ActivityCompat.requestPermissions(activity, permissions, permissions.length);
        }
    }
}
