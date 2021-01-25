package com.giphyapp.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Environment
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.giphyapp.databinding.GifItemBinding
import com.giphyapp.models.Data
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import com.bumptech.glide.request.target.Target

class FileAdapter(var context: Context) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

    inner class FileViewHolder(val binding:GifItemBinding): RecyclerView.ViewHolder(binding.root)

    private val differCallBack = object : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallBack)
    var files: List<File>
        get() = differ.currentList
        set(value) {differ.submitList(value)}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder( GifItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
        ))
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {

        holder.itemView.apply {
            val file = files[position]

            Glide.with(context)
                    .load(file.path)
                    //.thumbnail(Glide.with(context).load(file.path))
                    .into(holder.binding.ivGif)


            holder.binding.ivGif.setOnClickListener {
                onItemClickListener?.let { it(file) }
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    private var onItemClickListener: ((File) -> Unit)? = null

    fun setOnItemClickListener(listener: (File) -> Unit) {
        onItemClickListener = listener
    }
}