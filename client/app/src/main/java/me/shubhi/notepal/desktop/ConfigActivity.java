package me.shubhi.notepal.desktop;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.shubhi.utils.ui.ToastUtils;
import me.shubhi.data.entity.Notebook;
import me.shubhi.data.schema.NoteSchema;
import me.shubhi.data.store.NotebookStore;
import me.shubhi.notepal.Constants;
import me.shubhi.notepal.R;
import me.shubhi.notepal.databinding.ActivityWidgetConfigurationBinding;
import me.shubhi.notepal.dialog.picker.NotebookPickerDialog;
import me.shubhi.notepal.util.AppWidgetUtils;

public class ConfigActivity extends AppCompatActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Notebook selectedNotebook;
    private SharedPreferences sharedPreferences;
    private ActivityWidgetConfigurationBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setResult(RESULT_CANCELED);
        super.onCreate(savedInstanceState);

        /* Setup the content layout. */
        binding = DataBindingUtil.inflate(getLayoutInflater(),
                R.layout.activity_widget_configuration, null, false);
        setContentView(binding.getRoot());
        binding.llFolder.setOnClickListener(view -> showNotebookPicker());
        binding.btnPositive.setOnClickListener(view -> onConfirm());

        /* Handle arguments. */
        handleArguments();
    }

    private void handleArguments() {
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        sharedPreferences = getApplication().getSharedPreferences(
                Constants.APP_WIDGET_PREFERENCES_NAME, Context.MODE_MULTI_PROCESS);
        String key = Constants.APP_WIDGET_PREFERENCE_KEY_NOTEBOOK_CODE_PREFIX + String.valueOf(mAppWidgetId);
        long notebookCode = sharedPreferences.getLong(key, 0);

        if (notebookCode != 0) {
            Disposable disposable = Observable.create((ObservableOnSubscribe<Notebook>) emitter -> {
                Notebook notebook = NotebookStore.getInstance().get(notebookCode);
                emitter.onNext(notebook);
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(notebook -> {
                selectedNotebook = notebook;
                updateWhenNotebookSelected();
            }, throwable -> ToastUtils.showShort(R.string.text_notebook_not_found));
        }
    }

    private void onConfirm() {
        Editor editor = sharedPreferences.edit();
        String sqlCondition = null;
        if (selectedNotebook != null) {
            sqlCondition = NoteSchema.TREE_PATH + " LIKE '" + selectedNotebook.getTreePath() + "'||'%'";
            String key = Constants.APP_WIDGET_PREFERENCE_KEY_NOTEBOOK_CODE_PREFIX + String.valueOf(mAppWidgetId);
            editor.putLong(key, selectedNotebook.getCode());
        }
        editor.putString(Constants.APP_WIDGET_PREFERENCE_KEY_SQL_PREFIX + String.valueOf(mAppWidgetId), sqlCondition).apply();
        AppWidgetUtils.notifyAppWidgets(getApplicationContext());
        finishWithOK();
    }

    private void showNotebookPicker() {
        NotebookPickerDialog.newInstance().setOnItemSelectedListener((dialog, notebook, position) -> {
            selectedNotebook = notebook;
            updateWhenNotebookSelected();
            dialog.dismiss();
        }).show(getSupportFragmentManager(), "NOTEBOOK_PICKER");
    }

    private void updateWhenNotebookSelected() {
        if (selectedNotebook != null) {
            binding.tvFolder.setText(selectedNotebook.getTitle());
            binding.tvFolder.setTextColor(selectedNotebook.getColor());
        }
    }

    private void finishWithOK() {
        Intent intent = new Intent();
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, intent);
        finish();
    }
}
