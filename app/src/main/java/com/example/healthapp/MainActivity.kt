package com.example.healthapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.healthapp.data.AppDatabase
import com.example.healthapp.data.HealthRepository
import com.example.healthapp.data.HealthDao
import com.example.healthapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var healthRepository: HealthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the repository
        val healthDao = AppDatabase.getDatabase(application).healthDao() // Assuming you're using Room
        healthRepository = HealthRepository(healthDao)

        // Run the deletion logic once per app startup
        runDeletionOnce()

        // Set up the custom Toolbar
        val toolbar: Toolbar = binding.toolbar
        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Define top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_entry, R.id.navigation_database, R.id.navigation_graph
            )
        )

        // Link the NavController with the ActionBar and BottomNavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    private fun runDeletionOnce() {
        val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
        val hasDeletedOldRecords = sharedPreferences.getBoolean("hasDeletedOldRecords", false)

        if (!hasDeletedOldRecords) {
            // Run the deletion logic
            lifecycleScope.launch {
                healthRepository.deleteRecordsOlderThan61Days() // Ensure this function exists in HealthRepository

                // Mark the operation as completed for this session
                sharedPreferences.edit().putBoolean("hasDeletedOldRecords", true).apply()
            }
        }
    }
}
