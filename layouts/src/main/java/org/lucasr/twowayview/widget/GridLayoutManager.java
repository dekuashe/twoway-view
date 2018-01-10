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
import android.content.res.TypedArray;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.view.View;

import org.lucasr.twowayview.widget.Spans.LaneInfo;

public class GridLayoutManager extends BaseLayoutManager {
    private static final String LOGTAG = "GridLayoutManager";

    private int mNumColumns;
    private int mNumRows;

    public GridLayoutManager(Context context, int orientation) {
        super(context, orientation);
    }

    public GridLayoutManager(Context context, int orientation, float aspectRatio) {
        super(context, orientation, aspectRatio);
    }

    public GridLayoutManager(Context context, int orientation, int mNumColumns, int mNumRows) {
        this(context, orientation, 1.0f, mNumColumns, mNumRows);
    }

    public GridLayoutManager(Context context, int orientation, float aspectRatio, int mNumColumns, int mNumRows) {
        super(context, orientation, aspectRatio);
        this.mNumColumns = mNumColumns;
        this.mNumRows = mNumRows;
    }

    public GridLayoutManager(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.twowayview_GridLayoutManager, 0, 0);

        final int cols = a.getInt(R.styleable.twowayview_GridLayoutManager_twowayview_numColumns, 1);
        final int rows = a.getInt(R.styleable.twowayview_GridLayoutManager_twowayview_numRows, 1);

        a.recycle();

        this.mNumColumns = cols;
        this.mNumRows = rows;
    }

    @Override
    int getLaneCount() {
        return (isVertical() ? mNumColumns : mNumRows);
    }

    @Override
    void getLaneForPosition(LaneInfo outInfo, int position, int direction) {
        final int lane = (position % getLaneCount());
        outInfo.set(lane, lane);
    }

    @Override
    void moveLayoutToPosition(int position, int offset, Recycler recycler, State state) {
        final Spans spans = getLanes();
        spans.resetForOffset(offset);

        getLaneForPosition(mTempLaneInfo, position, DIRECTION_END);
        final int lane = mTempLaneInfo.startLane;
        if (lane == 0) {
            return;
        }

        final View child = recycler.getViewForPosition(position);
        measureChild(child, DIRECTION_END);

        final int dimension =
                (isVertical() ? getDecoratedMeasuredHeight(child) : getDecoratedMeasuredWidth(child));

        for (int i = lane - 1; i >= 0; i--) {
            spans.offset(i, dimension);
        }
    }

    public int getNumColumns() {
        return mNumColumns;
    }

    public void setNumColumns(int numColumns) {
        if (mNumColumns == numColumns) {
            return;
        }

        mNumColumns = numColumns;
        if (isVertical()) {
            requestLayout();
        }
    }

    public int getNumRows() {
        return mNumRows;
    }

    public void setNumRows(int numRows) {
        if (mNumRows == numRows) {
            return;
        }

        mNumRows = numRows;
        if (!isVertical()) {
            requestLayout();
        }
    }
}
