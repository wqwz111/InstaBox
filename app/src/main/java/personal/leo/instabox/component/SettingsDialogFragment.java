package personal.leo.instabox.component;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;

import personal.leo.instabox.R;


public class SettingsDialogFragment extends DialogFragment {
    private static final String PREFERENCES_NAME = "settings";
    private static final String PREF_KEY_NETWORK = "network";
    private static final String PREF_KEY_AUTO_DOWNLOAD = "auto_download";

    private boolean mAllowedOverMetered;
    private boolean mAllowedAutoDownload;

    public interface SettingsDialogListener {
        void onDismiss(boolean allowedOverMetered, boolean allowedAutoDownload);
    }

    SettingsDialogListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (SettingsDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement SettingsDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        SharedPreferences settings = getActivity().getSharedPreferences(PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        View view = getActivity().getLayoutInflater().inflate(R.layout.settings_dialog, null);
        final Switch networkSwitch = (Switch) view.findViewById(R.id.network_usage_switch);
        final Switch autoDownloadSwitch = (Switch) view.findViewById(R.id.auto_download_switch);
        networkSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAllowedOverMetered = isChecked;
            }
        });
        autoDownloadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAllowedAutoDownload = isChecked;
            }
        });

        networkSwitch.setChecked(settings.getBoolean(PREF_KEY_NETWORK, true));
        autoDownloadSwitch.setChecked(settings.getBoolean(PREF_KEY_AUTO_DOWNLOAD, true));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mListener.onDismiss(mAllowedOverMetered, mAllowedAutoDownload);
    }
}
