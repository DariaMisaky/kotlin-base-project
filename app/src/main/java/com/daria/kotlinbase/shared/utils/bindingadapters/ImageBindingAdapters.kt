package com.daria.kotlinbase.shared.utils.bindingadapters

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import coil3.load
import coil3.request.crossfade

@BindingAdapter("imageUrl")
fun ImageView.bindImageUrl(url: String?) {
    if (url.isNullOrBlank()) return
    load(url) {
        crossfade(true)
    }
}
