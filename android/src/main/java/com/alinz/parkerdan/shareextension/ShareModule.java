package com.alinz.parkerdan.shareextension;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;

import java.util.List;


public class ShareModule extends ReactContextBaseJavaModule {
    private static final String FIELD_VALUE = "uri";
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
            promise.resolve(processIntent());
        } catch (Exception ex) {
            promise.reject("Error", ex.getMessage());
        }
    }

    private WritableArray processIntent() throws Exception {
        WritableArray array = Arguments.createArray();

        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            throw new Exception("Activity doesn't exist");
        }

        Intent intent = currentActivity.getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            String type = intent.getType();
            Uri uri;

            if ("text/plain".equals(type)) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (uri == null) {
                    String value = intent.getStringExtra(Intent.EXTRA_TEXT);
                    if (value.startsWith("content://") || value.startsWith("file://")) {
                        uri = Uri.parse(value);
                    } else {
                        throw new Exception("Sharing simple text is not supported: " + value);
                    }
                }
            } else {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            }

            WritableMap metadata = getMetadata(uri);
            array.pushMap(metadata);
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for (Uri uri : uris) {
                WritableMap metadata = getMetadata(uri);
                array.pushMap(metadata);
            }
        } else {
            throw new Exception("Invalid intent action: " + action);
        }

        return array;
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
