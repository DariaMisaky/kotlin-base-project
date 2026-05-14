package com.daria.kotlinbase.presentation.products.list

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.daria.kotlinbase.BR
import com.daria.kotlinbase.R
import com.daria.kotlinbase.databinding.FragmentProductListBinding
import com.daria.kotlinbase.shared.base.BaseFragment
import org.koin.androidx.viewmodel.ext.android.viewModel

class ProductListFragment :
    BaseFragment<FragmentProductListBinding, ProductListViewModel>(R.layout.fragment_product_list) {

    override val viewModel: ProductListViewModel by viewModel()

    private val productsAdapter by lazy {
        ProductsAdapter(onProductClick = viewModel::onProductClicked)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setVariable(BR.viewModel, viewModel)
        binding.productsRecycler.apply {
            layoutManager = GridLayoutManager(context, GRID_SPAN)
            adapter = productsAdapter
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            productsAdapter.submitList(state.products)
        }
        binding.swipeRefresh.setOnRefreshListener { viewModel.onRefresh() }
        binding.retryButton.setOnClickListener { viewModel.onRetry() }
    }

    companion object {
        private const val GRID_SPAN = 2
    }
}
