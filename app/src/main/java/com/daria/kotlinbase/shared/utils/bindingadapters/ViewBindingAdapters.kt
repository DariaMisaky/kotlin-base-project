package com.daria.kotlinbase.shared.utils.bindingadapters

import android.view.View
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar

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

@BindingAdapter("onRefresh")
fun SwipeRefreshLayout.bindOnRefresh(listener: SwipeRefreshLayout.OnRefreshListener?) {
    setOnRefreshListener(listener)
}

@Suppress("UNCHECKED_CAST")
@BindingAdapter("items")
fun <T> RecyclerView.bindItems(items: List<T>?) {
    (adapter as? ListAdapter<T, *>)?.submitList(items)
}

@BindingAdapter("onNavigationClick")
fun MaterialToolbar.bindOnNavigationClick(listener: View.OnClickListener?) {
    setNavigationOnClickListener(listener)
}
