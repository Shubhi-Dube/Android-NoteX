package me.shubhi.notepal.fragment.setting;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import me.shubhi.commons.event.PageName;
import me.shubhi.commons.event.RxBus;
import me.shubhi.commons.event.RxMessage;
import me.shubhi.commons.event.*;
import me.shubhi.commons.fragment.BPreferenceFragment;
import me.shubhi.notepal.R;

import static me.shouheng.commons.event.RxMessage.CODE_NOTE_LIST_STYLE_CHANGED;


@PageName(name = UMEvent.PAGE_SETTING_NOTE)
public class SettingsNote extends BPreferenceFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) actionBar.setTitle(R.string.setting_category_universal_note);

        addPreferencesFromResource(R.xml.preferences_note);

        findPreference(R.string.key_note_expanded_note).setOnPreferenceClickListener(preference -> {
            RxBus.getRxBus().post(new RxMessage(CODE_NOTE_LIST_STYLE_CHANGED, null));
            return true;
        });
    }
}
