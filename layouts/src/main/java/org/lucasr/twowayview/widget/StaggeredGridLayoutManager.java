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
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import org.lucasr.twowayview.widget.Spans.LaneInfo;

public class StaggeredGridLayoutManager extends GridLayoutManager {
    private static final String LOGTAG = "StaggeredGridLayoutManager";

    protected static class StaggeredItemEntry extends BaseLayoutManager.ItemEntry {
        private final int span;
        private int width;
        private int height;

        public StaggeredItemEntry(int startLane, int anchorLane, int span) {
            super(startLane, anchorLane);
            this.span = span;
        }

        public StaggeredItemEntry(Parcel in) {
            super(in);
            this.span = in.readInt();
            this.width = in.readInt();
            this.height = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(span);
            out.writeInt(width);
            out.writeInt(height);
        }

        public static final Parcelable.Creator<StaggeredItemEntry> CREATOR
                = new Parcelable.Creator<StaggeredItemEntry>() {
            @Override
            public StaggeredItemEntry createFromParcel(Parcel in) {
                return new StaggeredItemEntry(in);
            }

            @Override
            public StaggeredItemEntry[] newArray(int size) {
                return new StaggeredItemEntry[size];
            }
        };
    }

    public StaggeredGridLayoutManager(Context context, int orientation) {
        super(context, orientation);
    }

    public StaggeredGridLayoutManager(Context context, int orientation, float aspectRatio) {
        super(context, orientation, aspectRatio);
    }

    public StaggeredGridLayoutManager(Context context, int orientation, int numColumns, int numRows) {
        super(context, orientation, numColumns, numRows);
    }

    public StaggeredGridLayoutManager(Context context, int orientation, float aspectRatio, int numColumns, int numRows) {
        super(context, orientation, aspectRatio, numColumns, numRows);
    }

    public StaggeredGridLayoutManager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    int getLaneSpanForChild(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return lp.span;
    }

    @Override
    int getLaneSpanForPosition(int position) {
        final StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(position);
        if (entry == null) {
            throw new IllegalStateException("Could not find span for position " + position);
        }

        return entry.span;
    }

    @Override
    void getLaneForPosition(LaneInfo outInfo, int position, int direction) {
        final StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(position);
        if (entry != null) {
            outInfo.set(entry.startLane, entry.anchorLane);
            return;
        }

        outInfo.setUndefined();
    }

    @Override
    void getLaneForChild(LaneInfo outInfo, View child, int direction) {
        super.getLaneForChild(outInfo, child, direction);
        if (outInfo.isUndefined()) {
            getLanes().findLane(outInfo, getLaneSpanForChild(child), direction);
        }
    }

    @Override
    void moveLayoutToPosition(int position, int offset, Recycler recycler, State state) {
        final boolean isVertical = isVertical();
        final Spans spans = getLanes();

        spans.resetForOffset(0);

        for (int i = 0; i <= position; i++) {
            StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(i);

            if (entry != null) {
                mTempLaneInfo.set(entry.startLane, entry.anchorLane);

                // The spans might have been invalidated because an added or
                // removed item. See BaseLayoutManager.invalidateItemLanes().
                if (mTempLaneInfo.isUndefined()) {
                    spans.findLane(mTempLaneInfo, getLaneSpanForPosition(i), DIRECTION_END);
                    entry.setLane(mTempLaneInfo);
                }

                spans.getChildFrame(mTempRect, entry.width, entry.height, mTempLaneInfo,
                        DIRECTION_END);
            } else {
                final View child = recycler.getViewForPosition(i);

                // XXX: This might potentially cause stalls in the main
                // thread if the layout ends up having to measure tons of
                // child views. We might need to add different policies based
                // on known assumptions regarding certain layouts e.g. child
                // views have stable aspect ratio, lane size is fixed, etc.
                measureChild(child, DIRECTION_END);

                // The measureChild() call ensures an entry is created for
                // this position.
                entry = (StaggeredItemEntry) getItemEntryForPosition(i);

                mTempLaneInfo.set(entry.startLane, entry.anchorLane);
                spans.getChildFrame(mTempRect, getDecoratedMeasuredWidth(child),
                        getDecoratedMeasuredHeight(child), mTempLaneInfo, DIRECTION_END);

                cacheItemFrame(entry, mTempRect);
            }

            if (i != position) {
                pushChildFrame(entry, mTempRect, entry.startLane, entry.span, DIRECTION_END);
            }
        }

        spans.getLane(mTempLaneInfo.startLane, mTempRect);
        spans.resetForDirection(DIRECTION_END);
        spans.offset(offset - (isVertical ? mTempRect.bottom : mTempRect.right));
    }

    @Override
    ItemEntry cacheChildLaneAndSpan(View child, int direction) {
        final int position = getPosition(child);

        mTempLaneInfo.setUndefined();

        StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(position);
        if (entry != null) {
            mTempLaneInfo.set(entry.startLane, entry.anchorLane);
        }

        if (mTempLaneInfo.isUndefined()) {
            getLaneForChild(mTempLaneInfo, child, direction);
        }

        if (entry == null) {
            entry = new StaggeredItemEntry(mTempLaneInfo.startLane, mTempLaneInfo.anchorLane,
                    getLaneSpanForChild(child));
            setItemEntryForPosition(position, entry);
        } else {
            entry.setLane(mTempLaneInfo);
        }

        return entry;
    }

    void cacheItemFrame(StaggeredItemEntry entry, Rect childFrame) {
        entry.width = childFrame.right - childFrame.left;
        entry.height = childFrame.bottom - childFrame.top;
    }

    @Override
    ItemEntry cacheChildFrame(View child, Rect childFrame) {
        StaggeredItemEntry entry = (StaggeredItemEntry) getItemEntryForPosition(getPosition(child));
        if (entry == null) {
            throw new IllegalStateException("Tried to cache frame on undefined item");
        }

        cacheItemFrame(entry, childFrame);
        return entry;
    }

    @Override
    public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
        boolean result = super.checkLayoutParams(lp);
        if (lp instanceof LayoutParams) {
            final LayoutParams staggeredLp = (LayoutParams) lp;
            result &= (staggeredLp.span >= 1 && staggeredLp.span <= getLaneCount());
        }

        return result;
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        if (isVertical()) {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        } else {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        }
    }

    @Override
    public LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        final LayoutParams staggeredLp = new LayoutParams((ViewGroup.MarginLayoutParams) lp);
        if (isVertical()) {
            staggeredLp.width = LayoutParams.MATCH_PARENT;
            staggeredLp.height = lp.height;
        } else {
            staggeredLp.width = lp.width;
            staggeredLp.height = LayoutParams.MATCH_PARENT;
        }

        if (lp instanceof LayoutParams) {
            final LayoutParams other = (LayoutParams) lp;
            staggeredLp.span = Math.max(1, Math.min(other.span, getLaneCount()));
        }

        return staggeredLp;
    }

    @Override
    public LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
        return new LayoutParams(c, attrs);
    }

    public static class LayoutParams extends TwoWayView.LayoutParams {
        private static final int DEFAULT_SPAN = 1;

        public int span;

        public LayoutParams(int width, int height) {
            super(width, height);
            span = DEFAULT_SPAN;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.twowayview_StaggeredGridViewChild);
            span = Math.max(DEFAULT_SPAN, a.getInt(R.styleable.twowayview_StaggeredGridViewChild_twowayview_span, -1));
            a.recycle();
        }

        public LayoutParams(ViewGroup.LayoutParams other) {
            super(other);
            init(other);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams other) {
            super(other);
            init(other);
        }

        private void init(ViewGroup.LayoutParams other) {
            if (other instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) other;
                span = lp.span;
            } else {
                span = DEFAULT_SPAN;
            }
        }
    }
}