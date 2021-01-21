package com.giphyapp.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.giphyapp.R
import com.giphyapp.adapters.GifAdapter
import com.giphyapp.databinding.ActivityMainBinding
import com.giphyapp.db.GifDatabase
import com.giphyapp.repository.GifsRepository
import com.giphyapp.util.Resource
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var viewModel: GifsViewModel
    lateinit var gifAdapter: GifAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupViewModel()
        setFABListener()
        setupRecyclerView()

        viewModel.trendingGifs.observe(this, Observer { response ->
            when(response) {
                is Resource.Succes -> {
                    hideProgressBar()
                    response.data?.let { giphyResponse ->
                        gifAdapter.differ.submitList(giphyResponse.data)
                    }
                }

                is Resource.Error -> {
                    hideProgressBar()
                    response.message?.let { message ->
                        Log.e("Main Activity", "An error occured: $message")
                    }
                }

                is Resource.Loading -> {
                    showProgressBar()
                }
            }
        })
    }

    private fun hideProgressBar() {
        //TODO("IMPLEMENT PROGRESS BAR")
    }

    private fun showProgressBar() {
        //TODO("IMPLEMENT PROGRESS BAR")
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

    private fun setFABListener() {
        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    private fun setupViewModel() {
        val gifsRepository = GifsRepository(GifDatabase(this))
        val viewModelProviderFactory = GifsViewModelProviderFactory(gifsRepository)
        viewModel = ViewModelProvider(this, viewModelProviderFactory).get(GifsViewModel::class.java)
    }

    private fun setupRecyclerView() = binding.rvGifs.apply {
        gifAdapter = GifAdapter(this@MainActivity)
        adapter = gifAdapter
        var sglm: StaggeredGridLayoutManager = StaggeredGridLayoutManager(2, LinearLayoutManager.VERTICAL)
        //sglm.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_NONE
        layoutManager = sglm
    }
}