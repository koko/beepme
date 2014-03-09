/*
This file is part of BeepMe.

BeepMe is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

BeepMe is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with BeepMe. If not, see <http://www.gnu.org/licenses/>.

Copyright since 2012 Michael Glanznig
http://beepme.glanznig.com
*/

package com.glanznig.beepme.helper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.glanznig.beepme.BeeperApp;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

public class PhotoUtils {
	
	private static final String TAG = "PhotoUtils";
	
	private static final String PHOTO_PREFIX = "beeper_img_";
	private static final String PHOTO_THUMB_SUFFIX = "_thumb_";
	private static final String CHANGE_PHOTO_NAME = "swap";
	
	private static final String NORMAL_MODE_DIR = "normal";
	private static final String TEST_MODE_DIR = "testmode";
	private static final String THUMB_DIR = "thumbs";
	
	private static final int[] thumbSizes = {48, 64};
	
	public static final int TAKE_PHOTO_INTENT = 3648;
	public static final int CHANGE_PHOTO_INTENT = 3638;
	public static final String EXTRA_KEY = MediaStore.EXTRA_OUTPUT;
	
	public static Intent getTakePhotoIntent(Context ctx, Date timestamp) {
		return getPhotoIntent(ctx, timestamp);
	}
	
	public static Intent getChangePhotoIntent(Context ctx) {
		return getPhotoIntent(ctx, null);
	}
	
	private static Intent getPhotoIntent(Context ctx, Date timestamp) {
		BeeperApp app = (BeeperApp)ctx.getApplicationContext();
		
		// external storage is ready and writable - can be used
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File picDir = null;
			
			// add a sub directory depending on whether we are in test mode 
			if (!app.getPreferences().isTestMode()) {
				picDir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), NORMAL_MODE_DIR);
			}
			else {
				picDir = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES), TEST_MODE_DIR);
			}
			
			String picFilename = null;
			if (timestamp != null) {
				picFilename = PHOTO_PREFIX + new SimpleDateFormat("yyyyMMddHHmmss").format(timestamp) + ".jpg";
			}
			else {
				picFilename = PHOTO_PREFIX + CHANGE_PHOTO_NAME + ".jpg";
			}
			
			File pictureFile = new File(picDir, picFilename);
			try {
                if(pictureFile.exists() == false) {
                    pictureFile.getParentFile().mkdirs();
                    pictureFile.createNewFile();
                }
            } catch (IOException e) {
            	Log.e(TAG, "unable to create file.", e);
            	return null;
            }
			
			Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePic = takePic.putExtra(EXTRA_KEY, Uri.fromFile(pictureFile));
			
			return takePic;
		}
		
		return null;
	}
	
	public static Bitmap getBitmap(Context ctx, String uri) {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	    	try {
	    		return MediaStore.Images.Media.getBitmap(
	    				ctx.getContentResolver(), Uri.fromFile(new File(uri)));
	    	}
	    	catch(IOException ioe) {
	    		Log.e(TAG, ioe.getMessage());
	    	}
		}
		
		return null;
	}
	
	public static boolean swapPhoto(Context ctx, Date timestamp) {
		BeeperApp app = (BeeperApp)ctx.getApplicationContext();
		
		// external storage is ready and writable - can be used
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File picDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
			
			// add a sub directory depending on whether we are in test mode 
			if (!app.getPreferences().isTestMode()) {
				picDir = new File(picDir.getAbsolutePath() + File.separator + "normal");
			}
			else {
				picDir = new File(picDir.getAbsolutePath() + File.separator + "testmode");
			}
			
			String picFilename = PHOTO_PREFIX + new SimpleDateFormat("yyyyMMddHHmmss").format(timestamp) + ".jpg";
			String swapFilename = PHOTO_PREFIX + CHANGE_PHOTO_NAME + ".jpg";
			
			File pictureFile = new File(picDir, picFilename);
			File swapFile = new File(picDir, swapFilename);
			
			if (pictureFile.delete()) {
				return swapFile.renameTo(pictureFile);
			}
		}
		
		return false;
	}
	
	public static boolean deletePhoto(String uri) {
		return deletePhoto(uri, false);
	}
	
	public static boolean deleteThumbnails(String uri) {
		return deletePhoto(uri, true);
	}
	
	private static boolean deletePhoto(String uri, boolean thumbnailsOnly) {
		if (uri != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			
			File photo = new File(uri);
			if (photo != null) {
				String path = photo.getParent();
				
				// delete thumbnails
				for (int i = 0; i < thumbSizes.length; i++) {
					// get name without .jpg extension
					String thumbPath = path + File.separator + THUMB_DIR;
					String name = photo.getName().substring(0, photo.getName().length() - 5);
					name = name + PHOTO_THUMB_SUFFIX + thumbSizes[i] + ".jpg";
					File thumb = new File(thumbPath, name);
					thumb.delete();
				}
				
				if (!thumbnailsOnly) {
					photo.delete();
				}
			}
			return true;
		}
		return false;
	}
	
	public static boolean deleteSwapPhoto(Context ctx) {
		BeeperApp app = (BeeperApp)ctx.getApplicationContext();
		
		// external storage is ready and writable - can be used
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File picDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
			
			// add a sub directory depending on whether we are in test mode 
			if (!app.getPreferences().isTestMode()) {
				picDir = new File(picDir.getAbsolutePath() + File.separator + "normal");
			}
			else {
				picDir = new File(picDir.getAbsolutePath() + File.separator + "testmode");
			}
			
			String picFilename = PHOTO_PREFIX + CHANGE_PHOTO_NAME + ".jpg";
			File pictureFile = new File(picDir, picFilename);
			if (pictureFile != null) {
				deletePhoto(pictureFile.getAbsolutePath());
				return true;
			}
		}
		
		return false;
	}
	
	public static void regenerateThumbnails(Context ctx, String uri, Handler handler) {
		deleteThumbnails(uri);		
		final float scale = ctx.getResources().getDisplayMetrics().density;
		for (int i = 0; i < thumbSizes.length; i++) {
			generateThumbnail(uri, thumbSizes[i], (int)(thumbSizes[i] * scale + 0.5f), handler);
		}
	}
	
	public static void generateThumbnails(Context ctx, String uri, Handler handler) {
		final float scale = ctx.getResources().getDisplayMetrics().density;
		for (int i = 0; i < thumbSizes.length; i++) {
			generateThumbnail(uri, thumbSizes[i], (int)(thumbSizes[i] * scale + 0.5f), handler);
		}
	}
	
	public static void generateThumbnail(String uri, int thumbName, int size, Handler handler) {
		if (uri != null && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File photo = new File(uri);
			if (photo != null) {
				String path = photo.getParent() + File.separator + THUMB_DIR;
				// get name without .jpg extension
				String name = photo.getName().substring(0, photo.getName().length() - 5);
				name = name + PHOTO_THUMB_SUFFIX + thumbName + ".jpg";
				photo = new File(path, name);
				
				// create thumbs dir if it not already exists
				if(photo.getParentFile().exists() == false) {
                    photo.getParentFile().mkdirs();
				}
				
				if (photo != null) {
					AsyncImageScaler scaler = new AsyncImageScaler(uri, photo.getAbsolutePath(), thumbName, size, size, handler);
					scaler.start();
				}
			}
		}
	}
	
	public static String getThumbnailUri(String photoUri, int size) {
		if (photoUri != null) {
			File photo = new File(photoUri);
			String path = photo.getParent() + File.separator + THUMB_DIR;
			// get name without .jpg extension
			String name = photo.getName().substring(0, photo.getName().length() - 5);
			name = name + PHOTO_THUMB_SUFFIX + size + ".jpg";
			photo = new File(path, name);
			
			return photo.getAbsolutePath();
		}
		return null;
	}
	
	public static boolean isEnabled(Context ctx) {
		boolean enabled = true;
		
		//check if device has camera feature
        if (!ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
        	enabled = false;
        }
        else {
        	//check if device has app for taking images
        	List<ResolveInfo> availApps = ctx.getPackageManager().queryIntentActivities(
        			new Intent(MediaStore.ACTION_IMAGE_CAPTURE), PackageManager.MATCH_DEFAULT_ONLY);
        	if (availApps.size() == 0) {
        		enabled = false;
        	}
        	//check if image can be saved to external storage
        	else if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
        		enabled = false;
        	}
        }
		
		return enabled;
	}

}
