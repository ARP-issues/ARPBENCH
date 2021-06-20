package io.evercam.androidapp.sharing;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.ArrayList;

import io.evercam.androidapp.R;
import io.evercam.androidapp.custom.CustomedDialog;

public class TransferOwnerDialogFragment extends DialogFragment
{
    private String mCameraId = "";
    private ArrayList<String> mUserList = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.transfer_dialog_title)
                .setMessage(R.string.transfer_dialog_content)
                .setPositiveButton(R.string.transfer, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        CustomedDialog.getSelectNewOwnerDialog(getActivity(), mUserList).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null).create();
    }

    public TransferOwnerDialogFragment setCameraId(String cameraId)
    {
        this.mCameraId = cameraId;
        return this;
    }

    public TransferOwnerDialogFragment setUserList(ArrayList<String> userList)
    {
        this.mUserList = userList;
        return this;
    }
}
