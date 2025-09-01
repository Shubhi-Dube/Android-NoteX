package me.shubhi.notepal.onedrive;

import android.os.AsyncTask;

import me.shubhi.data.store.AttachmentsStore;
import me.shubhi.notepal.common.preferences.SyncPreferences;

public class ClearBackupStateTask extends AsyncTask<Void, Void, Void>{

    @Override
    protected Void doInBackground(Void... voids) {
        SyncPreferences syncPreferences = SyncPreferences.getInstance();
        syncPreferences.setOneDriveLastSyncTime(0);
        syncPreferences.setOneDriveDatabaseLastSyncTime(0);
        syncPreferences.setOneDrivePreferenceLastSyncTime(0);
        AttachmentsStore.getInstance().clearOneDriveBackupState();
        return null;
    }
}
