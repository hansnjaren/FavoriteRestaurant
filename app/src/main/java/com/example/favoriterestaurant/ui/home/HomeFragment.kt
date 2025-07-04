package com.example.favoriterestaurant.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.TextView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.favoriterestaurant.R
import com.example.favoriterestaurant.databinding.FragmentHomeBinding
import org.w3c.dom.Text
import java.util.ArrayList
import kotlin.collections.MutableList

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val listView: ListView = binding.listView

        val itemList: MutableList<String> = mutableListOf()

        for (i in 0 until 30) {
            itemList.add(i.toString())
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, itemList)
        listView.adapter = adapter

        listView.setOnItemClickListener { parent, view, position, id ->
            val item = parent.getItemAtPosition(position).toString()
//            Toast.makeText(context, "$position 번째 아이템 클릭: $item", Toast.LENGTH_SHORT).show()
            AlertDialog.Builder(requireContext())
                .setTitle("Item Click")
                .setMessage("Chosen: $item")
                .setPositiveButton("OK", null)
                .show()
        }


        return root
    }

}