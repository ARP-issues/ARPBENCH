/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp.location;

import android.location.Location;

import org.microg.nlp.Provider;

interface LocationProvider extends Provider {
    int FASTEST_REFRESH_INTERVAL = 2500; // in milliseconds

    void onEnable();

    void onDisable();

    void reportLocation(Location location);

    void forceLocation(Location location);
}
