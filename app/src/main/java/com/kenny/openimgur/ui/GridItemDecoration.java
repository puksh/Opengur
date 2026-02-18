package com.kenny.openimgur.ui;

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.View;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration {
    private int mSpace;

    private int mNumColumns;

    public GridItemDecoration(int spacing, int numColumns) {
        mSpace = spacing;
        mNumColumns = numColumns;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int column = 0;
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();

        if (layoutManager instanceof StaggeredGridLayoutManager
                && view.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            column = ((StaggeredGridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
        } else if (layoutManager instanceof GridLayoutManager
                && view.getLayoutParams() instanceof GridLayoutManager.LayoutParams) {
            column = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
        }

        outRect.left = mSpace - (column * mSpace / mNumColumns);
        outRect.right = (column + 1) * mSpace / mNumColumns;
        outRect.top = mSpace;
    }
}