package fr.free.nrw.commons.caching;

import android.util.Log;

import com.github.varunpant.quadtree.Point;
import com.github.varunpant.quadtree.QuadTree;

import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.upload.MwVolleyApi;

public class CacheController {

    private double x, y;
    private QuadTree quadTree;
    private Point[] pointsFound;
    private double xMinus, xPlus, yMinus, yPlus;

    private static final String TAG = CacheController.class.getName();
    private static final int EARTH_RADIUS = 6378137;

    public CacheController() {
        quadTree = new QuadTree(-180, -90, +180, +90);
    }

    public void setQtPoint(double decLongitude, double decLatitude) {
        x = decLongitude;
        y = decLatitude;
        Log.d(TAG, "New QuadTree created");
        Log.d(TAG, "X (longitude) value: " + x + ", Y (latitude) value: " + y);
    }

    public void cacheCategory() {
        List<String> pointCatList = new ArrayList<String>();
        if (MwVolleyApi.GpsCatExists.getGpsCatExists() == true) {
             pointCatList.addAll(MwVolleyApi.getGpsCat());
            Log.d(TAG, "Categories being cached: " + pointCatList);
        } else {
            Log.d(TAG, "No categories found, so no categories cached");
        }
        quadTree.set(x, y, pointCatList);
    }

    public List findCategory() {
        //Convert decLatitude and decLongitude to a coordinate offset range
        convertCoordRange();
        pointsFound = quadTree.searchWithin(xMinus, yMinus, xPlus, yPlus);
        List<String> displayCatList = new ArrayList<String>();
        Log.d(TAG, "Points found in quadtree: " + pointsFound);

        if (pointsFound.length != 0) {
            Log.d(TAG, "Entering for loop");

            for (Point point : pointsFound) {
                Log.d(TAG, "Nearby point: " + point.toString());
                displayCatList = (List<String>)point.getValue();
                Log.d(TAG, "Nearby cat: " + point.getValue());
            }

            Log.d(TAG, "Categories found in cache: " + displayCatList.toString());
        } else {
            Log.d(TAG, "No categories found in cache");
        }
        return displayCatList;
    }

    //Based on algorithm at http://gis.stackexchange.com/questions/2951/algorithm-for-offsetting-a-latitude-longitude-by-some-amount-of-meters
    public void convertCoordRange() {
        //Position, decimal degrees
        double lat = y;
        double lon = x;

        //offsets in meters
        double offset = 100;

        //Coordinate offsets in radians
        double dLat = offset/EARTH_RADIUS;
        double dLon = offset/(EARTH_RADIUS*Math.cos(Math.PI*lat/180));

        //OffsetPosition, decimal degrees
        yPlus = lat + dLat * 180/Math.PI;
        yMinus = lat - dLat * 180/Math.PI;
        xPlus = lon + dLon * 180/Math.PI;
        xMinus = lon - dLon * 180/Math.PI;
        Log.d(TAG, "Search within: xMinus=" + xMinus + ", yMinus=" + yMinus + ", xPlus=" + xPlus + ", yPlus=" + yPlus);
    }
}
