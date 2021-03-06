
package com.s24.mastodonclient.ui.toot_list
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.s24.mastodonclient.BuildConfig
import com.s24.mastodonclient.R
import com.s24.mastodonclient.databinding.FragmentTootListBinding
import com.s24.mastodonclient.entity.Account
import com.s24.mastodonclient.entity.Toot
import com.s24.mastodonclient.ui.toot_detail.TootDetailActivity
import com.s24.mastodonclient.ui.toot_detail.TootDetailFragment
import com.s24.mastodonclient.ui.toot_edit.TootEditActivity

class TootListFragment: Fragment(R.layout.fragment_toot_list),
    TootListAdapter.Callback {

    companion object {
        val TAG = TootListFragment::class.java.simpleName

        private const val REQUEST_CODE_TOOT_EDIT = 0x01
        private const val BUNDLE_KEY_TIMELINE_TYPE_ORDINAL = "timeline_type_ordinal"

        @JvmStatic
        fun newInstance(timelineType: TimelineType): TootListFragment{
            val args = Bundle().apply {
                putInt(BUNDLE_KEY_TIMELINE_TYPE_ORDINAL, timelineType.ordinal)
            }
            return TootListFragment().apply {
                arguments = args
            }
        }
    }

    private lateinit var layoutManager: LinearLayoutManager

    private var timelineType = TimelineType.PublicTimeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireArguments().also {
            val typeOrdinal = it.getInt(
                BUNDLE_KEY_TIMELINE_TYPE_ORDINAL,
                TimelineType.PublicTimeline.ordinal
            )
            timelineType = TimelineType.values()[typeOrdinal]
        }
    }

    private val viewModel: TootListViewModel by viewModels {
        TootListViewModelFactory(
            BuildConfig.INSTANCE_URL,
            BuildConfig.USERNAME,
            timelineType,
            lifecycleScope,
            requireContext()
        )
    }
    
    private val loadNextScrollListener = object : RecyclerView
            .OnScrollListener(){

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int){
                    super.onScrolled(recyclerView, dx, dy)
                    val isLoadingSnapshot = viewModel.isLoading.value?: return
                    if (isLoadingSnapshot || !viewModel.hasNext){
                        return
                    }

                    val visibleItemCount = recyclerView.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosision = layoutManager
                        .findFirstVisibleItemPosition()

                    if ((totalItemCount - visibleItemCount) <=
                            firstVisibleItemPosision){
                        viewModel.loadNext()
                    }
                }
            }

    private var binding: FragmentTootListBinding? = null


    //private val tootRepository = TootRepository(API_BASE_URL)

    private lateinit var adapter: TootListAdapter



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tootListSnapshot = viewModel.tootList.value?: ArrayList<Toot>().also{
            viewModel.tootList.value = it
        }

        adapter = TootListAdapter(layoutInflater, tootListSnapshot, this)
        layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )
        val bindingData: FragmentTootListBinding? = DataBindingUtil.bind(view)
        binding = bindingData?: return

        bindingData.recyclerView.also {
            it.layoutManager = layoutManager
            it.adapter = adapter
            it.addOnScrollListener(loadNextScrollListener)
        }
        bindingData.swipeRefreshLayout.setOnRefreshListener {
            viewModel.clear()
            viewModel.loadNext()
        }
        bindingData.fab.setOnClickListener{
            launchTootEditActivity()
        }
        viewModel.isLoading.observe(viewLifecycleOwner, Observer {
            binding?.swipeRefreshLayout?.isRefreshing = it
        })
        viewModel.accountInfo.observe(viewLifecycleOwner, Observer {
            showAccountInfo(it)
        })
        viewModel.tootList.observe(viewLifecycleOwner, Observer {
            adapter.notifyDataSetChanged()
        })
        viewLifecycleOwner.lifecycle.addObserver(viewModel)
    }
    private fun launchTootEditActivity(){
        val intent = TootEditActivity.newIntent(requireContext())
        startActivityForResult(intent, REQUEST_CODE_TOOT_EDIT)
    }

    private fun showAccountInfo(accountInfo: Account){
        val activity = requireActivity()
        if (activity is AppCompatActivity){
            activity.supportActionBar?.subtitle = accountInfo.username
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_TOOT_EDIT
            && requestCode == Activity.RESULT_OK){
            viewModel.clear()
            viewModel.loadNext()
        }
    }

    override fun onDestroyView(){
        super.onDestroyView()

        binding?.unbind()
    }

    override fun openDetail(toot: Toot) {
        val intent = TootDetailActivity.newIntent(requireContext(), toot)
        startActivity(intent)
    }

    override fun delete(toot: Toot) {
        viewModel.delete(toot)
    }
}