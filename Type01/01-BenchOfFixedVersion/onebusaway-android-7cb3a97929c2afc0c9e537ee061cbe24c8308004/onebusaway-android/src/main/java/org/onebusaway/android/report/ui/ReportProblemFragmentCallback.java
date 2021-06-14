/*
 * Copyright (C) 2016 University of South Florida
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.android.report.ui;

public interface ReportProblemFragmentCallback {

    /**
     * Called when the problem report was successfully submitted.
     * Callback mechanism implemented as described in Android best-practices documentation:
     * http://developer.android.com/training/basics/fragments/communicating.html
     */
    void onReportSent();
}
