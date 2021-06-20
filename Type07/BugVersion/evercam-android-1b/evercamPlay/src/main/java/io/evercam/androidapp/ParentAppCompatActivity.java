package io.evercam.androidapp;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.logentries.android.AndroidLogger;
import com.nineoldandroids.view.ViewHelper;
import com.splunk.mint.Mint;

import io.evercam.androidapp.feedback.MixpanelHelper;
import io.evercam.androidapp.utils.Constants;
import io.evercam.androidapp.utils.PropertyReader;

public class ParentAppCompatActivity extends AppCompatActivity
{
    private PropertyReader propertyReader;

    private static MixpanelHelper mixpanelHelper;

    protected Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        propertyReader = new PropertyReader(this);

        initBugSense();

        mixpanelHelper = new MixpanelHelper(this, propertyReader);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader.KEY_SPLUNK_MINT))
            {
                Mint.startSession(this);
            }
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader.KEY_SPLUNK_MINT))
            {
                Mint.closeSession(this);
            }
        }

        getMixpanel().flush();
    }

    public PropertyReader getPropertyReader()
    {
        return propertyReader;
    }

    /**
     * @return the Mixpanel helper class
     */
    public static MixpanelHelper getMixpanel()
    {
        mixpanelHelper.registerSuperProperty("Client-Type", "Play-Android");

        return mixpanelHelper;
    }

    private void initBugSense()
    {
        if(Constants.isAppTrackingEnabled)
        {
            if(propertyReader.isPropertyExist(PropertyReader.KEY_SPLUNK_MINT))
            {
                String bugSenseCode = propertyReader.getPropertyStr(PropertyReader
                        .KEY_SPLUNK_MINT);
                Mint.initAndStartSession(this,bugSenseCode);
            }
        }
    }

    public static void sendToMint(Exception e)
    {
        if(Constants.isAppTrackingEnabled)
        {
            Mint.logException(e);
        }
    }

    public void sendToLogentries(AndroidLogger logger, String message)
    {
        if(logger != null)
        {
            logger.info(message);
        }
    }

    public static void sendWithMsgToMint(String messageName, String message, Exception e)
    {
        Mint.logExceptionMessage(messageName, message, e);
    }

    protected boolean toolbarIsShown()
    {
        return ViewHelper.getTranslationY(mToolbar) == 0;
    }

    protected boolean toolbarIsHidden()
    {
        return ViewHelper.getTranslationY(mToolbar) == -mToolbar.getHeight();
    }

    protected void showToolbar()
    {
        moveToolbar(0);
    }

    protected void hideToolbar()
    {
        moveToolbar(-mToolbar.getHeight());
    }

    protected void moveToolbar(float toTranslationY)
    {
        if (ViewHelper.getTranslationY(mToolbar) == toTranslationY)
        {
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(ViewHelper.getTranslationY(mToolbar), toTranslationY).setDuration(200);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
        {
            @Override
            public void onAnimationUpdate(ValueAnimator animation)
            {
                float translationY = (float) animation.getAnimatedValue();
                ViewHelper.setTranslationY(mToolbar, translationY);
            }
        });
        animator.start();
    }

    protected void setGradientTitleBackground()
    {
        if(mToolbar != null)
        {
            mToolbar.setBackgroundResource(R.drawable.gradient_title);
        }
    }

    protected void setOpaqueTitleBackground()
    {
        if(mToolbar != null)
        {
            mToolbar.setBackgroundColor(getResources().getColor(R.color.dark_gray_background));
        }
    }

    /**
     *  Basic tool bar set up, with opaque background
     */
    protected void setUpBasicToolbar()
    {
        mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setOpaqueTitleBackground();
        setSupportActionBar(mToolbar);
    }

    /**
     *  Default tool bar that apply for most activities -
     *  With opaque background and home as up button
     */
    protected void setUpDefaultToolbar()
    {
        setUpBasicToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void setUpGradientToolbarWithHomeButton()
    {
        mToolbar = (Toolbar) findViewById(R.id.tool_bar);
        setGradientTitleBackground();
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void setHomeIconAsCancel()
    {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_cancel_padding);
        }
    }

    protected void updateTitleText(int textId)
    {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setTitle(textId);
        }
    }

    protected void updateTitleText(String title)
    {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
        {
            actionBar.setTitle(title);
        }
    }
}
