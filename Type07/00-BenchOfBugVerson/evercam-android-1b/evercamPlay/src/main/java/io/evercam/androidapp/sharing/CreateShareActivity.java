package io.evercam.androidapp.sharing;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import io.evercam.androidapp.ParentAppCompatActivity;
import io.evercam.androidapp.R;
import io.evercam.androidapp.tasks.CreateShareTask;

public class CreateShareActivity extends ParentAppCompatActivity
{
    private static final String TAG = "CreateShareActivity";
    private Spinner mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_share);

        setUpDefaultToolbar();
        setHomeIconAsCancel();

        setUpRightsSpinner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_share_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_action_share:
                onShareMenuClicked();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setUpRightsSpinner()
    {
        mSpinner = (Spinner) findViewById(R.id.access_permission_spinner);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, RightsStatus.getDefaultItems(this));
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner);
        mSpinner.setAdapter(spinnerArrayAdapter);
    }

    private void onShareMenuClicked()
    {
        EditText usernameEditText = (EditText) findViewById(R.id.create_share_user_edit_text);
        EditText messageEditText = (EditText) findViewById(R.id.create_share_message_edit_text);
        String usernameText = usernameEditText.getText().toString();
        String messageText = messageEditText.getText().toString();
        if(!usernameText.isEmpty())
        {
            String selectedRights = mSpinner.getSelectedItem().toString();
            RightsStatus rightsStatus = new RightsStatus(this, selectedRights);
            CreateShareTask.launch(this, usernameText, SharingActivity.evercamCamera.getCameraId(),
                    rightsStatus.getRightString(), messageText);
        }
    }
}

