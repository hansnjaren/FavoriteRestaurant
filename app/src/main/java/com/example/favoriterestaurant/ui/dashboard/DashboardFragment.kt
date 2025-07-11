package com.example.favoriterestaurant.ui.dashboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentDashboardBinding
import com.example.favoriterestaurant.utils.DialogUtils
import com.example.favoriterestaurant.utils.ImageItem
import com.example.favoriterestaurant.utils.getImageItemListFlow
import com.example.favoriterestaurant.utils.imageList
import com.example.favoriterestaurant.utils.saveImageItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ImageAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageItem>
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    var selectMode = false

    private var selectList: MutableList<Boolean> = MutableList(imageList.size) { _ -> false }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val imageViewPadding: FrameLayout = view.findViewById(R.id.imageViewPadding)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageData = imageList[position]
        val uri = imageData.uri
        holder.imageView.setImageURI(uri)

        if (selectList.size != imageList.size) selectList =
            MutableList(imageList.size) { _ -> false }

        // val params = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams
//        val paddingParams = holder.imageViewPadding.layoutParams as ViewGroup.MarginLayoutParams
        if (selectList[position]) {
//            paddingParams.setMargins(16)
            holder.imageViewPadding.setPadding(16)
        } else {
//            paddingParams.setMargins(0)
            holder.imageViewPadding.setPadding(0)
        }
//        holder.imageViewPadding.layoutParams = paddingParams
        holder.imageViewPadding.requestLayout()



        holder.itemView.setOnClickListener {
            if (selectMode) {
                Log.d("selecting", "selecting pictures")
                if (selectList.size != imageList.size) {
                    selectList = MutableList(imageList.size) { _ -> false }
                }
                selectList[position] = !selectList[position]

                // val params = holder.imageView.layoutParams as ViewGroup.MarginLayoutParams
//                val paddingParams = holder.imageViewPadding.layoutParams as ViewGroup.MarginLayoutParams
                if (selectList[position]) {
//            paddingParams.setMargins(16)
                    holder.imageViewPadding.setPadding(16)
                } else {
//            paddingParams.setMargins(0)
                    holder.imageViewPadding.setPadding(0)
                }
//                holder.imageViewPadding.layoutParams = paddingParams
                holder.imageViewPadding.requestLayout()
            } else {
                DialogUtils.showImageDialog(
                    context = holder.itemView.context,
                    imageData = imageData,
                    imageList = imageList,
                    submitList = { newList -> submitList(newList, false) },
                    releasePermission = { uri ->
                        try {
                            holder.itemView.context.contentResolver.releasePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            // ignore
                        }
                    }
                )
            }
        }
    }

    override fun getItemCount(): Int = imageList.size

    // 데이터 갱신 메서드
    fun submitList(newItems: List<ImageItem>, add: Boolean) {
        val uriList: MutableList<Uri> = mutableListOf()
        for (image in imageList) {
            uriList.add(image.uri)
        }
        for (item in newItems) {
            if (!uriList.contains(item.uri)) {
                imageList.add(item)
            }
            else {
                val position = imageList.map { it.uri }.indexOf(item.uri)
                Log.d("submitList", position.toString())
                if (!add) imageList[position].order = item.order
                imageList[position].visible = item.visible
            }
        }

        if (add) {
            for ((counter, image) in imageList.withIndex()) {
                image.order = counter
            }
        }

        imageList.sortBy { it.order }
        notifyDataSetChanged()
        CoroutineScope(Dispatchers.IO).launch {
            saveImageItemList(context, imageList)
        }
    }

    fun cancel() {
        assert(imageList.size == selectList.size)
        for (position in 0 until selectList.size) {
            if (selectList[position]) {
                selectList[position] = false
                notifyItemChanged(position)
            }
        }
    }

    fun delete() {
        assert(imageList.size == selectList.size)
        var newList = imageList.toMutableList()
        for (position in selectList.size - 1 downTo 0) {
            if (selectList[position]) {
                selectList[position] = false
                newList = newList.apply { removeAt(position) }
                try {
                    context.contentResolver.releasePersistableUriPermission(
                        imageList[position].uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    // ignore
                }
            }
        }
        imageList.clear()
        imageList.addAll(newList)
        for (position in 0 until imageList.size) {
            imageList[position].order = position
        }
        submitList(newList, false)
    }

    fun moveItem(from: Int, to: Int) {
        val item = imageList.removeAt(from)
        imageList.add(to, item)
        imageList[from].order = from
        imageList[to].order = to
        Log.d("dash", "from: $from, to: $to")
        for(position in from until to + 1) {
            imageList[position].order = position
        }
        notifyItemMoved(from, to)
    }
}

class ItemTouchHelperCallback(private val adapter: ImageAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags =
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        val swipeFlags = 0
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.moveItem(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // ignore since no swipe motion detection needed
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        adapter.submitList(imageList.toMutableList(), false)
    }
}

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    private lateinit var adapter: ImageAdapter

    private val REQUEST_CODE = 1001

    private val pickImages = registerForActivityResult(
//        ActivityResultContracts.PickMultipleVisualMedia()
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val images: MutableList<ImageItem> = mutableListOf()
            for ((counter, uri) in uris.withIndex()) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
                val cursor = requireContext().contentResolver.query(uri, projection, null, null, null)
                var fileName = ""
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        fileName = it.getString(nameIndex)
                    }
                }
                images.add(
                    ImageItem(
                        order = imageList.size + counter,
                        visible = true,
                        uri = uri,
                        name = fileName,
                        desc = "No description",
                        address = null,
                        phoneNumber = null,
                        coord = null,
                        marker = null
                    )
                )
            }
            adapter.submitList(images, true)
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val view: View = binding.root

        val recyclerView: RecyclerView = binding.imageListView

        val buttonSelectImages = view.findViewById<Button>(R.id.buttonSelectImages)

        val select = view.findViewById<Button>(R.id.select)
        val delete = view.findViewById<Button>(R.id.delete)
        val cancel = view.findViewById<Button>(R.id.cancel)


        adapter = ImageAdapter(requireContext(), imageList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            getImageItemListFlow(requireContext()).collect { loadedList ->
                var nonVisible = false
                for (image in loadedList) {
                    Log.d("visible", image.visible.toString())
                    Log.d("order", image.order.toString())
                    if(!image.visible) nonVisible = true
                    image.visible = true
                }
                if (nonVisible) loadedList.sortedBy { it.order }
                adapter.submitList(loadedList, false)
            }
        }

        val callback = ItemTouchHelperCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        buttonSelectImages.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            }
            else {
//                pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                pickImages.launch(arrayOf("image/*"))
            }
        }

        select.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: select, select mode: ${adapter.selectMode}")
            itemTouchHelper.attachToRecyclerView(null)
            select.visibility = View.GONE
            delete.visibility = View.VISIBLE
            cancel.visibility = View.VISIBLE
        }

        delete.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: delete, select mode: ${adapter.selectMode}")
            adapter.delete()
            itemTouchHelper.attachToRecyclerView(recyclerView)
            select.visibility = View.VISIBLE
            delete.visibility = View.GONE
            cancel.visibility = View.GONE
        }

        cancel.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: cancel, select mode: ${adapter.selectMode}")
            adapter.cancel()
            itemTouchHelper.attachToRecyclerView(recyclerView)
            select.visibility = View.VISIBLE
            delete.visibility = View.GONE
            cancel.visibility = View.GONE
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pickImages.launch(arrayOf("image/*"))
        }
    }
}