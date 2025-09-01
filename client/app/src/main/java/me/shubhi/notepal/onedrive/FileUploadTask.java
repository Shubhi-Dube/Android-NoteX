package me.shubhi.notepal.onedrive;

import android.os.AsyncTask;

import com.onedrive.sdk.extensions.Item;

import java.io.File;

import me.shubhi.utils.stability.L;


public class FileUploadTask extends AsyncTask<File, Integer, String> {

    private String itemId;

    private String conflictBehavior;

    private OneDriveManager.UploadProgressCallback<Item> uploadProgressCallback;

    FileUploadTask(String itemId,
                   String conflictBehavior,
                   OneDriveManager.UploadProgressCallback<Item> uploadProgressCallback) {
        this.itemId = itemId;
        this.conflictBehavior = conflictBehavior;
        this.uploadProgressCallback = uploadProgressCallback;
    }

    @Override
    protected String doInBackground(File... files) {
        L.d(files.length);
        for (File file : files) {
            OneDriveManager.getInstance().upload(itemId, file, conflictBehavior, uploadProgressCallback);
        }
        return "executed";
    }
}
