package com.example.healthapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.healthapp.R
import com.example.healthapp.data.AppDatabase
import com.example.healthapp.data.HealthDao
import com.example.healthapp.data.HealthRepository
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private lateinit var dynamicContent: FrameLayout
    private lateinit var healthRepository: HealthRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize the FrameLayout
        dynamicContent = rootView.findViewById(R.id.dynamic_content)

        // Initialize the HealthRepository
        val healthDao: HealthDao = AppDatabase.getDatabase(requireContext()).healthDao()
        healthRepository = HealthRepository(healthDao)

        // Initialize buttons
        val aboutButton: Button = rootView.findViewById(R.id.button_about)
        val settingsButton: Button = rootView.findViewById(R.id.button_settings)
        val weeklyReportButton: Button = rootView.findViewById(R.id.button_weekly_report)
        val monthlyReportButton: Button = rootView.findViewById(R.id.button_monthly_report)

        // Set click listeners for buttons
        aboutButton.setOnClickListener {
            showAboutContent()
        }

        settingsButton.setOnClickListener {
            showSettingsContent()
        }

        weeklyReportButton.setOnClickListener {
            showWeeklyReportContent()
        }

        monthlyReportButton.setOnClickListener {
            showMonthlyReportContent()
        }

        return rootView
    }

    private fun showAboutContent() {
        dynamicContent.removeAllViews()
        val aboutView = LayoutInflater.from(requireContext()).inflate(R.layout.view_about, dynamicContent, false)

        // Set font size based on settings
        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getString("font_size", "Medium") ?: "Medium"

        val aboutTextView: TextView = aboutView.findViewById(R.id.about_text)
        aboutTextView.textSize = when (fontSize) {
            "Small" -> 17f
            "Large" -> 22f
            else -> 20f
        }

        dynamicContent.addView(aboutView)
    }

    private fun showSettingsContent() {
        dynamicContent.removeAllViews()
        val settingsView = LayoutInflater.from(requireContext()).inflate(R.layout.view_settings, dynamicContent, false)

        val fontSizeRadioGroup: RadioGroup = settingsView.findViewById(R.id.font_size_radio_group)
        val weightUnitRadioGroup: RadioGroup = settingsView.findViewById(R.id.weight_unit_radio_group)

        // Retrieve current preferences and set the corresponding radio buttons
        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getString("font_size", "Medium") ?: "Medium"
        val weightUnit = sharedPreferences.getString("weight_unit", "lbs") ?: "lbs"

        // Set the correct radio button for font size
        when (fontSize) {
            "Small" -> fontSizeRadioGroup.check(R.id.radio_small)
            "Medium" -> fontSizeRadioGroup.check(R.id.radio_medium)
            "Large" -> fontSizeRadioGroup.check(R.id.radio_large)
        }

        // Set the correct radio button for weight unit
        when (weightUnit) {
            "kgs" -> weightUnitRadioGroup.check(R.id.radio_kgs)
            "lbs" -> weightUnitRadioGroup.check(R.id.radio_lbs)
        }

        settingsView.findViewById<View>(R.id.confirm_button).setOnClickListener {
            confirmSettings(fontSizeRadioGroup, weightUnitRadioGroup)
        }

        dynamicContent.addView(settingsView)
    }

    private fun confirmSettings(fontSizeRadioGroup: RadioGroup, weightUnitRadioGroup: RadioGroup) {
        val selectedFontSizeId = fontSizeRadioGroup.checkedRadioButtonId
        val selectedWeightUnitId = weightUnitRadioGroup.checkedRadioButtonId

        if (selectedFontSizeId == -1 || selectedWeightUnitId == -1) {
            Toast.makeText(requireContext(), "Please select both font size and weight unit", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedFontSize = fontSizeRadioGroup.findViewById<RadioButton>(selectedFontSizeId).text.toString()
        val selectedWeightUnit = weightUnitRadioGroup.findViewById<RadioButton>(selectedWeightUnitId).text.toString()

        AlertDialog.Builder(requireContext())
            .setMessage("Confirm settings: Font size: $selectedFontSize, Weight unit: $selectedWeightUnit?")
            .setPositiveButton("Yes") { dialog, _ ->
                saveSettings(selectedFontSize, selectedWeightUnit)
                dialog.dismiss()
                hideSettingsOptions(fontSizeRadioGroup, weightUnitRadioGroup)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun saveSettings(fontSize: String, weightUnit: String) {
        // Save settings to shared preferences or a global object
        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("font_size", fontSize).apply()
        sharedPreferences.edit().putString("weight_unit", weightUnit).apply()
    }

    private fun hideSettingsOptions(fontSizeRadioGroup: RadioGroup, weightUnitRadioGroup: RadioGroup) {
        fontSizeRadioGroup.visibility = View.GONE
        weightUnitRadioGroup.visibility = View.GONE
        dynamicContent.findViewById<View>(R.id.confirm_button)?.visibility = View.GONE
        Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun showWeeklyReportContent() {
        dynamicContent.removeAllViews()
        val weeklyReportView = LayoutInflater.from(requireContext()).inflate(R.layout.view_weekly_report, dynamicContent, false)

        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getString("font_size", "Medium") ?: "Medium"
        val isKg = sharedPreferences.getString("weight_unit", "lbs") == "kgs"

        val textSize = when (fontSize) {
            "Small" -> 17f
            "Large" -> 22f
            else -> 20f
        }

        weeklyReportView.findViewById<TextView>(R.id.weekly_calories_heading).textSize = textSize
        weeklyReportView.findViewById<TextView>(R.id.weekly_calories_text).textSize = textSize
        weeklyReportView.findViewById<TextView>(R.id.weekly_weight_heading).textSize = textSize
        weeklyReportView.findViewById<TextView>(R.id.weekly_weight_text).textSize = textSize
        weeklyReportView.findViewById<TextView>(R.id.weekly_exercise_heading).textSize = textSize
        weeklyReportView.findViewById<TextView>(R.id.weekly_exercise_text).textSize = textSize

        lifecycleScope.launch {
            val (last7DaysCalories, previous7DaysCalories) = healthRepository.get7DayCaloriesAverage()
            val (last7DaysWeight, previous7DaysWeight) = healthRepository.get7DayWeightAverage(isKg)
            val (last7DaysExercise, previous7DaysExercise) = healthRepository.get7DayExerciseMinutesAverage()

            val caloriesDifference = calculatePercentageDifference(previous7DaysCalories, last7DaysCalories)
            val weightDifference = calculatePercentageDifference(previous7DaysWeight, last7DaysWeight)
            val exerciseDifference = calculatePercentageDifference(previous7DaysExercise, last7DaysExercise)

            weeklyReportView.findViewById<TextView>(R.id.weekly_calories_text).text =
                "Calories: ${String.format("%.2f", last7DaysCalories)} (Change: $caloriesDifference%)"
            weeklyReportView.findViewById<TextView>(R.id.weekly_weight_text).text =
                "Weight: ${String.format("%.2f", last7DaysWeight)} ${if (isKg) "kg" else "lbs"} (Change: $weightDifference%)"
            weeklyReportView.findViewById<TextView>(R.id.weekly_exercise_text).text =
                "Exercise: ${String.format("%.2f", last7DaysExercise)} mins (Change: $exerciseDifference%)"
        }

        dynamicContent.addView(weeklyReportView)
    }


    private fun showMonthlyReportContent() {
        dynamicContent.removeAllViews()
        val monthlyReportView = LayoutInflater.from(requireContext()).inflate(R.layout.view_monthly_report, dynamicContent, false)

        val sharedPreferences = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val fontSize = sharedPreferences.getString("font_size", "Medium") ?: "Medium"
        val isKg = sharedPreferences.getString("weight_unit", "lbs") == "kgs"

        val textSize = when (fontSize) {
            "Small" -> 17f
            "Large" -> 22f
            else -> 20f
        }

        monthlyReportView.findViewById<TextView>(R.id.monthly_calories_heading).textSize = textSize
        monthlyReportView.findViewById<TextView>(R.id.monthly_calories_text).textSize = textSize
        monthlyReportView.findViewById<TextView>(R.id.monthly_weight_heading).textSize = textSize
        monthlyReportView.findViewById<TextView>(R.id.monthly_weight_text).textSize = textSize
        monthlyReportView.findViewById<TextView>(R.id.monthly_exercise_heading).textSize = textSize
        monthlyReportView.findViewById<TextView>(R.id.monthly_exercise_text).textSize = textSize

        lifecycleScope.launch {
            val (last30DaysCalories, previous30DaysCalories) = healthRepository.get30DayCaloriesAverage()
            val (last30DaysWeight, previous30DaysWeight) = healthRepository.get30DayWeightAverage(isKg)
            val (last30DaysExercise, previous30DaysExercise) = healthRepository.get30DayExerciseMinutesAverage()

            val caloriesDifference = calculatePercentageDifference(previous30DaysCalories, last30DaysCalories)
            val weightDifference = calculatePercentageDifference(previous30DaysWeight, last30DaysWeight)
            val exerciseDifference = calculatePercentageDifference(previous30DaysExercise, last30DaysExercise)

            monthlyReportView.findViewById<TextView>(R.id.monthly_calories_text).text =
                "Calories: ${String.format("%.2f", last30DaysCalories)} (Change: $caloriesDifference%)"
            monthlyReportView.findViewById<TextView>(R.id.monthly_weight_text).text =
                "Weight: ${String.format("%.2f", last30DaysWeight)} ${if (isKg) "kg" else "lbs"} (Change: $weightDifference%)"
            monthlyReportView.findViewById<TextView>(R.id.monthly_exercise_text).text =
                "Exercise: ${String.format("%.2f", last30DaysExercise)} mins (Change: $exerciseDifference%)"
        }

        dynamicContent.addView(monthlyReportView)
    }



    private fun calculatePercentageDifference(previous: Float, current: Float): String {
        return if (previous == 0f) {
            "N/A"
        } else {
            val difference = ((current - previous) / abs(previous)) * 100
            String.format("%.2f", difference)
        }
    }
}
