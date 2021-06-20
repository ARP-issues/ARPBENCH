package io.evercam.androidapp.sharing;

import android.content.Context;

import io.evercam.androidapp.R;

public class SharingStatus
{
    public enum SharingStatusEnum {PUBLIC, LINK, USER}

    private SharingStatusEnum mStatusEnum = SharingStatusEnum.USER;
    private boolean mIsDiscoverable;
    private boolean mIsPublic;
    private int mStatusStringId;
    private int mStatusDetailStringId;
    private int mImageResourceId;

    public SharingStatus(boolean isDiscoverable, boolean isPublic)
    {
        if(isPublic && isDiscoverable)
        {
            initByEnum(SharingStatusEnum.PUBLIC);
        }
        if(isPublic && !isDiscoverable)
        {
            initByEnum(SharingStatusEnum.LINK);
        }
        if(!isPublic && !isDiscoverable)
        {
            initByEnum(SharingStatusEnum.USER);
        }
    }

    public SharingStatus(SharingStatusEnum statusEnum)
    {
        initByEnum(statusEnum);
    }

    public SharingStatus(String statusString, Context context)
    {
        if(statusString.equals(context.getString(R.string.sharing_status_public)))
        {
            initByEnum(SharingStatusEnum.PUBLIC);
        }
        else if(statusString.equals(context.getString(R.string.sharing_status_link)))
        {
            initByEnum(SharingStatusEnum.LINK);
        }
        else if(statusString.equals(context.getString(R.string.sharing_status_specific_user)))
        {
            initByEnum(SharingStatusEnum.USER);
        }
    }

    private void initByEnum(SharingStatusEnum statusEnum)
    {
        setStatusEnum(statusEnum);

        switch(statusEnum)
        {
            case PUBLIC:
                setIsDiscoverable(true);
                setIsPublic(true);
                setImageResourceId(R.drawable.ic_globe);
                setStatusStringId(R.string.sharing_status_public);
                setStatusDetailStringId(R.string.sharing_status_detail_public);
                break;
            case LINK:
                setIsDiscoverable(false);
                setIsPublic(true);
                setImageResourceId(R.drawable.ic_link);
                setStatusStringId(R.string.sharing_status_link);
                setStatusDetailStringId(R.string.sharing_status_detail_link);
                break;
            case USER:
                setIsDiscoverable(false);
                setIsPublic(false);
                setImageResourceId(R.drawable.ic_fontawsome_users);
                setStatusStringId(R.string.sharing_status_specific_user);
                setStatusDetailStringId(R.string.sharing_status_detail_specific_user);
                break;
        }
    }

    public SharingStatusEnum getStatusEnum()
    {
        return mStatusEnum;
    }

    public void setStatusEnum(SharingStatusEnum mStatusEnum)
    {
        this.mStatusEnum = mStatusEnum;
    }

    public boolean isDiscoverable()
    {
        return mIsDiscoverable;
    }

    public void setIsDiscoverable(boolean mIsDiscoverable)
    {
        this.mIsDiscoverable = mIsDiscoverable;
    }

    public boolean isPublic()
    {
        return mIsPublic;
    }

    public void setIsPublic(boolean mIsPublic)
    {
        this.mIsPublic = mIsPublic;
    }

    public int getStatusStringId()
    {
        return mStatusStringId;
    }

    public void setStatusStringId(int statusStringId)
    {
        this.mStatusStringId = statusStringId;
    }

    public int getStatusDetailStringId()
    {
        return mStatusDetailStringId;
    }

    public void setStatusDetailStringId(int mStatusDetailStringId)
    {
        this.mStatusDetailStringId = mStatusDetailStringId;
    }

    public int getImageResourceId()
    {
        return mImageResourceId;
    }

    public void setImageResourceId(int mImageResourceId)
    {
        this.mImageResourceId = mImageResourceId;
    }
}
