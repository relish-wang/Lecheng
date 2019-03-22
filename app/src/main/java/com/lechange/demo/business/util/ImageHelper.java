package com.lechange.demo.business.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.LruCache;

import com.lechange.demo.business.util.TaskPoolHelper.RunnableTask;
import com.lechange.opensdk.utils.LCOpenSDK_Utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageHelper {

    private final static String TAG = "ImageHelper";
    private static Options mDefaultOption;
    //最近最少使用算法的缓存策略
    private static LruCache<String, Drawable> mImageCache = new LruCache<String, Drawable>(100);

    static {
        mDefaultOption = new Options();
        //demo里面为了降低使用内存，图片缩小了一倍
        mDefaultOption.inSampleSize = 2;
        mDefaultOption.inPreferredConfig = Config.RGB_565;
    }


    public static void loadRealImage(final String url, final Handler handler) {
        downloadImage(url, "real", handler);
    }

    /**
     * 加载普通图片(缓存)
     *
     * @param url
     * @param key
     * @param handler
     */
    public static void loadCacheImage(final String url, final Handler handler) {
        String[] imageIDBuffer = url.split("[/?]");
        final String imageID = imageIDBuffer[imageIDBuffer.length - 2];
        Drawable drawable = mImageCache.get(imageID);
        if (drawable != null) {
            Message msg = new Message();
            msg.what = url.hashCode();
            msg.obj = drawable;
            handler.handleMessage(msg);
        } else {
            downloadImage(url, imageID, handler);
        }
    }


    /**
     * 加载加密图片(实时)
     *
     * @param url
     * @param key
     * @param handler
     */
    public static void loadRealImage(final String url, String deviceId, String key, final Handler handler) {
        downloadImage(url, "real", deviceId, key, handler);
    }


    /**
     * 加载加密图片(缓存)
     *
     * @param url
     * @param key
     * @param handler
     */
    public static void loadCacheImage(final String url, final String deviceId, String key, final Handler handler) {
        String[] imageIDBuffer = url.split("[/?]");

        // FIXME java.lang.ArrayIndexOutOfBoundsException: length=1; index=-1 at com.lechange.demo.business.util.ImageHelper.loadCacheImage(ImageHelper.java:83)
        if (imageIDBuffer.length < 2) return;

        final String imageID = imageIDBuffer[imageIDBuffer.length - 2];
        Drawable drawable = mImageCache.get(imageID);
        if (drawable != null) {
            Message msg = new Message();
            msg.what = url.hashCode();
            msg.obj = drawable;
            handler.handleMessage(msg);
        } else {
            downloadImage(url, imageID, deviceId, key, handler);
        }
    }


    /**
     * 下载普通图片任务
     *
     * @param url
     * @param imageID
     * @param handler
     */
    private static void downloadImage(final String url, final String imageID, final Handler handler) {
        TaskPoolHelper.addTask(new RunnableTask(imageID) {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Drawable drawable = null;
                try {
                    //创建一个url对象  
                    URL resurl = new URL(url);
                    //设置超时时间
                    HttpURLConnection urlConn = (HttpURLConnection) resurl.openConnection();
                    urlConn.setConnectTimeout(5000);
                    urlConn.setReadTimeout(5000);
                    //打开URL对应的资源输入流  
                    InputStream is = urlConn.getInputStream();
                    //从InputStream流中解析出图片 
                    Bitmap bitmap = BitmapFactory.decodeStream(is, null, mDefaultOption);
                    if (bitmap != null) {
                        drawable = new BitmapDrawable(bitmap);
                    }
                    //加入缓存
                    mImageCache.put(imageID, drawable);
                    //关闭输入流  
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 解析设备列表信息
                handler.obtainMessage(url.hashCode(), drawable).sendToTarget(); // 发送成功消息到界面
            }
        });
    }

    /**
     * 下载加密图片任务
     *
     * @param url
     * @param imageID
     * @param key
     * @param handler
     */
    private static void downloadImage(final String url, final String imageID, final String deviceId, final String key, final Handler handler) {
        TaskPoolHelper.addTask(new RunnableTask(imageID) {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                Drawable drawable = null;
                try {
                    //创建一个url对象  
                    URL resurl = new URL(url);
                    //设置超时时间
                    HttpURLConnection urlConn = (HttpURLConnection) resurl.openConnection();
                    urlConn.setConnectTimeout(5000);
                    urlConn.setReadTimeout(5000);
                    //打开URL对应的资源输入流  
                    InputStream is = urlConn.getInputStream();
                    //从InputStream流中解析出图片  
                    ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
                    byte[] buff = new byte[500];
                    int rc = 0;
                    int length = 0;
                    while ((rc = is.read(buff, 0, 500)) > 0) {
                        length += rc;
                        swapStream.write(buff, 0, rc);
                    }

                    byte[] srcBuf = swapStream.toByteArray();
                    byte[] dstBuf = new byte[500000];
                    int[] dstLen = new int[1];
                    dstLen[0] = 500000;
                    Bitmap bitmap; //=null
                    switch (LCOpenSDK_Utils.decryptPic(srcBuf, length, deviceId, key, dstBuf, dstLen)) {
                        case 0:  //解密成功
                            bitmap = BitmapFactory.decodeByteArray(dstBuf, 0, dstLen[0], mDefaultOption);
                            if (bitmap != null) {
                                drawable = new BitmapDrawable(bitmap);
                            }
                            break;
                        case 3: //图片非加密
                            bitmap = BitmapFactory.decodeByteArray(srcBuf, 0, length, mDefaultOption);
                            if (bitmap != null) {
                                drawable = new BitmapDrawable(bitmap);
                            }
                            break;
                        default: //解密失败
                            break;
                    }
                    //加入缓存
                    mImageCache.put(imageID, drawable);
                    //关闭输入流
                    is.close();
                    swapStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // 解析设备列表信息
                handler.obtainMessage(url.hashCode(), drawable).sendToTarget(); // 发送成功消息到界面
            }
        });
    }

    public static void clear() {
        TaskPoolHelper.clearTask();
        mImageCache.evictAll();
    }
}
