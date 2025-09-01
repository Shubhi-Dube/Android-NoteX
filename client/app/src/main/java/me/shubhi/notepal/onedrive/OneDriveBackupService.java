package me.shubhi.notepal.onedrive;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.onedrive.sdk.extensions.Item;

import java.io.File;

import me.shubhi.commons.utils.NetworkUtils;
import me.shubhi.data.DBConfig;
import me.shubhi.notepal.common.preferences.SyncPreferences;
import me.shubhi.notepal.manager.FileManager;
import me.shubhi.notepal.util.SynchronizeUtils;
import me.shubhi.utils.stability.L;


public class OneDriveBackupService extends IntentService {

    private SyncPreferences syncPreferences;

    public static void start(Context context) {
        Intent service = new Intent(context, OneDriveBackupService.class);
        context.startService(service);
    }

    public OneDriveBackupService() {
        super("OneDriveBackupService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        syncPreferences = SyncPreferences.getInstance();

        boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(getApplicationContext());
        boolean isWifi = NetworkUtils.isWifi(getApplicationContext());
        boolean isOnlyWifi = syncPreferences.isBackupOnlyInWifi();

        if (isNetworkAvailable && (!isOnlyWifi || isWifi)) {
            uploadDatabaseAndPreferences();
            updateAttachments();
        }
    }

    private void uploadDatabaseAndPreferences() {
        String itemId = syncPreferences.getOneDriveBackupItemId();
        if (!TextUtils.isEmpty(itemId)) {
            if (SynchronizeUtils.shouldOneDriveDatabaseSync()) {
                uploadDatabase(itemId);
            }
            if (SynchronizeUtils.shouldOneDrivePreferencesSync()) {
                uploadPreferences(itemId);
            }
        }
    }

    private void updateAttachments() {
        String filesItemId = syncPreferences.getOneDriveFilesBackupItemId();
        if (!TextUtils.isEmpty(filesItemId)) {
            BatchUploadPool batchUploadPool = BatchUploadPool.getInstance(filesItemId);
            if (batchUploadPool.isTerminated()) {
                batchUploadPool.begin();
            }
        } else {
            L.e("Error! No files backup item id.");
        }
    }

    private void uploadDatabase(String itemId) {
        File database = getDatabasePath(DBConfig.DATABASE_NAME);
        new FileUploadTask(itemId, OneDriveConstants.CONFLICT_BEHAVIOR_REPLACE, new OneDriveManager.UploadProgressCallback<Item>() {
            @Override
            public void success(Item item) {
                syncPreferences.setOneDriveDatabaseItemId(item.id);
                syncPreferences.setOneDriveDatabaseLastSyncTime(System.currentTimeMillis());
                syncPreferences.setOneDriveLastSyncTime(System.currentTimeMillis());
            }

            @Override
            public void failure(Exception e) {
                L.e(e);
            }
        }).execute(database);
    }

    private void uploadPreferences(String itemId) {
        File preferences = FileManager.getPreferencesFile(this);
        new FileUploadTask(itemId, OneDriveConstants.CONFLICT_BEHAVIOR_REPLACE, new OneDriveManager.UploadProgressCallback<Item>() {

            @Override
            public void success(Item item) {
                syncPreferences.setOneDrivePreferencesItemId(item.id);
                syncPreferences.setOneDrivePreferenceLastSyncTime(System.currentTimeMillis());
                syncPreferences.setOneDriveLastSyncTime(System.currentTimeMillis());
            }

            @Override
            public void failure(Exception e) {
                L.e(e);
            }
        }).execute(preferences);
    }
}
