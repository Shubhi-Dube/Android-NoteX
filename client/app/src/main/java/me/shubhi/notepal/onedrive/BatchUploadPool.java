package me.shubhi.notepal.onedrive;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.shubhi.data.entity.Attachment;
import me.shubhi.notepal.common.preferences.SyncPreferences;
import me.shubhi.utils.stability.L;

/**
 * todo 1. Find out why two files are not synchronized to OneDrive; 2. Test new strategy.
 *
 */
public class BatchUploadPool {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    /**
     * Thread number region at least 2 and at most 4. Real thread number is CORE_POOL_SIZE + 1 */
    private static final int CORE_POOL_SIZE = Math.max(2, Math.min(CPU_COUNT - 1, 3));

    /**
     * Max upload files count for one shot. */
    private static final int MAX_BATCH_UPLOAD_COUNT = 20;

    private ExecutorService executor;

    private String itemId;

    private int filesUploaded = 0;

    private List<Attachment> attachmentToUpload;

    private static BatchUploadPool instance;

    public static BatchUploadPool getInstance(String itemId) {
        if (instance == null) {
            synchronized (BatchUploadPool.class) {
                if (instance == null) {
                    instance = new BatchUploadPool(itemId);
                }
            }
        }
        return instance;
    }

    private BatchUploadPool(String itemId) {
        this.itemId = itemId;
    }

    void begin() {
        this.executor = Executors.newFixedThreadPool(CORE_POOL_SIZE + 1);
        new GetUploadAttachmentTask(this::onGetAttachment).execute(MAX_BATCH_UPLOAD_COUNT);
    }

    private void onGetAttachment(List<Attachment> attachments) {
        attachmentToUpload = attachments;
        doTask();
    }

    boolean isTerminated() {
        return executor == null || executor.isTerminated();
    }

    private void shutDown() {
        executor.shutdown();
    }

    private void doTask() {
        /* Latch to control the upload event. */
        int countDownLatch = attachmentToUpload.size() > MAX_BATCH_UPLOAD_COUNT ? MAX_BATCH_UPLOAD_COUNT : attachmentToUpload.size();
        CountDownLatch downLatch = new CountDownLatch(countDownLatch);
        // Submit a new task to watch the executor.
        executor.submit(new FileUploadWatcher(downLatch, new FileUploadWatcher.OnWatchListener() {
            @Override
            public void onFinish() {
                L.d("All uploaded!");
                SyncPreferences.getInstance().setOneDriveLastSyncTime(System.currentTimeMillis());
                shutDown();
            }

            @Override
            public void onFail(String msg) {
                L.e("Error in watcher : " + msg);
            }
        }));
        // Start upload for the first time
        batchUpload(downLatch);
    }

    private void batchUpload(CountDownLatch countDownLatch) {
        L.d("Batch upload started!");
        for (Attachment attachment : attachmentToUpload) {
            L.d(attachment.getCode() + " to upload.");
            executor.submit(new FileUploadRunnable(attachment, itemId, countDownLatch, new FileUploadRunnable.OnUploadListener() {
                @Override
                public void onSuccess() {
                    L.d(attachment.getCode() + " uploaded.");
                    L.d(++filesUploaded + " files are synchronized.");
                }

                @Override
                public void onFail(String msg) {
                    L.e("Error when synchronize file : " + msg);
                }
            }));
        }
    }
}
