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

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;

import org.lucasr.twowayview.TwoWayLayoutManager;

class Spans {
    public static final int NO_LANE = -1;

    private final BaseLayoutManager mLayout;
    private final boolean mIsVertical;
    private final Rect[] mLanes;
    private final Rect[] mSavedLanes;
    private final int mLaneSizeH;
    private final int mLaneSizeV;

    private final Rect mTempRect = new Rect();
    private final LaneInfo mTempLaneInfo = new LaneInfo();

    private Integer mInnerStart;
    private Integer mInnerEnd;

    public static class LaneInfo {
        public int startLane;
        public int anchorLane;

        public boolean isUndefined() {
            return (startLane == NO_LANE || anchorLane == NO_LANE);
        }

        public void set(int startLane, int anchorLane) {
            this.startLane = startLane;
            this.anchorLane = anchorLane;
        }

        public void setUndefined() {
            startLane = NO_LANE;
            anchorLane = NO_LANE;
        }
    }

    public Spans(BaseLayoutManager layout, int orientation, Rect[] lanes, int laneSizeH, int laneSizeV) {
        mLayout = layout;
        mIsVertical = (orientation == RecyclerView.VERTICAL);
        mLanes = lanes;
        mLaneSizeH = laneSizeH;
        mLaneSizeV = laneSizeV;

        mSavedLanes = new Rect[mLanes.length];
        for (int i = 0; i < mLanes.length; i++) {
            mSavedLanes[i] = new Rect();
        }
    }

    public Spans(BaseLayoutManager layout, int laneCount) {
        mLayout = layout;
        mIsVertical = layout.isVertical();

        mLanes = new Rect[laneCount];
        mSavedLanes = new Rect[laneCount];
        for (int i = 0; i < laneCount; i++) {
            mLanes[i] = new Rect();
            mSavedLanes[i] = new Rect();
        }

        mLaneSizeH = calculateLaneSizeH(layout, laneCount);
        mLaneSizeV = calculateLaneSizeV(layout, laneCount);

        final int paddingLeft = layout.getPaddingLeft();
        final int paddingTop = layout.getPaddingTop();

        for (int i = 0; i < laneCount; i++) {
            final int laneStartH = i * mLaneSizeH;
            final int laneStartV = i * mLaneSizeV;

            final int l = paddingLeft + (mIsVertical ? laneStartH : 0);
            final int t = paddingTop + (mIsVertical ? 0 : laneStartV);
            final int r = (mIsVertical ? l + mLaneSizeH : l);
            final int b = (mIsVertical ? t : t + mLaneSizeV);

            mLanes[i].set(l, t, r, b);
        }
    }

    public static int calculateLaneSizeH(BaseLayoutManager layout, int laneCount) {
        final int paddingLeft = layout.getPaddingLeft();
        final int paddingRight = layout.getPaddingRight();
        final int width = layout.getWidth() - paddingLeft - paddingRight;
        final int paddingTop = layout.getPaddingTop();
        final int paddingBottom = layout.getPaddingBottom();
        final int height = layout.getHeight() - paddingTop - paddingBottom;

        if (layout.isVertical())
            return width / laneCount;
        else
            return (int) (height / layout.getAspectRatio() / laneCount);
    }

    public static int calculateLaneSizeV(BaseLayoutManager layout, int laneCount) {
        final int paddingLeft = layout.getPaddingLeft();
        final int paddingRight = layout.getPaddingRight();
        final int width = layout.getWidth() - paddingLeft - paddingRight;
        final int paddingTop = layout.getPaddingTop();
        final int paddingBottom = layout.getPaddingBottom();
        final int height = layout.getHeight() - paddingTop - paddingBottom;

        if (layout.isVertical())
            return (int) (width / layout.getAspectRatio() / laneCount);
        else
            return height / laneCount;
    }

    private void invalidateEdges() {
        mInnerStart = null;
        mInnerEnd = null;
    }

    public int getOrientation() {
        return (mIsVertical ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL);
    }

    public void save() {
        for (int i = 0; i < mLanes.length; i++) {
            mSavedLanes[i].set(mLanes[i]);
        }
    }

    public void restore() {
        for (int i = 0; i < mLanes.length; i++) {
            mLanes[i].set(mSavedLanes[i]);
        }
    }

    public int getLaneSizeH() {
        return mLaneSizeH;
    }

    public int getLaneSizeV() {
        return mLaneSizeV;
    }

    public int getCount() {
        return mLanes.length;
    }

    private void offsetLane(int lane, int offset) {
        mLanes[lane].offset(mIsVertical ? 0 : offset,
                mIsVertical ? offset : 0);
    }

    public void offset(int offset) {
        for (int i = 0; i < mLanes.length; i++) {
            offset(i, offset);
        }

        invalidateEdges();
    }

    public void offset(int lane, int offset) {
        offsetLane(lane, offset);
        invalidateEdges();
    }

    public void getLane(int lane, Rect laneRect) {
        laneRect.set(mLanes[lane]);
    }

    public int pushChildFrame(Rect outRect, int lane, int margin, int direction) {
        final int delta;

        final Rect laneRect = mLanes[lane];
        if (mIsVertical) {
            if (direction == TwoWayLayoutManager.DIRECTION_END) {
                delta = outRect.top - laneRect.bottom;
                laneRect.bottom = outRect.bottom + margin;
            } else {
                delta = outRect.bottom - laneRect.top;
                laneRect.top = outRect.top - margin;
            }
        } else {
            if (direction == TwoWayLayoutManager.DIRECTION_END) {
                delta = outRect.left - laneRect.right;
                laneRect.right = outRect.right + margin;
            } else {
                delta = outRect.right - laneRect.left;
                laneRect.left = outRect.left - margin;
            }
        }

        invalidateEdges();

        return delta;
    }

    public void popChildFrame(Rect outRect, int lane, int margin, int direction) {
        final Rect laneRect = mLanes[lane];
        if (mIsVertical) {
            if (direction == TwoWayLayoutManager.DIRECTION_END) {
                laneRect.top = outRect.bottom - margin;
            } else {
                laneRect.bottom = outRect.top + margin;
            }
        } else {
            if (direction == TwoWayLayoutManager.DIRECTION_END) {
                laneRect.left = outRect.right - margin;
            } else {
                laneRect.right = outRect.left + margin;
            }
        }

        invalidateEdges();
    }

    public void getChildFrame(Rect outRect, int childWidth, int childHeight, LaneInfo laneInfo,
                              int direction) {
        final Rect startRect = mLanes[laneInfo.startLane];

        // The anchor lane only applies when we're get child frame in the direction
        // of the forward scroll. We'll need to rethink this once we start working on
        // RTL support.
        final int anchorLane =
                (direction == TwoWayLayoutManager.DIRECTION_END ? laneInfo.anchorLane : laneInfo.startLane);
        final Rect anchorRect = mLanes[anchorLane];

        if (mIsVertical) {
            outRect.left = startRect.left;
            outRect.top =
                    (direction == TwoWayLayoutManager.DIRECTION_END ? anchorRect.bottom : anchorRect.top - childHeight);
        } else {
            outRect.top = startRect.top;
            outRect.left =
                    (direction == TwoWayLayoutManager.DIRECTION_END ? anchorRect.right : anchorRect.left - childWidth);
        }

        outRect.right = outRect.left + childWidth;
        outRect.bottom = outRect.top + childHeight;
    }

    private boolean intersects(int start, int count, Rect r) {
        for (int l = start; l < start + count; l++) {
            if (Rect.intersects(mLanes[l], r)) {
                return true;
            }
        }

        return false;
    }

    private int findLaneThatFitsSpan(int anchorLane, int laneSpan, int direction) {
        final int findStart = Math.max(0, anchorLane - laneSpan + 1);
        final int findEnd = Math.min(findStart + laneSpan, mLanes.length - laneSpan + 1);
        for (int l = findStart; l < findEnd; l++) {
            mTempLaneInfo.set(l, anchorLane);

            getChildFrame(mTempRect, mIsVertical ? laneSpan * mLaneSizeH : 1,
                    mIsVertical ? 1 : laneSpan * mLaneSizeV, mTempLaneInfo, direction);

            if (!intersects(l, laneSpan, mTempRect)) {
                return l;
            }
        }

        return Spans.NO_LANE;
    }

    public void findLane(LaneInfo outInfo, int laneSpan, int direction) {
        outInfo.setUndefined();

        int targetEdge = (direction == TwoWayLayoutManager.DIRECTION_END ? Integer.MAX_VALUE : Integer.MIN_VALUE);
        for (int l = 0; l < mLanes.length; l++) {
            final int laneEdge;
            if (mIsVertical) {
                laneEdge = (direction == TwoWayLayoutManager.DIRECTION_END ? mLanes[l].bottom : mLanes[l].top);
            } else {
                laneEdge = (direction == TwoWayLayoutManager.DIRECTION_END ? mLanes[l].right : mLanes[l].left);
            }

            if ((direction == TwoWayLayoutManager.DIRECTION_END && laneEdge < targetEdge) ||
                (direction == TwoWayLayoutManager.DIRECTION_START && laneEdge > targetEdge)) {

                final int targetLane = findLaneThatFitsSpan(l, laneSpan, direction);
                if (targetLane != NO_LANE) {
                    targetEdge = laneEdge;
                    outInfo.set(targetLane, l);
                }
            }
        }
    }

    public void resetForDirection(int direction) {
        for (Rect laneRect : mLanes) {
            if (mIsVertical) {
                if (direction == TwoWayLayoutManager.DIRECTION_START) {
                    laneRect.bottom = laneRect.top;
                } else {
                    laneRect.top = laneRect.bottom;
                }
            } else {
                if (direction == TwoWayLayoutManager.DIRECTION_START) {
                    laneRect.right = laneRect.left;
                } else {
                    laneRect.left = laneRect.right;
                }
            }
        }

        invalidateEdges();
    }

    public void resetForOffset(int offset) {
        for (Rect laneRect : mLanes) {
            laneRect.offsetTo(mIsVertical ? laneRect.left : offset,
                    mIsVertical ? offset : laneRect.top);

            if (mIsVertical) {
                laneRect.bottom = laneRect.top;
            } else {
                laneRect.right = laneRect.left;
            }
        }

        invalidateEdges();
    }

    public int getInnerStart() {
        if (mInnerStart != null) {
            return mInnerStart;
        }

        mInnerStart = Integer.MIN_VALUE;


        for (Rect laneRect : mLanes) {
            mInnerStart = Math.max(mInnerStart, mIsVertical ? laneRect.top : laneRect.left);
        }

        return mInnerStart;
    }

    public int getInnerEnd() {
        if (mInnerEnd != null) {
            return mInnerEnd;
        }

        mInnerEnd = Integer.MAX_VALUE;
        for (Rect laneRect : mLanes) {
            mInnerEnd = Math.min(mInnerEnd, mIsVertical ? laneRect.bottom : laneRect.right);
        }

        return mInnerEnd;
    }
}