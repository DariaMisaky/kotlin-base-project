package com.daria.kotlinbase.presentation.products.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daria.kotlinbase.databinding.ItemProductBinding
import com.daria.kotlinbase.domain.entities.Product

class ProductsAdapter(
    private val onProductClick: (Int) -> Unit,
) : ListAdapter<Product, ProductsAdapter.ProductViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
        )
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.product = product
            binding.root.setOnClickListener { onProductClick(product.id) }
            binding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
        }
    }
}
