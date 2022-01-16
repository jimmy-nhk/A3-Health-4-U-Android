package com.a3.clientapp.helper.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.a3.clientapp.model.Client;

// client view model
public class ClientViewModel extends ViewModel {

    private final MutableLiveData<Client> clientMutableLiveData = new MutableLiveData<>();


    public void setValue(Client client){
        clientMutableLiveData.setValue(client);
    }

    public LiveData<Client> getSelectedClient() {
        return clientMutableLiveData;
    }

    public Client getValue(){
        return clientMutableLiveData.getValue();
    }
}
