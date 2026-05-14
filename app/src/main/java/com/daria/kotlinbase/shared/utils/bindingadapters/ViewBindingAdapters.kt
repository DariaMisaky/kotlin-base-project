package com.daria.kotlinbase.shared.utils.bindingadapters

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

@BindingAdapter("isVisible")
fun View.bindIsVisible(visible: Boolean?) {
    visibility = if (visible == true) View.VISIBLE else View.GONE
}

@BindingAdapter("isVisibleOrInvisible")
fun View.bindIsVisibleOrInvisible(visible: Boolean?) {
    visibility = if (visible == true) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("isRefreshing")
fun SwipeRefreshLayout.bindIsRefreshing(refreshing: Boolean?) {
    isRefreshing = refreshing == true
}
