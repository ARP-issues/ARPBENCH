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

import java.util.Locale;


/**
 * OpenStreetMap URL builder.
 * @author Daniele Ricci
 */
public class OsmUrlBuilder {
    private static final String URL = "https://www.openstreetmap.org/#map=12/%1$,.2f/%2$,.2f";

    private OsmUrlBuilder() {
    }

    public static String build(double lat, double lon) {
        return String.format(Locale.US, URL, lat, lon);
    }
}
