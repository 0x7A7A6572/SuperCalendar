package com.ldf.calendar.behavior;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.ldf.calendar.Utils;
import com.ldf.calendar.view.LinkageViewPager;
import com.ldf.calendar.view.MonthPager;

public class ViewPagerBehavior extends CoordinatorLayout.Behavior<LinkageViewPager> {
    private int initOffset = -1;
    private int olderChildTop = -1;
    private int minOffset = -1;
    private Context context;
    private boolean initiated = false;
    boolean hidingTop = false;
    boolean showingTop = false;

    public ViewPagerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, LinkageViewPager child, View dependency) {
        return dependency instanceof LinkageViewPager;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, LinkageViewPager child, int layoutDirection) {
        parent.onLayoutChild(child, layoutDirection);
        MonthPager monthPager = getMonthPager(parent);
        initMinOffsetAndInitOffset(parent, child, monthPager);
        return true;
    }

    private void initMinOffsetAndInitOffset(CoordinatorLayout parent,
                                            ViewPager child,
                                            MonthPager monthPager) {
        if (monthPager.getBottom() > 0 && initOffset == -1) {
            initOffset = monthPager.getViewHeight();
            saveTop(initOffset);
        }
        if (!initiated) {
            initOffset = monthPager.getViewHeight();
            saveTop(initOffset);
            initiated = true;
        }
        child.offsetTopAndBottom(Utils.loadTop());
        minOffset = getMonthPager(parent).getCellHeight();
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, LinkageViewPager child,
                                       View directTargetChild, View target, int nestedScrollAxes) {
        Log.v("ldf", "onStartNestedScroll");

        MonthPager monthPager = (MonthPager) coordinatorLayout.getChildAt(0);
        monthPager.setScrollable(false);
        boolean isVertical = (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;

        return isVertical;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, LinkageViewPager child,
                                  View target, int dx, int dy, int[] consumed) {
        Log.v("ldf", "onNestedPreScroll");
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        child.setVerticalScrollBarEnabled(true);

        MonthPager monthPager = (MonthPager) coordinatorLayout.getChildAt(0);
        if (monthPager.getPageScrollState() != ViewPager.SCROLL_STATE_IDLE) {
            consumed[1] = dy;
            Log.w("ldf", "onNestedPreScroll: MonthPager dragging");
            Toast.makeText(context, "loading month data", Toast.LENGTH_SHORT).show();
            return;
        }

        // 上滑，正在隐藏顶部的日历
        hidingTop = dy > 0 && child.getTop() <= initOffset
                && child.getTop() > getMonthPager(coordinatorLayout).getCellHeight();
        // 下滑，正在展示顶部的日历
        showingTop = dy < 0 && !ViewCompat.canScrollVertically(target, -1);

        if (hidingTop || showingTop) {
            consumed[1] = Utils.scroll(child, dy,
                    getMonthPager(coordinatorLayout).getCellHeight(),
                    getMonthPager(coordinatorLayout).getViewHeight());
            saveTop(child.getTop());
        }
        //z* 利用偏移量判断日历的展开与收缩状态
        if (child.getTop() == initOffset && child.getTop() != olderChildTop) {
            Log.e("ldf", "展开状态");
            child.onCalendarStateChange(true);
        } else if (child.getTop() == getMonthPager(coordinatorLayout).getCellHeight() && child.getTop() != olderChildTop) {
            Log.e("ldf", "收缩状态");
            child.onCalendarStateChange(false);
        }
        olderChildTop = child.getTop();

        //  Log.v("ldf","偏移量：" + initOffset + " child.getTop: " + child.getTop() + "month height:" + getMonthPager(coordinatorLayout).getCellHeight());
    }


    @Override
    public void onStopNestedScroll(final CoordinatorLayout parent, final LinkageViewPager child, View target) {
        Log.v("ldf", "onStopNestedScroll");
        super.onStopNestedScroll(parent, child, target);
        MonthPager monthPager = (MonthPager) parent.getChildAt(0);
        monthPager.setScrollable(true);
        if (!Utils.isScrollToBottom()) {
            if (initOffset - Utils.loadTop() > Utils.getTouchSlop(context) && hidingTop) {
                Utils.scrollTo(parent, child, getMonthPager(parent).getCellHeight(), 500);
            } else {
                Utils.scrollTo(parent, child, getMonthPager(parent).getViewHeight(), 150);
            }
        } else {
            if (Utils.loadTop() - minOffset > Utils.getTouchSlop(context) && showingTop) {
                Utils.scrollTo(parent, child, getMonthPager(parent).getViewHeight(), 500);
            } else {
                Utils.scrollTo(parent, child, getMonthPager(parent).getCellHeight(), 150);
            }
        }
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, LinkageViewPager child, View target, float velocityX, float velocityY, boolean consumed) {
        Log.d("ldf", "onNestedFling: velocityY: " + velocityY);
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, LinkageViewPager child, View target, float velocityX, float velocityY) {
        // 日历隐藏和展示过程，不允许RecyclerView进行fling
        if (hidingTop || showingTop) {
            return true;
        } else {
            return false;
        }
    }

    private MonthPager getMonthPager(CoordinatorLayout coordinatorLayout) {
        return (MonthPager) coordinatorLayout.getChildAt(0);
    }

    private void saveTop(int top) {
        Utils.saveTop(top);
        if (Utils.loadTop() == initOffset) {
            Utils.setScrollToBottom(false);
        } else if (Utils.loadTop() == minOffset) {
            Utils.setScrollToBottom(true);
        }
    }

}
