package com.example.healthapp.ui.graphs

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.healthapp.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.android.material.button.MaterialButton

class GraphsFragment : Fragment() {

    private lateinit var graphsViewModel: GraphsViewModel
    private lateinit var lineChart: LineChart

    private var lastClickedDataType: DataType? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)
        graphsViewModel = ViewModelProvider(this).get(GraphsViewModel::class.java)

        lineChart = root.findViewById(R.id.line_chart)
        lineChart.description.isEnabled = false

        // Set up the X-axis formatter
        lineChart.xAxis.valueFormatter = DateAxisFormatter()
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.granularity = 3f * 24 * 60 * 60 * 1000 // 3 days in milliseconds
        lineChart.xAxis.labelCount = 10 // Ensure 10 labels on the X-axis

        // Apply text color based on the current theme (dark or light mode)
        applyChartTextColorBasedOnTheme()

        val weightButton: MaterialButton = root.findViewById(R.id.button_weight_data)
        val calorieButton: MaterialButton = root.findViewById(R.id.button_calorie_data)
        val exerciseButton: MaterialButton = root.findViewById(R.id.button_exercise_data)

        weightButton.setOnClickListener {
            toggleGraph(DataType.WEIGHT)
        }

        calorieButton.setOnClickListener {
            toggleGraph(DataType.CALORIES)
        }

        exerciseButton.setOnClickListener {
            toggleGraph(DataType.EXERCISE)
        }

        // Load initial data (optional, you could start with an empty chart)
        updateGraph(DataType.WEIGHT, null)

        return root
    }

    private fun applyChartTextColorBasedOnTheme() {
        // Check if dark mode is active
        val isDarkMode = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }

        // Choose the appropriate color
        val textColor = if (isDarkMode) {
            ContextCompat.getColor(requireContext(), android.R.color.white) // white for dark mode
        } else {
            ContextCompat.getColor(requireContext(), android.R.color.black) // black for light mode
        }

        lineChart.legend.textColor = textColor

        // Apply color to X axis
        lineChart.xAxis.textColor = textColor

        // Apply color to Left Y axis
        lineChart.axisLeft.textColor = textColor

        // Apply color to Right Y axis (if used)
        lineChart.axisRight.textColor = textColor


        // Redraw the chart with the new colors
        lineChart.invalidate()
    }

    private fun toggleGraph(dataType: DataType) {
        if (lastClickedDataType == dataType) {
            // If the same button is clicked twice, only show that dataset
            updateGraph(dataType, null)
            lastClickedDataType = null // Reset the toggle state
        } else {
            // Otherwise, show two datasets (current and previous)
            val previousDataType = lastClickedDataType
            updateGraph(dataType, previousDataType)
            lastClickedDataType = dataType // Remember the last clicked button
        }
    }

    private fun updateGraph(primaryDataType: DataType, secondaryDataType: DataType?) {
        graphsViewModel.getCombinedData { lineDataSets ->
            requireActivity().runOnUiThread {
                lineChart.clear() // Clear the existing chart data

                val primaryDataSet = when (primaryDataType) {
                    DataType.WEIGHT -> lineDataSets.firstOrNull { it.label == "Weight Data" }
                    DataType.CALORIES -> lineDataSets.firstOrNull { it.label == "Calorie Data" }
                    DataType.EXERCISE -> lineDataSets.firstOrNull { it.label == "Exercise Data" }
                }

                val secondaryDataSet = when (secondaryDataType) {
                    DataType.WEIGHT -> lineDataSets.firstOrNull { it.label == "Weight Data" }
                    DataType.CALORIES -> lineDataSets.firstOrNull { it.label == "Calorie Data" }
                    DataType.EXERCISE -> lineDataSets.firstOrNull { it.label == "Exercise Data" }
                    else -> null
                }

                primaryDataSet?.apply {
                    axisDependency = YAxis.AxisDependency.LEFT
                    setDrawValues(false) // Disable values on data points
                }
                secondaryDataSet?.apply {
                    axisDependency = YAxis.AxisDependency.RIGHT
                    setDrawValues(false) // Disable values on data points
                }

                lineChart.data = LineData(listOfNotNull(primaryDataSet, secondaryDataSet))

                lineChart.invalidate() // Refresh the chart
            }
        }
    }

    enum class DataType {
        WEIGHT, CALORIES, EXERCISE
    }
}
