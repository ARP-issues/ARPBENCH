package io.evercam.androidapp.sharing;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.evercam.CameraShare;
import io.evercam.CameraShareInterface;
import io.evercam.PatchCameraBuilder;
import io.evercam.androidapp.R;
import io.evercam.androidapp.custom.CustomedDialog;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.dto.AppUser;
import io.evercam.androidapp.tasks.FetchShareListTask;
import io.evercam.androidapp.tasks.PatchCameraTask;

public class SharingListFragment extends ListFragment
{
    private final String TAG = "SharingListFragment";

    private ImageView mSharingStatusImageView;
    private TextView mSharingStatusTextView;
    private TextView mSharingStatusDetailTextView;

    private ShareListArrayAdapter mShareAdapter;
    private List<CameraShareInterface> mShareList = new ArrayList<>();

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id)
    {
        //If list header is clicked
        if(position == 0)
        {
            SharingStatus status = new SharingStatus(SharingActivity.evercamCamera.isDiscoverable(),
                    SharingActivity.evercamCamera.isPublic());
            String selectedItem = getString(status.getStatusStringId());
            CustomedDialog.getShareStatusDialog(this, selectedItem).show();
        }
        else //If share item is clicked
        {
            CameraShareInterface shareInterface = mShareList.get(position - 1);

            CustomedDialog.getRightsStatusDialog(this, shareInterface).show();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        View headerView = getActivity().getLayoutInflater().inflate(R.layout.share_list_header,getListView(),false);

        mShareAdapter = new ShareListArrayAdapter(getActivity(),
                R.layout.share_list_item, mShareList);

        //Add header for the sharing status
        getListView().addHeaderView(headerView);
        //Remove divider from list
        getListView().setDivider(null);

        setListAdapter(mShareAdapter);

        mSharingStatusImageView = (ImageView) headerView.findViewById(R.id.share_status_icon_image_view);
        mSharingStatusTextView = (TextView) headerView.findViewById(R.id.sharing_status_text_view);
        mSharingStatusDetailTextView = (TextView) headerView.findViewById(R.id.sharing_status_detail_text_view);

        retrieveSharingStatusFromCamera();

        FetchShareListTask.launch(SharingActivity.evercamCamera.getCameraId(), getActivity());
    }

    public void updateShareListOnUi(ArrayList<CameraShareInterface> shareList)
    {
        mShareList.clear();
        mShareList.addAll(shareList);
        mShareAdapter.notifyDataSetChanged();

        updateMenuInSharingActivity();
    }

    public void retrieveSharingStatusFromCamera()
    {
        SharingStatus status = new SharingStatus(SharingActivity.evercamCamera.isDiscoverable(),
                SharingActivity.evercamCamera.isPublic());

        updateSharingStatusUi(status);
    }

    public void updateSharingStatusUi(SharingStatus status)
    {
        mSharingStatusImageView.setImageResource(status.getImageResourceId());
        mSharingStatusTextView.setText(status.getStatusStringId());
        mSharingStatusDetailTextView.setText(status.getStatusDetailStringId());
    }

    public void patchSharingStatusAndUpdateUi(SharingStatus status)
    {
        new PatchCameraTask(buildPatchCamera(status).build(),
                getActivity()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private PatchCameraBuilder buildPatchCamera(SharingStatus status)
    {
        PatchCameraBuilder patchCameraBuilder = new PatchCameraBuilder(SharingActivity.evercamCamera
                .getCameraId());
        patchCameraBuilder.setPublic(status.isPublic()).setDiscoverable(status.isDiscoverable());
        return patchCameraBuilder;
    }

    private void updateMenuInSharingActivity()
    {
        if(getActivity() instanceof SharingActivity)
        {
            AppUser defaultUser = AppData.defaultUser;
            if(defaultUser != null)
            {
                String username = defaultUser.getUsername();
                ((SharingActivity) getActivity()).showTransferMenu(isOwner(username));
            }
        }
    }

    /**
     * TODO: Update the logic after adding owner info in the share list
     */
    private boolean isOwner(String username)
    {
        boolean userExists = false;
        if(mShareList.size() > 0)
        {
            for(CameraShareInterface shareInterface : mShareList)
            {
                String userId = "";
                if(shareInterface instanceof CameraShare)
                {
                    userId = ((CameraShare) shareInterface).getUserId();
                }

                if(userId.equals(username)) userExists = true;
            }
        }

        return !userExists;
    }

    public ArrayList<String> getUsernameList()
    {
        ArrayList<String> usernameList = new ArrayList<>();
        if(mShareList.size() > 0)
        {
            for(CameraShareInterface shareInterface : mShareList)
            {
                if(shareInterface instanceof CameraShare)
                {
                    usernameList.add(((CameraShare) shareInterface).getUserId());
                }
            }
        }
        return usernameList;
    }
}