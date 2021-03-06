package com.a3.vendorapp.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.a3.vendorapp.R;
import com.a3.vendorapp.helper.adapter.ClientAdapter;
import com.a3.vendorapp.helper.viewModel.VendorViewModel;
import com.a3.vendorapp.model.Client;
import com.a3.vendorapp.model.Vendor;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ClientsFragment extends Fragment {

    // attributes
    private static final String TAG = "UsersFragment";
    private RecyclerView recyclerView;

    private ClientAdapter clientAdapter;
    private List<Client> mClients;
    private List<Client> searchClientList;


    private FirebaseFirestore fireStore;
    private CollectionReference clientCollection;
    private final String CLIENT_COLLECTION = "clients";
    private VendorViewModel vendorViewModel;

    private Vendor currentVendor;
    EditText searchVendors;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_users, container, false);

        searchVendors = view.findViewById(R.id.search_vendors);
        searchVendors.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        recyclerView = view.findViewById(R.id.recycler_view_users);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mClients = new ArrayList<>();

        // init firestore
        fireStore = FirebaseFirestore.getInstance();
        clientCollection = fireStore.collection(CLIENT_COLLECTION);


        // set the current value
        if (isAdded()){
            vendorViewModel = new ViewModelProvider(requireActivity()).get(VendorViewModel.class);
            currentVendor = vendorViewModel.getValue();
        }


        loadClients();

        return view;
    }

    // search users
    private void searchUsers(String s) {

        // clear list
        mClients = new ArrayList<>();

        // if search is empty
        if (s.equals("")){
            // load all vendors again
            loadClients();
            return;
        }

        // iterate through the search list
        for (Client client: searchClientList
             ) {

            // check condition
            if (client.getUserName().toLowerCase().contains(s)){
                mClients.add(client);
            }
        }

        // set layout
        if (isAdded()){
            clientAdapter = new ClientAdapter(getContext(), mClients, currentVendor, false);
            recyclerView.setAdapter(clientAdapter);
        }


    }

    private void loadClients() {


        // load the vendor
        clientCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                // validate in the normal case without search
                if (searchVendors.getText().toString().equals("")){
                    mClients = new ArrayList<>();

                    // get the client lists
                    for (DocumentSnapshot ds: value
                    ){
                        mClients.add(ds.toObject(Client.class));
                    }

                    searchClientList = mClients;
                    Log.d(TAG, "mVendors: size" + mClients.size());

                    // set view
                    if (isAdded()){
                        clientAdapter = new ClientAdapter(getContext(), mClients, currentVendor, false);
                        recyclerView.setAdapter(clientAdapter);
                    }

                }

            }
        });
    }
}