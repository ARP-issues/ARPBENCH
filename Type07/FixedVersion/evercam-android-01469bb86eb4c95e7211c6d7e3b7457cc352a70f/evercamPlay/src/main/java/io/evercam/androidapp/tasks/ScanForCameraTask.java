package io.evercam.androidapp.tasks;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.evercam.Vendor;
import io.evercam.androidapp.ScanActivity;
import io.evercam.androidapp.dto.AppData;
import io.evercam.androidapp.feedback.ScanFeedbackItem;
import io.evercam.androidapp.utils.Commons;
import io.evercam.androidapp.utils.NetInfo;
import io.evercam.network.EvercamDiscover;
import io.evercam.network.IdentifyCameraRunnable;
import io.evercam.network.NatRunnable;
import io.evercam.network.OnvifRunnable;
import io.evercam.network.UpnpRunnable;
import io.evercam.network.discovery.DiscoveredCamera;
import io.evercam.network.discovery.IpScan;
import io.evercam.network.discovery.NatMapEntry;
import io.evercam.network.discovery.NetworkInfo;
import io.evercam.network.discovery.ScanRange;
import io.evercam.network.discovery.ScanResult;
import io.evercam.network.discovery.UpnpDevice;

public class ScanForCameraTask extends AsyncTask<Void, DiscoveredCamera, ArrayList<DiscoveredCamera>>
{
    private final String TAG = "ScanForCameraTask";

    private WeakReference<ScanActivity> scanActivityReference;
    private NetInfo netInfo;
    private Date startTime;
    public ExecutorService pool;
    public ArrayList<UpnpDevice> upnpDeviceList;
    private boolean upnpDone = false;
    private boolean natDone = false;
    private boolean onvifDone = false;

    //Check if single IP scan and port scan is completed or not by comparing the start and end count
    private int singleIpStartedCount = 0;
    private int singleIpEndedCount = 0;

    private String externalIp = "";
    private float scanPercentage = 0;
    private int totalDevices = 255;
    //ONVIF,SSDP and NAT discovery take 9 percents each. The rest is allocated to IP scan
    private final int PER__DISCOVERY_METHOD_PERCENT = 9;

    public ScanForCameraTask(ScanActivity scanActivity)
    {
        this.scanActivityReference = new WeakReference<>(scanActivity);
        netInfo = new NetInfo(scanActivity);
        pool = Executors.newFixedThreadPool(EvercamDiscover.DEFAULT_FIXED_POOL);
        upnpDeviceList = new ArrayList<>();
    }

    @Override
    protected void onPreExecute()
    {
        getScanActivity().onScanningStarted();
    }

    @Override
    protected ArrayList<DiscoveredCamera> doInBackground(Void... params)
    {
        startTime = new Date();
        try
        {
            final ScanRange scanRange = new ScanRange(netInfo.getGatewayIp(), netInfo.getNetmaskIp());
            totalDevices = scanRange.size();

            externalIp = NetworkInfo.getExternalIP();

            if(!pool.isShutdown() && ! isCancelled())
            {
                pool.execute(onvifRunnable);
                pool.execute(upnpRunnable);
                pool.execute(new NatRunnable(netInfo.getGatewayIp())
                {
                    @Override
                    public void onFinished(ArrayList<NatMapEntry> mapEntries)
                    {
                        if(getScanActivity() != null)
                        {
                            for(DiscoveredCamera discoveredCamera : getScanActivity().discoveredCameras)

                            {
                                DiscoveredCamera mergedCamera = EvercamDiscover.mergeNatTableToCamera(discoveredCamera, mapEntries);
                                publishProgress(mergedCamera);
                            }
                        }

                        natDone = true;
                        scanPercentage += PER__DISCOVERY_METHOD_PERCENT;
                        updatePercentageOnActivity(scanPercentage);
                    }
                });
            }

            IpScan ipScan = new IpScan(new ScanResult(){
                @Override
                public void onActiveIp(String ip)
                {
                    if(!pool.isShutdown() && !isCancelled())
                    {
                        pool.execute(new IdentifyCameraRunnable(ip) {
                            @Override
                            public void onCameraFound(DiscoveredCamera camera, Vendor
                                    vendor)
                            {
                                camera.setExternalIp(externalIp);

                                //Iterate UPnP device list and publish the UPnP details if matches
                                EvercamDiscover.mergeUpnpDevicesToCamera(camera, upnpDeviceList);

                                publishProgress(camera);
                            }

                            @Override
                            public void onFinished()
                            {
                                singleIpEndedCount ++;
                            }
                        });
                        singleIpStartedCount++;
                    }
                }

                @Override
                public void onIpScanned(String ip)
                {
                    scanPercentage += getPerDevicePercent();

                    updatePercentageOnActivity(scanPercentage);
                }
            });
            ipScan.scanAll(scanRange);
        }
        catch(Exception e)
        {
            Log.e(TAG, e.getLocalizedMessage());
            e.printStackTrace();
        }

        int loopCount = 0;
        while(!onvifDone && !upnpDone || ! natDone || singleIpStartedCount != singleIpEndedCount)
        {
            loopCount++;

            if(loopCount > 20) break; //Wait for maximum 10 secs

            if(isCancelled()) break;

            try
            {
                Thread.sleep(500);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        if(getScanActivity() != null)
        {
            return getScanActivity().discoveredCameras;
        }
        return new ArrayList<>();
    }

    @Override
    protected void onProgressUpdate(DiscoveredCamera... discoveredCameras)
    {
        if(getScanActivity() != null)
        {
            getScanActivity().addNewCameraToResultList(discoveredCameras[0]);
        }
    }

    @Override
    protected void onPostExecute(ArrayList<DiscoveredCamera> cameraList)
    {
        if(getScanActivity() != null)
        {
            getScanActivity().showScanResults(cameraList);
            getScanActivity().onScanningFinished(cameraList);
        }

        pool.shutdown();

        Float scanningTime = Commons.calculateTimeDifferenceFrom(startTime);
        Log.d(TAG, "Scanning time: " + scanningTime);

        String username = "";
        if(AppData.defaultUser != null)
        {
            username = AppData.defaultUser.getUsername();
        }
        new ScanFeedbackItem(getScanActivity(), username, scanningTime, cameraList).sendToKeenIo();
    }
    
    private float getPerDevicePercent()
    {
        return (float)(100 - PER__DISCOVERY_METHOD_PERCENT*3)/totalDevices;
    }
    
    private ScanActivity getScanActivity()
    {
        return scanActivityReference.get();
    }

    private void updatePercentageOnActivity(Float percentage)
    {
        if(getScanActivity() != null)
        {
            if(percentage != null)
            {
                if(!isCancelled() && getStatus() != Status.FINISHED)
                {
                    getScanActivity().updateScanPercentage(percentage);
                }
            }
            else
            {
                getScanActivity().updateScanPercentage(percentage);
            }
        }
    }

    private OnvifRunnable onvifRunnable = new OnvifRunnable()
    {
        @Override
        public void onFinished()
        {
            scanPercentage += PER__DISCOVERY_METHOD_PERCENT;
            updatePercentageOnActivity(scanPercentage);
            onvifDone = true;
        }

        @Override
        public void onDeviceFound(DiscoveredCamera discoveredCamera)
        {
            discoveredCamera.setExternalIp(externalIp);
            publishProgress(discoveredCamera);
        }
    };

    private UpnpRunnable upnpRunnable = new UpnpRunnable(){
        @Override
        public void onDeviceFound(UpnpDevice upnpDevice)
        {
            Log.d(TAG, "UPnP device found: " + upnpDevice.toString());
            upnpDeviceList.add(upnpDevice);
            // If IP address matches
            String ipFromUpnp = upnpDevice.getIp();
            if (ipFromUpnp != null && !ipFromUpnp.isEmpty())
            {
                for(DiscoveredCamera discoveredCamera : getScanActivity().discoveredCameras)
                {
                    if(discoveredCamera.getIP().equals(upnpDevice.getIp()))
                    {
                        DiscoveredCamera publishCamera = new DiscoveredCamera(discoveredCamera.getIP());
                        EvercamDiscover.mergeSingleUpnpDeviceToCamera(upnpDevice, publishCamera);
                        publishProgress(publishCamera);
                        break;
                    }
                }
            }
        }

        @Override
        public void onFinished(ArrayList<UpnpDevice> arrayList)
        {
            upnpDone = true;
            scanPercentage += PER__DISCOVERY_METHOD_PERCENT;
            updatePercentageOnActivity(scanPercentage);
        }
    };
}
