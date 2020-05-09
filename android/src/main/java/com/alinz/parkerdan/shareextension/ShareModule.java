package com.alinz.parkerdan.shareextension;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import android.graphics.Bitmap;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.io.InputStream;


public class ShareModule extends ReactContextBaseJavaModule {
    private static final String FIELD_VALUE = "value"; // uri
    private static final String FIELD_NAME = "name";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_SIZE = "size";

    public ShareModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "ReactNativeShareExtension";
    }

    @ReactMethod
    public void close() {
        getCurrentActivity().finish();
    }

    @ReactMethod
    public void data(Promise promise) {
        try {
            WritableMap result = processIntent();
            promise.resolve(result);
        } catch (Exception ex) {
            String message = ex.getMessage();
            promise.reject("Error", message);
        }
    }

    public WritableMap processIntent() throws Exception {
        WritableMap map = Arguments.createMap();

        String value = "";
        String type = "";
        int size = 0;

        Activity currentActivity = getCurrentActivity();

        if (currentActivity != null) {
            Intent intent = currentActivity.getIntent();
            String action = intent.getAction();
            type = intent.getType();

            if (Intent.ACTION_SEND.equals(action)) {
                Uri uri;
                if ("text/plain".equals(type)) {
                    value = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (!value.startsWith("content://") || !value.startsWith("file://")) {
                        throw new Exception("Invalid Intent Text: " + value);
                    }

                    uri = Uri.parse(value);
                } else {
                    uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
//                      value = "file://" + RealPathUtil.getRealPathFromURI(currentActivity, uri);
                }
                return getMetadata(uri);
            } else {
                throw new Exception("Invalid Intent Action: " + action);
            }
        }

        // return default values
        map.putString(FIELD_TYPE, type);
        map.putString(FIELD_VALUE, value);
        map.putInt(FIELD_SIZE, size);
        map.putString(FIELD_NAME, value);
        return map;
    }

    // Copied from io/github/elyx0/reactnativedocumentpicker/DocumentPickerModule.java
    private WritableMap getMetadata(Uri uri) {
        WritableMap map = Arguments.createMap();

        map.putString(FIELD_VALUE, uri.toString());

        ContentResolver contentResolver = getReactApplicationContext().getContentResolver();

        map.putString(FIELD_TYPE, contentResolver.getType(uri));

        Cursor cursor = contentResolver.query(uri, null, null, null, null, null);

        try {
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (!cursor.isNull(displayNameIndex)) {
                    map.putString(FIELD_NAME, cursor.getString(displayNameIndex));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    int mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
                    if (!cursor.isNull(mimeIndex)) {
                        map.putString(FIELD_TYPE, cursor.getString(mimeIndex));
                    }
                }

                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (!cursor.isNull(sizeIndex)) {
                    map.putInt(FIELD_SIZE, cursor.getInt(sizeIndex));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return map;
    }
}
