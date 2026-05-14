package com.daria.kotlinbase.presentation.products.detail

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.daria.kotlinbase.R
import com.daria.kotlinbase.databinding.FragmentProductDetailBinding
import com.daria.kotlinbase.shared.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ProductDetailFragment :
    BaseFragment<FragmentProductDetailBinding, ProductDetailViewModel>(R.layout.fragment_product_detail) {
    private val args by navArgs<ProductDetailFragmentArgs>()

    override val viewModel: ProductDetailViewModel by viewModel {
        parametersOf(args.productId)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
    }
}
