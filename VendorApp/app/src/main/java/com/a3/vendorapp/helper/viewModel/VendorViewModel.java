package com.a3.vendorapp.helper.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.a3.vendorapp.model.Vendor;


public class VendorViewModel extends ViewModel {
    //attributes
    private final MutableLiveData<Vendor> vendorMutableLiveData = new MutableLiveData<>();

    // setvalue
    public void setValue(Vendor vendor){
        vendorMutableLiveData.setValue(vendor);
    }

    // get value
    public LiveData<Vendor> getSelectedVendor() {
        return vendorMutableLiveData;
    }

    // get value
    public Vendor getValue(){
        return vendorMutableLiveData.getValue();
    }
}
