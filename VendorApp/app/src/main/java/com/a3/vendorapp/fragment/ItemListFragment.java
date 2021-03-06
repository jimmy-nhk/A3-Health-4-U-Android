package com.a3.vendorapp.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3.vendorapp.R;
import com.a3.vendorapp.helper.adapter.ItemRecyclerViewAdapter;
import com.a3.vendorapp.model.Item;
import com.a3.vendorapp.model.Vendor;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ItemListFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<Item> itemList;
    private ItemRecyclerViewAdapter mAdapter;
    private static final String TAG = ItemListFragment.class.getSimpleName();
    private static final String ITEM_COLLECTION = "items";
    private FirebaseFirestore fireStore;
    private CollectionReference itemCollection;
    private Vendor vendor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {



        //view
        View view = inflater.inflate(R.layout.fragment_food_list, container, false);

        // get vendor
        assert getArguments() != null;
        vendor = getArguments().getParcelable("vendor"); // get vendor

        // init service
        initService(view);

        Log.d(TAG, "FoodListFragment: onCreateView");

        return view;
    }
    private void initService(View view) {
        // init fireStore db
        fireStore = FirebaseFirestore.getInstance();
        itemCollection = fireStore.collection(ITEM_COLLECTION);

        //Fetch item from server
        itemList = new ArrayList<>(); //Reset value of item List

        // load items
        itemCollection
                .whereEqualTo("vendorID" , vendor.getId() )
                .addSnapshotListener((value, error) -> {

            // clear to list
            itemList = new ArrayList<>();

            //reverse way (newest show first)
            for (int i = value.size() - 1 ; i >= 0; i--){

                itemList.add(value.getDocuments().get(i).toObject(Item.class));
            }

            // sort again
            itemList.sort((o1, o2) -> {
                // reverse sort
                if (o1.getId() < o2.getId()){
                    return 1; // normal will return -1
                } else if (o1.getId() > o2.getId()){
                    return -1; // reverse
                }
                return 0;
            });

            recyclerView = view.findViewById(R.id.recycler_view);

            // check activity null
            if (isAdded()){
                mAdapter = new ItemRecyclerViewAdapter(getActivity(), itemList);

                Log.d(TAG, "initService: itemList size: " + itemList.size());

                // linear styles
                LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
                linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                recyclerView.setLayoutManager(linearLayoutManager);
                recyclerView.setItemAnimator(new DefaultItemAnimator());
                recyclerView.setNestedScrollingEnabled(true);
                recyclerView.setAdapter(mAdapter);
            }

        });
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}