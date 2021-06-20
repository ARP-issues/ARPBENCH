/*
 * Kontalk Android client
 * Copyright (C) 2017 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.position;


/**
 * Download Google Static Map
 *
 * @author Daniele Ricci
 * @author Andrea Cappelli
 */
public class GMStaticUrlBuilder {
    private static final String URL = "http://maps.googleapis.com/maps/api/staticmap";

    private String mCenter;
    private int mZoom = 15;
    private String mSize = "600x300";
    private String mMarker;
    private int mScale = 1;

    public GMStaticUrlBuilder setCenter(double lat, double lon) {
        mCenter = lat + "," + lon;
        return this;
    }

    public GMStaticUrlBuilder setZoom(Integer zoom) {
        if (zoom != null)
            mZoom = zoom;
        return this;
    }

    public GMStaticUrlBuilder setSize(int width, int height) {
        mSize = width + "x" + height;
        return this;
    }

    public GMStaticUrlBuilder setMarker(double lat, double lon) {
        StringBuilder marker = new StringBuilder();

        marker.append("%7C")
            .append(lat)
            .append(',')
            .append(lon);

        mMarker = marker.toString();

        return this;
    }

    public GMStaticUrlBuilder setScale(Integer scale) {
        if (scale != null)
            mScale = scale;

        return this;
    }

    public String toString() {
        return new StringBuilder()
            .append(URL)
            .append("?center=")
            .append(mCenter)
            .append("&zoom=")
            .append(mZoom)
            .append("&size=")
            .append(mSize)
            .append("&scale=")
            .append(mScale)
            .append("&markers=")
            .append(mMarker)
            .toString();
    }
}
