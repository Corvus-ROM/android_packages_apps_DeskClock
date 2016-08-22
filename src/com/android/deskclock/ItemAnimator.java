/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemAnimator extends SimpleItemAnimator {

    private final List<Animator> mAddAnimatorsList = new ArrayList<>();
    private final List<Animator> mRemoveAnimatorsList = new ArrayList<>();
    private final List<Animator> mChangeAnimatorsList = new ArrayList<>();
    private final List<Animator> mMoveAnimatorsList = new ArrayList<>();

    private final Map<ViewHolder, Animator> mAnimators = new ArrayMap<>();

    @Override
    public boolean animateRemove(final ViewHolder holder) {
        endAnimation(holder);

        final float prevAlpha = holder.itemView.getAlpha();

        final Animator removeAnimator = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 0f);
        removeAnimator.setDuration(getRemoveDuration());
        removeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchRemoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                holder.itemView.setAlpha(prevAlpha);
                dispatchRemoveFinished(holder);
            }
        });
        mRemoveAnimatorsList.add(removeAnimator);
        mAnimators.put(holder, removeAnimator);
        return true;
    }

    @Override
    public boolean animateAdd(final ViewHolder holder) {
        endAnimation(holder);

        final float prevAlpha = holder.itemView.getAlpha();
        holder.itemView.setAlpha(0f);

        final Animator addAnimator = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 1f)
                .setDuration(getAddDuration());
        addAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchAddStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                holder.itemView.setAlpha(prevAlpha);
                dispatchAddFinished(holder);
            }
        });
        mAddAnimatorsList.add(addAnimator);
        mAnimators.put(holder, addAnimator);
        return true;
    }

    @Override
    public boolean animateMove(final ViewHolder holder, int fromX, int fromY,
            int toX, int toY) {
        endAnimation(holder);

        final View view = holder.itemView;
        final int deltaX = toX - fromX;
        final int deltaY = toY - fromY;

        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }

        final float prevTranslationX = view.getTranslationX();
        final float prevTranslationY = view.getTranslationY();
        view.setTranslationX(-deltaX);
        view.setTranslationY(-deltaY);

        final long moveDuration = getMoveDuration();

        final ObjectAnimator moveAnimator;
        if (deltaX != 0 && deltaY != 0) {
            final PropertyValuesHolder moveX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f);
            final PropertyValuesHolder moveY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveX, moveY);
        } else if (deltaX != 0) {
            final PropertyValuesHolder moveX = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveX);
        } else {
            final PropertyValuesHolder moveY = PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f);
            moveAnimator = ObjectAnimator.ofPropertyValuesHolder(holder.itemView, moveY);
        }

        moveAnimator.setDuration(moveDuration);
        moveAnimator.setInterpolator(AnimatorUtils.INTERPOLATOR_FAST_OUT_SLOW_IN);
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchMoveStarting(holder);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(holder);
                view.setTranslationX(prevTranslationX);
                view.setTranslationY(prevTranslationY);
                dispatchMoveFinished(holder);
            }
        });
        mMoveAnimatorsList.add(moveAnimator);
        mAnimators.put(holder, moveAnimator);

        return true;
    }

    @Override
    public boolean animateChange(final ViewHolder oldHolder, final ViewHolder newHolder, int fromX,
            int fromY, int toX, int toY) {
        endAnimation(oldHolder);
        endAnimation(newHolder);

        if (oldHolder == newHolder) {
            // The same view holder is being changed, i.e. it is changing position but not type.
            return animateMove(oldHolder, fromX, fromY, toX, toY);
        } else if (oldHolder == null || newHolder == null) {
            // Two holders are required for change animations.
            return true;
        } else if (!(oldHolder instanceof OnAnimateChangeListener) ||
                !(newHolder instanceof OnAnimateChangeListener)) {
            // Both holders must implement OnAnimateChangeListener in order to animate.
            return true;
        }

        final long changeDuration = getChangeDuration();

        final Animator oldChangeAnimator = ((OnAnimateChangeListener) oldHolder)
                .onAnimateChange(oldHolder, newHolder, changeDuration);
        oldChangeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchChangeStarting(oldHolder, true);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(oldHolder);
                dispatchChangeFinished(oldHolder, true);
            }
        });
        mAnimators.put(oldHolder, oldChangeAnimator);
        mChangeAnimatorsList.add(oldChangeAnimator);

        final Animator newChangeAnimator = ((OnAnimateChangeListener) newHolder)
                .onAnimateChange(oldHolder, newHolder, changeDuration);
        newChangeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchChangeStarting(newHolder, false);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                mAnimators.remove(newHolder);
                dispatchChangeFinished(newHolder, false);
            }
        });
        mAnimators.put(newHolder, newChangeAnimator);
        mChangeAnimatorsList.add(newChangeAnimator);

        return true;
    }

    @Override
    public void runPendingAnimations() {
        final AnimatorSet removeAnimatorSet = new AnimatorSet();
        removeAnimatorSet.playTogether(mRemoveAnimatorsList);
        mRemoveAnimatorsList.clear();

        final AnimatorSet addAnimatorSet = new AnimatorSet();
        addAnimatorSet.playTogether(mAddAnimatorsList);
        mAddAnimatorsList.clear();

        final AnimatorSet changeAnimatorSet = new AnimatorSet();
        changeAnimatorSet.playTogether(mChangeAnimatorsList);
        mChangeAnimatorsList.clear();

        final AnimatorSet moveAnimatorSet = new AnimatorSet();
        moveAnimatorSet.playTogether(mMoveAnimatorsList);
        mMoveAnimatorsList.clear();

        final AnimatorSet pendingAnimatorSet = new AnimatorSet();
        pendingAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeAllListeners();
                dispatchFinishedWhenDone();
            }
        });
        // Required order: removes, then changes & moves simultaneously, then additions.
        pendingAnimatorSet.play(changeAnimatorSet)
                .with(moveAnimatorSet)
                .before(addAnimatorSet)
                .after(removeAnimatorSet);
        pendingAnimatorSet.start();
    }

    @Override
    public void endAnimation(ViewHolder holder) {
        final Animator animator = mAnimators.get(holder);

        mAnimators.remove(holder);
        mAddAnimatorsList.remove(animator);
        mRemoveAnimatorsList.remove(animator);
        mChangeAnimatorsList.remove(animator);
        mMoveAnimatorsList.remove(animator);

        if (animator != null) {
            animator.end();
        }

        dispatchFinishedWhenDone();
    }

    @Override
    public void endAnimations() {
        final List<Animator> animatorList = new ArrayList<>(mAnimators.values());
        for (Animator animator : animatorList) {
            animator.end();
        }
        dispatchFinishedWhenDone();
    }

    @Override
    public boolean isRunning() {
        return !mAnimators.isEmpty();
    }

    private void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    public interface OnAnimateChangeListener {
        Animator onAnimateChange(ViewHolder oldHolder, ViewHolder newHolder, long duration);
    }
}