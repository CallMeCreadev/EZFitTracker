package com.example.healthapp.ui.entry

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthapp.R
import com.example.healthapp.data.CalorieEntry
import com.example.healthapp.data.ExerciseEntry
import com.example.healthapp.data.WeightEntry
import com.google.android.material.button.MaterialButton

class EntryFragment : Fragment() {

    private lateinit var entryViewModel: EntryViewModel

    private lateinit var editTextValue: EditText
    private lateinit var buttonUpdate: MaterialButton
    private lateinit var radioGroupExercise: RadioGroup
    private lateinit var radioMinutes: RadioButton
    private lateinit var radioSteps: RadioButton
    private lateinit var buttonTimeExercise: MaterialButton
    private lateinit var btnStartStop: MaterialButton
    private lateinit var tvTimer: TextView
    private lateinit var btnSaveTime: MaterialButton
    private lateinit var btnReset: MaterialButton
    private var isKg: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (entryViewModel.isTimerRunning) {
                tvTimer.text = entryViewModel.formatElapsedTime(entryViewModel.getElapsedTime())
                handler.postDelayed(this, 1000) // Update every second
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_entry, container, false)

        entryViewModel = ViewModelProvider(requireActivity()).get(EntryViewModel::class.java)

        val buttonInputWeight: MaterialButton = root.findViewById(R.id.button_input_weight)
        val buttonInputCalories: MaterialButton = root.findViewById(R.id.button_input_calories)
        val buttonInputExercise: MaterialButton = root.findViewById(R.id.button_input_exercise)
        editTextValue = root.findViewById(R.id.edit_text_value)
        buttonUpdate = root.findViewById(R.id.button_update)
        radioGroupExercise = root.findViewById(R.id.radio_group_exercise)
        radioMinutes = root.findViewById(R.id.radio_minutes)
        radioSteps = root.findViewById(R.id.radio_steps)
        buttonTimeExercise = root.findViewById(R.id.button_time_exercise)
        btnStartStop = root.findViewById(R.id.btnStartStop)
        tvTimer = root.findViewById(R.id.tvTimer)
        btnSaveTime = root.findViewById(R.id.btnSaveTime)
        btnReset = root.findViewById(R.id.btnReset)

        // Retrieve isKg preference from SharedPreferences
        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        isKg = sharedPreferences.getString("weight_unit", "lbs") == "kgs"

        buttonInputWeight.setOnClickListener { showInputFields(if (isKg) "kgs" else "lbs") }
        buttonInputCalories.setOnClickListener { showInputFields("Cals or item") }
        buttonInputExercise.setOnClickListener { showExerciseInputFields() }
        buttonTimeExercise.setOnClickListener { showTimerView() }

        buttonUpdate.setOnClickListener { handleUpdate() }

        btnStartStop.setOnClickListener {
            if (entryViewModel.isTimerRunning) {
                entryViewModel.stopTimer()
            } else {
                entryViewModel.startTimer()
                handler.post(updateTimerRunnable) // Start updating the timer
            }
            updateStartStopButton()
        }

        btnReset.setOnClickListener {
            entryViewModel.resetTimer()
            updateTimerUI()
            updateStartStopButton()
        }

        btnSaveTime.setOnClickListener {
            val minutes = entryViewModel.saveElapsedTime()
            val exerciseEntry = ExerciseEntry(minutes = minutes, timestamp = System.currentTimeMillis())
            entryViewModel.insert(exerciseEntry)
            showUpdateDialog("Exercise updated 🎉", true)
        }

        updateTimerUI() // Update the timer UI based on the ViewModel state
        updateStartStopButton()

        return root
    }

    private fun isValidInput(value: String): Boolean {
        val isFloat = value.toFloatOrNull() != null
        return (isFloat && value.length < 8) || (!isFloat && value.length < 26)
    }

    private fun showInvalidInputDialog() {
        showUpdateDialog("Value was too large or had too many characters", false)
    }

    private fun showInputFields(hint: String) {
        val value = editTextValue.text.toString()
        if (!isValidInput(value)) {
            showInvalidInputDialog()
            return
        }

        radioGroupExercise.visibility = View.GONE
        editTextValue.hint = hint
        editTextValue.visibility = View.VISIBLE
        buttonUpdate.visibility = View.VISIBLE
        hideTimerView()
    }

    private fun showExerciseInputFields() {
        val value = editTextValue.text.toString()
        if (!isValidInput(value)) {
            showInvalidInputDialog()
            return
        }

        editTextValue.hint = "Minutes or Steps"
        radioGroupExercise.visibility = View.VISIBLE
        editTextValue.visibility = View.VISIBLE
        buttonUpdate.visibility = View.VISIBLE
        hideTimerView()
    }

    private fun handleUpdate() {
        val value = editTextValue.text.toString()
        val isNumeric = value.toFloatOrNull() != null

        // Validation check before proceeding
        if (!isValidInput(value)) {
            showInvalidInputDialog()
            return
        }

        when {
            radioGroupExercise.visibility == View.VISIBLE -> {
                // Handling exercise input
                val minutes = if (radioSteps.isChecked) {
                    value.toFloatOrNull()?.div(100) ?: 0f
                } else {
                    value.toFloatOrNull() ?: 0f
                }
                if (minutes > 0) {
                    val exerciseEntry = ExerciseEntry(minutes = minutes, timestamp = System.currentTimeMillis())
                    entryViewModel.insert(exerciseEntry)
                    showUpdateDialog("Exercise updated 🎉", true)
                } else {
                    showUpdateDialog("Please enter a valid value", false)
                }
            }
            editTextValue.hint == "lbs" || editTextValue.hint == "kgs" -> {
                if (isNumeric) {
                    val weight = value.toFloat()
                    val weightEntry = WeightEntry(weight = weight, timestamp = System.currentTimeMillis())
                    Log.d("EntryFragment", "Attempting to insert weight: $weight ${if (isKg) "kg" else "lbs"}")
                    entryViewModel.insert(weightEntry, isKg)
                    showUpdateDialog("Weight updated 🎉", true)
                } else {
                    Log.e("EntryFragment", "Non-numeric value entered for weight")
                    showUpdateDialog("Please enter a numeric value", false)
                }
            }
            editTextValue.hint == "Cals or item" -> {
                entryViewModel.processCalorieEntry(value)
                showUpdateDialog("Calories updated 🎉", true)
            }
        }

        // Hide input fields after updating
        editTextValue.visibility = View.GONE
        buttonUpdate.visibility = View.GONE
        radioGroupExercise.visibility = View.GONE
        editTextValue.text.clear()
    }

    private fun showUpdateDialog(message: String, isSuccess: Boolean) {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        dialog.findViewById<TextView>(android.R.id.message)?.setTextColor(
            if (isSuccess) resources.getColor(R.color.teal_700) else resources.getColor(android.R.color.black)
        )
    }

    private fun showTimerView() {
        editTextValue.visibility = View.GONE
        radioGroupExercise.visibility = View.GONE
        buttonUpdate.visibility = View.GONE

        btnStartStop.visibility = View.VISIBLE
        tvTimer.visibility = View.VISIBLE
        btnSaveTime.visibility = View.VISIBLE
        btnReset.visibility = View.VISIBLE
        btnSaveTime.isEnabled = false
    }

    private fun hideTimerView() {
        btnStartStop.visibility = View.GONE
        tvTimer.visibility = View.GONE
        btnSaveTime.visibility = View.GONE
        btnReset.visibility = View.GONE
        handler.removeCallbacks(updateTimerRunnable) // Stop updating the timer if view is hidden
    }

    private fun updateTimerUI() {
        tvTimer.text = entryViewModel.formatElapsedTime(entryViewModel.getElapsedTime())
        if (entryViewModel.isTimerRunning) {
            handler.postDelayed(updateTimerRunnable, 1000)
        }
    }

    private fun updateStartStopButton() {
        btnStartStop.text = if (entryViewModel.isTimerRunning) "Stop Timer" else "Start Timer"
        btnSaveTime.isEnabled = !entryViewModel.isTimerRunning
        btnReset.isEnabled = entryViewModel.getElapsedTime() > 0
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimerRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateTimerUI()
    }
}
