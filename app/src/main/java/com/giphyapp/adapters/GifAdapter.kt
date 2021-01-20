package com.giphyapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.giphyapp.databinding.GifItemBinding
import com.giphyapp.models.Data

class GifAdapter(var context: Context) : RecyclerView.Adapter<GifAdapter.GifViewHolder>() {

    inner class GifViewHolder(val binding:GifItemBinding): RecyclerView.ViewHolder(binding.root)

    private val differCallBack = object : DiffUtil.ItemCallback<Data>() {
        override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallBack)
    var gifs: List<Data>
        get() = differ.currentList
        set(value) {differ.submitList(value)}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GifViewHolder {
        return GifViewHolder( GifItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: GifViewHolder, position: Int) {
        holder.binding.apply {
            val gif = gifs[position]
            Glide.with(context).load(gif.images.downsized.url).into(ivGif)
            setOnItemClickListener {
                onItemClickListener?.let { it(gif) }
            }
        }
    }

    override fun getItemCount(): Int {
        return gifs.size
    }

    private var onItemClickListener: ((Data) -> Unit)? = null

    fun setOnItemClickListener(listener: (Data) -> Unit) {
        onItemClickListener = listener
    }
}