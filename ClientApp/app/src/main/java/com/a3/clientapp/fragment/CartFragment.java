package com.a3.clientapp.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3.clientapp.R;
import com.a3.clientapp.helper.adapter.CartItemRecyclerViewAdapter;
import com.a3.clientapp.helper.viewModel.ItemViewModel;
import com.a3.clientapp.model.Item;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


public class CartFragment extends Fragment {
    private static final String TAG = CartFragment.class.getSimpleName();

    private RecyclerView recyclerView;
    private List<Item> itemList;
    private CartItemRecyclerViewAdapter mAdapter;
    private TextView priceTxt;

    private ItemViewModel viewModel;


    public static CartFragment newInstance(String param1, String param2) {
        CartFragment fragment = new CartFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        itemList = new ArrayList<>(); //Reset value of item List

        recyclerView = view.findViewById(R.id.recycler_view);

        priceTxt = view.findViewById(R.id.priceTxt);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // view model
        viewModel = new ViewModelProvider(requireActivity()).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe(getViewLifecycleOwner(), itemList -> {

        // check null
            if (isAdded()){
                mAdapter = new CartItemRecyclerViewAdapter(getActivity(), itemList , viewModel);

                // linear styles
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
                linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setAdapter(mAdapter);
            }


        });

        // Create the observer which updates the UI.
        final Observer<Double> priceObserver = price -> {
            // Update the UI, in this case, a TextView.
            priceTxt.setText(round(price) + " $");
        };

        // get view model
        viewModel.getLiveTotalPrice().observe(requireActivity(), priceObserver);

    }

    // round up price
    public static double round(double d) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);return bd.doubleValue();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");

        onDestroy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

    }
}