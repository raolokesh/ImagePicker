package com.lokesh.imagepicker.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lokesh.imagepicker.R

class GalleryAdapter(
    private val context: Context,
    private val imageUrls: List<String>,
    private val maxSelection: Int,
    private val onSelectionChanged: (List<String>) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val selectedImages = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        Glide.with(context).load(imageUrl).into(holder.imageView)
//        holder.itemView.setBackgroundColor(if (selectedImages.contains(imageUrl)) Color.BLUE else Color.TRANSPARENT)
        holder.itemView.setOnClickListener {
            if (selectedImages.contains(imageUrl)) {
                holder.imageSelected.isChecked = false
                holder.imageSelected.visibility = View.GONE
                selectedImages.remove(imageUrl)
            } else if (selectedImages.size < maxSelection) {
                holder.imageSelected.isChecked = true
                holder.imageSelected.visibility = View.VISIBLE
                selectedImages.add(imageUrl)
            } else {
                Toast.makeText(
                    context,
                    "You can select up to $maxSelection images",
                    Toast.LENGTH_SHORT
                ).show()
            }
            onSelectionChanged(selectedImages.toList())
        }
    }

    override fun getItemCount(): Int {
        return imageUrls.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val imageSelected: RadioButton = itemView.findViewById(R.id.imageSelected)
    }
}
