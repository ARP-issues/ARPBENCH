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

package org.kontalk;

import org.junit.Before;

import org.kontalk.util.Preferences;


/**
 * Sets the test server address and some other stuff in {@link #setUp()}.
 */
public abstract class TestServerTest {
    public static final String TEST_SERVER_URI = "prime.kontalk.net|zeta.kontalk.net:7222";

    @Before
    public void setUp() {
        Preferences.setServerURI(TEST_SERVER_URI);
        Preferences.setAcceptAnyCertificate(true);
    }

}
