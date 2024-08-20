package com.example.healthapp.ui.database

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.healthapp.R
import com.example.healthapp.data.CalorieEntry
import com.example.healthapp.data.ExerciseEntry
import com.example.healthapp.data.WeightEntry
import java.text.SimpleDateFormat
import java.util.*

import java.text.DecimalFormat

class DatabaseAdapter(
    private val onItemClick: (Any) -> Unit,
    private val onUpdateClick: (Any, String) -> Unit,
    private val onDeleteClick: (Any) -> Unit
) : RecyclerView.Adapter<DatabaseAdapter.EntryViewHolder>() {

    private var entries: List<Any> = emptyList()
    private var textSize: Float = 20f // Default size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entry, parent, false)
        return EntryViewHolder(view, textSize)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
        holder.itemView.setOnClickListener { onItemClick(entry) }

        holder.updateButton.setOnClickListener {
            val newValue = holder.editTextValue.text.toString()
            onUpdateClick(entry, newValue)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(entry)
        }
    }

    fun setTextSize(size: Float) {
        this.textSize = size
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = entries.size

    fun setEntries(entries: List<Any>) {
        this.entries = entries
        notifyDataSetChanged()
    }

    class EntryViewHolder(itemView: View, private val textSize: Float) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.text_entry)
        val editTextValue: EditText = itemView.findViewById(R.id.edit_text_value)
        val updateButton: Button = itemView.findViewById(R.id.button_update)
        val deleteButton: Button = itemView.findViewById(R.id.button_delete)

        private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        private val decimalFormat = DecimalFormat("#.##") // For formatting to 2 decimal places

        fun bind(entry: Any) {
            val context = itemView.context
            val isKg = isKgPreference(context)
            val formattedTimestamp = dateFormat.format(Date(
                when (entry) {
                    is WeightEntry -> entry.timestamp
                    is CalorieEntry -> entry.timestamp
                    is ExerciseEntry -> entry.timestamp
                    else -> 0L
                }
            ))

            val displayText = when (entry) {
                is WeightEntry -> {
                    editTextValue.hint = if (isKg) "kgs" else "lbs"
                    editTextValue.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    "${entry.weight} ${if (isKg) "kg" else "lbs"}    |    $formattedTimestamp"
                }
                is CalorieEntry -> {
                    editTextValue.hint = "Cals or Item"
                    editTextValue.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                    val itemName = entry.name ?: "none"
                    "$itemName    |    ${entry.calories}cal    |    $formattedTimestamp"
                }
                is ExerciseEntry -> {
                    editTextValue.hint = "Minutes"
                    editTextValue.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    "${decimalFormat.format(entry.minutes)}min    |    $formattedTimestamp"
                }
                else -> entry.toString()
            }
            textView.text = displayText
            textView.textSize = textSize
            editTextValue.textSize = textSize
        }

        private fun isKgPreference(context: Context): Boolean {
            val sharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            return sharedPreferences.getString("weight_unit", "lbs") == "kgs"
        }
    }
}

