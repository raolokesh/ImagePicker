package com.lokesh.imagepicker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lokesh.imagepicker.adapter.GalleryAdapter

class GalleryBottomSheetFragment(
    private val imageUrlsGallery: List<String>,
    private val maxSelection: Int,
    private val onImagesSelected: (List<String>) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_gallery, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.gallery_view)
        recyclerView.layoutManager = GridLayoutManager(context, 4) // 3 columns
        val galleryAdapter =
            GalleryAdapter(requireContext(), imageUrlsGallery, maxSelection) { selectedImages ->
                // Update the activity with the selected images
                onImagesSelected(selectedImages)
            }
        recyclerView.adapter = galleryAdapter
        return view
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.isHideable = true

            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            it.requestLayout()
        }
    }


}
