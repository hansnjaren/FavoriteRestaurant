package com.example.favoriterestaurant.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentHomeBinding
import com.example.favoriterestaurant.utils.DialogUtils
import com.example.favoriterestaurant.utils.ImageItem
import com.example.favoriterestaurant.utils.getImageItemListFlow
import com.example.favoriterestaurant.utils.imageList
import com.example.favoriterestaurant.utils.saveImageItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DataAdapter(
    private val context: Context,
    private val imageList: MutableList<ImageItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectMode = false

    private var selectList: MutableList<Boolean> = MutableList(imageList.size) { _ -> false }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameView: TextView = view.findViewById(R.id.nameView)
        val descView: TextView = view.findViewById(R.id.descView)
        val smallImageView: ImageView = view.findViewById(R.id.smallImage)
        val listElement: LinearLayout = view.findViewById(R.id.list_element)

//        val debugOrderView: TextView = view.findViewById(R.id.debug_order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageList.count { image -> image.visible }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listHolder = holder as ViewHolder
        val imageData = imageList[position]
        if(!imageData.visible) return
        val name = imageData.name
        val desc = imageData.desc
        val uri = imageData.uri



        if (selectList.size != imageList.size) selectList =
            MutableList(imageList.size) { _ -> false }

        val params = holder.listElement.layoutParams as ViewGroup.MarginLayoutParams
        if (selectList[position]) {
            holder.listElement.setBackgroundColor(ContextCompat.getColor(context, R.color.list_select))
        } else {
            holder.listElement.setBackgroundColor(ContextCompat.getColor(context, R.color.list_default))
        }
        holder.listElement.layoutParams = params
        holder.listElement.requestLayout()



        listHolder.nameView.text = name
        listHolder.descView.text = desc
        listHolder.smallImageView.setImageURI(uri)
        listHolder.itemView.setOnClickListener {


            if (selectMode) {
                Log.d("selecting", "selecting pictures")
                if (selectList.size != imageList.size) {
                    selectList = MutableList(imageList.size) { _ -> false }
                }
                selectList[position] = !selectList[position]
                if (selectList[position]) {
                    holder.listElement.setBackgroundColor(ContextCompat.getColor(context, R.color.list_select))
                } else {
                    holder.listElement.setBackgroundColor(ContextCompat.getColor(context, R.color.list_default))
                }
                holder.listElement.requestLayout()
            }
            else {
                DialogUtils.showImageDialog(
                    context = listHolder.itemView.context,
                    imageData = imageData,
                    imageList = imageList,
                    submitList = { newList -> submitList(newList) },
                    releasePermission = { uri ->
                        try {
                            listHolder.itemView.context.contentResolver.releasePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            Log.d("MyTag", "no permission therefore not released")
                        }
                    }
                )
            }

        }

//        listHolder.debugOrderView.text = imageData.order.toString()

    }

    fun submitList(newItems: List<ImageItem>) {
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
                imageList[position].order = item.order
                imageList[position].visible = item.visible
            }
        }
//        imageList.sortBy { it.order }
        notifyDataSetChanged()
        CoroutineScope(Dispatchers.IO).launch {
            saveImageItemList(context, imageList)
        }
    }

    fun moveItem(from: Int, to: Int) {
        val item = imageList.removeAt(from)
        imageList.add(to, item)
        imageList[from].order = from
        imageList[to].order = to

        Log.d("home", "from: $from, to: $to")

//        for(position in from until to + 1) {
//            imageList[position].order = position
//        }

        notifyItemMoved(from, to)
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
        submitList(newList)
    }
}

class ItemTouchHelperCallback(private val adapter: DataAdapter) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
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
        adapter.submitList(imageList.toMutableList())
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    private lateinit var adapter: DataAdapter

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view: View = binding.root

        val recyclerView: RecyclerView = binding.dataView
        val queryView: EditText = binding.query
        val searchButtonView: Button = binding.searchButton
        val searchTextView: TextView = binding.searchText

        val select = view.findViewById<Button>(R.id.list_select)
        val delete = view.findViewById<Button>(R.id.list_delete)
        val cancel = view.findViewById<Button>(R.id.list_cancel)

        var search = false

//        for (image in imageList) {
//            image.visible = true
//        }
//        CoroutineScope(Dispatchers.IO).launch {
//            saveImageItemList(requireContext(), imageList)
//        }

        adapter = DataAdapter(requireContext(), imageList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            getImageItemListFlow(requireContext()).collect { loadedList ->
                if (!search) {
                    for (image in loadedList) {
                        image.visible = true
                    }
                    queryView.setText("")
                }
                adapter.submitList(loadedList)
            }
        }

        val callback = ItemTouchHelperCallback(adapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        searchButtonView.setOnClickListener {
            val query = queryView.text.toString()
            if (query.isEmpty()){
                itemTouchHelper.attachToRecyclerView(recyclerView)
                search = false
            }
            else{
                itemTouchHelper.attachToRecyclerView(null)
                search = true
            }
            val filteredList = imageList.filter { it.name.contains(query) }

            for (position in 0 until imageList.size) {
                imageList[position].visible = filteredList.contains(imageList[position])
            }

            imageList.sortWith(
                compareByDescending<ImageItem> { it.visible }
                    .thenBy { it.order }
            )

            adapter.submitList(imageList)
            searchTextView.text = query
        }

        select.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: select, select mode: ${adapter.selectMode}")
            select.visibility = View.GONE
            delete.visibility = View.VISIBLE
            cancel.visibility = View.VISIBLE
        }

        delete.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: delete, select mode: ${adapter.selectMode}")
            adapter.delete()
            select.visibility = View.VISIBLE
            delete.visibility = View.GONE
            cancel.visibility = View.GONE
        }

        cancel.setOnClickListener {
            adapter.selectMode = !adapter.selectMode
            Log.d("mode changer check", "action: cancel, select mode: ${adapter.selectMode}")
            adapter.cancel()
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

}