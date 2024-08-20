package com.example.healthapp.ui.database

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.healthapp.R
import com.example.healthapp.data.*
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*

class DatabaseFragment : Fragment() {

    private lateinit var databaseViewModel: DatabaseViewModel
    private lateinit var entryAdapter: DatabaseAdapter
    private var selectedItem: Any? = null

    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_database, container, false)

        databaseViewModel = ViewModelProvider(this).get(DatabaseViewModel::class.java)

        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getString("font_size", "Medium") ?: "Medium"
        val textSize = when (fontSize) {
            "Small" -> 17f
            "Large" -> 22f
            else -> 20f
        }

        val recyclerView = root.findViewById<RecyclerView>(R.id.recycler_view_entries)
        recyclerView.layoutManager = LinearLayoutManager(context)
        entryAdapter = DatabaseAdapter(
            { item -> selectItem(item) },
            { item, newValue -> updateSelectedItem(item, newValue) },
            { item -> showDeleteConfirmationDialog(item) }
        )

        entryAdapter.setTextSize(textSize)
        recyclerView.adapter = entryAdapter

        root.findViewById<MaterialButton>(R.id.button_weight).setOnClickListener {
            databaseViewModel.setCategory(DatabaseCategory.WEIGHT)
        }

        root.findViewById<MaterialButton>(R.id.button_calories).setOnClickListener {
            databaseViewModel.setCategory(DatabaseCategory.CALORIES)
        }

        root.findViewById<MaterialButton>(R.id.button_exercise).setOnClickListener {
            databaseViewModel.setCategory(DatabaseCategory.EXERCISE)
        }

        databaseViewModel.entries.observe(viewLifecycleOwner, Observer { entries ->
            entryAdapter.setEntries(entries)
        })

        return root
    }

    override fun onStop() {
        super.onStop()
        databaseViewModel.clearCategory()
    }

    private fun selectItem(item: Any) {
        selectedItem = item
        Toast.makeText(context, "Selected: $item", Toast.LENGTH_SHORT).show()
    }

    private fun updateSelectedItem(item: Any, newValue: String) {
        if (newValue.isEmpty()) {
            showUpdateDialog("Please enter a valid value", false)
            return
        }

        // Check if the value exceeds the length limits
        val isFloat = newValue.toFloatOrNull() != null
        if ((isFloat && newValue.length >= 8) || (!isFloat && newValue.length >= 26)) {
            showUpdateDialog("Value was too large or had too many characters", true)
            return
        }

        when (item) {
            is WeightEntry -> {
                val updatedItem = item.copy(weight = newValue.toFloatOrNull() ?: item.weight)
                databaseViewModel.update(updatedItem)
            }
            is CalorieEntry -> {
                val calories = newValue.toFloatOrNull()
                if (calories != null) {
                    val updatedItem = item.copy(calories = calories)
                    databaseViewModel.update(updatedItem)
                    if (item.name != null) {
                        onUpdateEvent(item, item.name, calories)
                    }
                } else {
                    val updatedItem = item.copy(name = newValue)
                    databaseViewModel.update(updatedItem)
                    if (item.calories != 0f) {
                        onUpdateEvent(item, newValue, item.calories)
                    }
                }
            }
            is ExerciseEntry -> {
                val updatedItem = item.copy(minutes = newValue.toFloatOrNull() ?: item.minutes)
                databaseViewModel.update(updatedItem)
            }
        }
        showToast("Item updated")
    }

    private fun showDeleteConfirmationDialog(item: Any) {
        val formattedTimestamp = dateFormat.format(Date(
            when (item) {
                is WeightEntry -> item.timestamp
                is CalorieEntry -> item.timestamp
                is ExerciseEntry -> item.timestamp
                else -> 0L
            }
        ))

        val message = when (item) {
            is WeightEntry -> "Weight: ${item.weight}, Date: $formattedTimestamp"
            is CalorieEntry -> {
                val itemName = item.name ?: "none given"
                "Item: $itemName, Calories: ${item.calories}, Date: $formattedTimestamp"
            }
            is ExerciseEntry -> "Exercise: ${item.minutes} mins, Date: $formattedTimestamp"
            else -> item.toString()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete the following item?\n\n$message")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteSelectedItem(item)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun deleteSelectedItem(item: Any) {
        when (item) {
            is WeightEntry -> databaseViewModel.delete(item)
            is CalorieEntry -> databaseViewModel.delete(item)
            is ExerciseEntry -> databaseViewModel.delete(item)
        }
        showToast("Item deleted")
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun showUpdateAllCaloriesDialog(message: String, item: CalorieEntry, isSuccess: Boolean) {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("Update All") { dialog, _ ->
                if (item.name != null && item.calories != 0f) {
                    databaseViewModel.updateNamesInCalories(item.name!!, item.calories.toInt())
                }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun onUpdateEvent(item: CalorieEntry, name: String, calories: Float) {
        val message = "Do you want to update all items named $name to $calories calories?"
        showUpdateAllCaloriesDialog(message, item.copy(name = name, calories = calories), isSuccess = true)
    }

    private fun showUpdateDialog(message: String, isSuccess: Boolean) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
