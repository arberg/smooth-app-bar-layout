/*
 * Copyright 2015 "Henry Tao <hi@henrytao.me>"
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

package me.henrytao.smoothappbarlayoutdemo.apdater;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import me.henrytao.smoothappbarlayoutdemo.R;

/**
 * Created by henrytao on 9/27/15.
 */
public class SimpleAdapter<T> extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {

  private static final boolean ENABLE_ANIMATION = true;

  private static final boolean ENABLE_CACHED_STATE = true;
  // If true, the adapter data knows whether views are extended or not. If false we just take current state of view in viewholder (so reused views are like in 'wrong' state)

  static class DecoratedData<T> {

    final T data;

    boolean extended;

    public DecoratedData(final T data) {
      this.data = data;
    }

    @Override
    public String toString() {
      return data.toString();
    }
  }

  private final List<DecoratedData<T>> mDataList;

  private final OnItemClickListener mOnItemClickListener;

  public SimpleAdapter(List<T> data, OnItemClickListener<T> onItemClickListener) {
    mDataList = new ArrayList<>(data.size());
    for (int i = 0; i < data.size(); i++) {
      mDataList.add(new DecoratedData<>(data.get(i)));
    }
    mOnItemClickListener = onItemClickListener;
  }

  @Override
  public int getItemCount() {
    return mDataList != null ? mDataList.size() : 0;
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext()), parent, mOnItemClickListener);
  }

  public DecoratedData<T> getItem(int position) {
    return mDataList != null && position >= 0 && position < mDataList.size() ? mDataList.get(position) : null;
  }

  public interface OnItemClickListener<I> {

    void onItemClick(I data);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    private static View createView(LayoutInflater inflater, ViewGroup parent, @LayoutRes int layoutId) {
      return inflater.inflate(layoutId, parent, false);
    }

    @Bind(R.id.title)
    TextView vTitle;

    @Bind(R.id.huge_sub_text)
    TextView vHuge;

    private DecoratedData mData;

    public ViewHolder(LayoutInflater inflater, ViewGroup parent, final OnItemClickListener onItemClickListener) {
      super(createView(inflater, parent, R.layout.item_simple));
      ButterKnife.bind(this, itemView);
      itemView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (onItemClickListener != null) {
            onItemClickListener.onItemClick(mData.data);
          }
          // reproduce bug
          // open 1
          // scroll down find open, close it
          // scrolling up will now be wrong. Of cause problem that it remembers bad view state below, but seems likely to be cause of problem
          mData.extended = ENABLE_CACHED_STATE ? !mData.extended : vHuge.getVisibility() != View.VISIBLE;
          if (ENABLE_ANIMATION) {
            vHuge.setVisibility(View.VISIBLE);
            if (mData.extended) {
              animateView(0, 3000, vHuge, false);
            } else {
              animateView(3000, 0, vHuge, true);
            }
          } else { // skip animation low api's.
            vHuge.setVisibility(mData.extended ? View.VISIBLE : View.GONE);
            vHuge.requestLayout();
          }
          System.out.println("clicked " + mData);
        }
      });
    }

    private void animateView(float startHeight, float endHeight, final View animatedView, final boolean hideAfter) {
      ValueAnimator mover = ValueAnimator.ofFloat(startHeight, endHeight);
      mover.setDuration(400);
      mover.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public void onAnimationUpdate(final ValueAnimator animation) {
          float value = ((Float) animation.getAnimatedValue()).floatValue();
          animatedView.getLayoutParams().height = (int) value;
          animatedView.requestLayout();
        }
      });
      mover.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(final Animator animation) {
          if (hideAfter) {
            vHuge.setVisibility(View.GONE);
          }
          animatedView.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
          animatedView.requestLayout();
        }
      });

      mover.start();
    }

    public void bind(DecoratedData data) {
      mData = data;
      vTitle.setText(data.toString());
      String hugeText = "";
      for (int i = 0; i < 60; i++) {
        hugeText += data.toString() + " HUGE\n";
      }
      vHuge.setText(hugeText);
      if (ENABLE_CACHED_STATE) {
        vHuge.setVisibility(mData.extended ? View.VISIBLE : View.GONE);
        itemView.requestLayout();
      }
    }
  }
}
