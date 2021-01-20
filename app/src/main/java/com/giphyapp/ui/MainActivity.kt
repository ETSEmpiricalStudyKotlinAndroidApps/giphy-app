package com.giphyapp.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.giphyapp.R
import com.giphyapp.api.RetrofitInstance
import com.giphyapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        lifecycleScope.launchWhenCreated {
            val response = try {
                RetrofitInstance.api.getTrendingGifs()
            }catch(e: IOException){
                Log.e("Main activiry", "Nije OK")
                return@launchWhenCreated
            }catch(e: HttpException){
                Log.e("Main activiry", "Nije OK")
                return@launchWhenCreated
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        val menuItem: MenuItem = menu.findItem(R.id.search)
        val searchView : SearchView = menuItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextChange(newText: String?): Boolean {
                Toast.makeText(this@MainActivity,"CHANGED",Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                Toast.makeText(this@MainActivity,"SUBMITED",Toast.LENGTH_SHORT).show()
                return true
            }
        })

        return true
    }
}