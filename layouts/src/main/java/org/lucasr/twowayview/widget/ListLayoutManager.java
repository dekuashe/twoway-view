/*
 * Copyright (C) 2014 Lucas Rocha
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

package org.lucasr.twowayview.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;

import org.lucasr.twowayview.widget.Spans.LaneInfo;

public class ListLayoutManager extends BaseLayoutManager {
    private static final String LOGTAG = "ListLayoutManager";

    public ListLayoutManager(Context context, int orientation) {
        super(context, orientation);
    }

    public ListLayoutManager(Context context, int orientation, float aspectRatio) {
        super(context, orientation, aspectRatio);
    }

    public ListLayoutManager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    int getLaneCount() {
        return 1;
    }

    @Override
    void getLaneForPosition(LaneInfo outInfo, int position, int direction) {
        outInfo.set(0, 0);
    }

    @Override
    void moveLayoutToPosition(int position, int offset, Recycler recycler, State state) {
        getLanes().resetForOffset(offset);
    }
}
