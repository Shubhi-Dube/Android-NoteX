package me.shubhi.notepal.util;

import android.app.Activity;
import android.text.TextUtils;

import com.onedrive.sdk.core.ClientException;

import java.io.File;

import me.shubhi.commons.utils.NetworkUtils;
import me.shubhi.utils.ui.ToastUtils;
import me.shubhi.notepal.PalmApp;
import me.shubhi.notepal.R;
import me.shubhi.notepal.activity.SettingsActivity;
import me.shubhi.notepal.onedrive.OneDriveBackupService;
import me.shubhi.notepal.fragment.setting.SettingsBackup;
import me.shubhi.notepal.manager.FileManager;
import me.shubhi.notepal.onedrive.DefaultCallback;
import me.shubhi.notepal.onedrive.OneDriveManager;
import me.shubhi.notepal.common.preferences.SyncPreferences;

/**
 * Created by shouh on 2018/4/5.
 */
public class SynchronizeUtils {

    /**
     * Sync to One Drive.
     *
     * @param activity current activity
     */
    public static void syncOneDrive(Activity activity) {
        syncOneDrive(activity, false);
    }

    /**
     * Sync to One Drive
     *
     * @param activity current activity
     * @param force true to force to synchronize
     */
    public static void syncOneDrive(Activity activity, boolean force) {
        // If forced to synchronize and the information is not set, go to the setting page.
        if (!SynchronizeUtils.checkOneDriveSettings()) {
            if (force) {
                ToastUtils.showShort(R.string.setting_backup_onedrive_login_drive_message);
                SettingsActivity.open(SettingsBackup.class).launch(activity);
            }
            return;
        }

        // Only synchronize to OneDrive if forced to or arrived the time interval.
        if (force || SynchronizeUtils.shouldOneDriveSync()) {
            OneDriveManager.getInstance().connectOneDrive(activity, new DefaultCallback<Void>(activity) {
                @Override
                public void success(Void aVoid) {
                    ToastUtils.showShort(R.string.text_syncing);
                    OneDriveBackupService.start(activity);
                }

                @Override
                public void failure(ClientException error) {
                    ToastUtils.showShort(error.getMessage());
                }
            });
        }
    }

    /**
     * Check if the OneDrive synchronization information is set.
     *
     * @return true if set, otherwise false
     */
    private static boolean checkOneDriveSettings() {
        String itemId = SyncPreferences.getInstance().getOneDriveBackupItemId();
        String filesItemId = SyncPreferences.getInstance().getOneDriveFilesBackupItemId();
        return !TextUtils.isEmpty(itemId) && !TextUtils.isEmpty(filesItemId);
    }

    /**
     * Should synchronize to OneDrive according to time interval in settings.
     *
     * @return true if should synchronize.
     */
    private static boolean shouldOneDriveSync() {

        boolean isNetworkAvailable = NetworkUtils.isNetworkAvailable(PalmApp.getContext());
        boolean isWifi = NetworkUtils.isWifi(PalmApp.getContext());
        boolean isOnlyWifi = SyncPreferences.getInstance().isBackupOnlyInWifi();

        long lastSyncTime = SyncPreferences.getInstance().getOneDriveLastSyncTime();
        long syncTimeInterval = SyncPreferences.getInstance().getSyncTimeInterval().millis;

        return isNetworkAvailable && (!isOnlyWifi || isWifi) && lastSyncTime + syncTimeInterval < System.currentTimeMillis();
    }

    /**
     * Should sync database to OneDrive
     *
     * @return true if should sync.
     */
    public static boolean shouldOneDriveDatabaseSync() {
        long lastSyncTime = SyncPreferences.getInstance().getOneDriveDatabaseLastSyncTime();
        File database = FileManager.getDatabaseFile(PalmApp.getContext());
        long lastModifiedTime = database.lastModified();
        return lastModifiedTime > lastSyncTime;
    }

    public static boolean shouldOneDrivePreferencesSync() {
        long lastSyncTime = SyncPreferences.getInstance().getOneDrivePreferenceLastSyncTime();
        File preferences = FileManager.getPreferencesFile(PalmApp.getContext());
        long lastModifiedTime = preferences.lastModified();
        return lastModifiedTime > lastSyncTime;
    }
}
