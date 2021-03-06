package com.a3.clientapp.activity;

import static androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.a3.clientapp.R;
import com.a3.clientapp.chat.MainChatActivity;
import com.a3.clientapp.fragment.CartFragment;
import com.a3.clientapp.fragment.HistoryFragment;
import com.a3.clientapp.fragment.HomeFragment;
import com.a3.clientapp.fragment.ItemListFragment;
import com.a3.clientapp.fragment.ProfileFragment;
import com.a3.clientapp.fragment.ReminderCallback;
import com.a3.clientapp.helper.broadcast.NotificationReceiver;
import com.a3.clientapp.helper.broadcast.NotificationService;
import com.a3.clientapp.helper.viewModel.CartViewModel;
import com.a3.clientapp.helper.viewModel.ItemViewModel;
import com.a3.clientapp.model.Cart;
import com.a3.clientapp.model.Client;
import com.a3.clientapp.model.Item;
import com.a3.clientapp.model.MessageObject;
import com.a3.clientapp.model.Order;
import com.a3.clientapp.model.Vendor;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements ReminderCallback{
    private final String ORDER_COLLECTION = "orders";
    private final String TAG = MainActivity.class.getSimpleName();
    private FragmentTransaction transactionFragment;

    // the item model list
    private ItemViewModel viewModel;
    private CartViewModel cartViewModel;

    private FirebaseFirestore fireStore;
    private CollectionReference orderCollection;
    private CollectionReference clientCollection;
    private final String CLIENT_COLLECTION = "clients";

    private BottomNavigationView bottomNavigationView;

    private int orderSize;
    private Client client;
    private String selectedCategory;
    private List<Cart> cartList;

    private CollectionReference messageCollection;
    private CollectionReference vendorCollection;
    private final String MESSAGE_COLLECTION = "messages";
    private final String VENDOR_COLLECTION = "vendors";


    public static final String CANCEL_NOTIFICATION = "Your order is cancelled!\nCheck it out!";
    public static final String ORDER_NOTIFICATION = "Successfully checked out!\nWaiting for processing!";
    public static final String PROCESS_NOTIFICATION = "Your order is successfully processed!\nCheck it out!";
    public static final String NEW_MESSAGE = "New Message is coming!";
    private Boolean isNotifying =false;
    private int interval=1;
    private NotificationReceiver notificationReceiver;
    private IntentFilter intentFilter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // request permission
        requestPermission();

        // init view model
        viewModel = new ViewModelProvider(this).get(ItemViewModel.class);
        viewModel.getSelectedItem().observe(this, item -> {
            // Perform an action with the latest item data
        });
        // selected category
        selectedCategory = "";

        cartViewModel = new ViewModelProvider(this).get(CartViewModel.class);

        // register service
        registerService();
        // bottom nav
        bottomNavigationView = findViewById(R.id.bottom_navigation_container);
        bottomNavigationView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // attaching bottom sheet behaviour - hide / show on scroll
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomNavigationView.getLayoutParams();
        layoutParams.setBehavior(new BottomNavigationBehavior());

        // add reminder data to fragment
        Bundle bundle = new Bundle();
        bundle.putBoolean("isNotifying", isNotifying);
        bundle.putInt("interval", 1);
        // set Fragmentclass Arguments
        HomeFragment homeFragment = new HomeFragment();
        homeFragment.setReminderCallback(this);
        homeFragment.setArguments(bundle);
        // init home fragment
        loadFragment(homeFragment);

        Intent intent = getIntent();
        if (intent != null) {
            //get current client
            client = intent.getParcelableExtra("client");
            Log.d(TAG, "onCreate: client=" + client);

            // to history fragment
            if (intent.getBooleanExtra("toHistory", false)){
                bottomNavigationView.setSelectedItemId(R.id.historyNav);
//                loadFragment(new HistoryFragment(client.getId()));
            }

            loadOrderList();

        }


        listenMessage();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE},

                1);

    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = item -> {

        switch (item.getItemId()) {
            case R.id.homePageNav:

                Bundle bundle = new Bundle();
                bundle.putBoolean("isNotifying", isNotifying);
                bundle.putInt("interval", interval);
                HomeFragment homeFragment = new HomeFragment();
                homeFragment.setReminderCallback(this);
                homeFragment.setArguments(bundle);
                loadFragment(homeFragment);
                return true;
            case R.id.itemsNav:
                ItemListFragment itemListFragment = new ItemListFragment();
                // Put item in bundle to send to ItemDetails fragment
                // send the string to ItemList itemListFragment
                try {
                    Bundle fragmentbundle = new Bundle();
                    fragmentbundle.putString("category", selectedCategory);
                    itemListFragment.setArguments(fragmentbundle);
                    loadFragment(itemListFragment);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            case R.id.cartNav:
                CartFragment cartFragment = new CartFragment();
                loadFragment(cartFragment);
                return true;
            case R.id.historyNav:
                HistoryFragment historyFragment = new HistoryFragment(client.getId());
                loadFragment(historyFragment);
                return true;
        }
        return false;
    };


    public void loadFragment(Fragment fragment) {
        try {
            FragmentManager fm = getSupportFragmentManager();
            Log.i(TAG, "Fragment stack size : " + fm.getBackStackEntryCount());

            for (int entry = 0; entry < fm.getBackStackEntryCount(); entry++) {
                Log.i(TAG, "Found fragment: " + fm.getBackStackEntryAt(entry).getId());
                fm.popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE);
                Log.i(TAG, "Pop successfully : " + fm.getBackStackEntryAt(entry).getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // load fragment
        transactionFragment = getSupportFragmentManager().beginTransaction();
        transactionFragment.replace(R.id.fragment_container, fragment);
//        transactionFragment.addToBackStack(null);
        transactionFragment.commit();
    }

    @Override
    protected void onStart() {
        toggleStatus("online");
        Log.d(TAG, TAG + "onStart");
        super.onStart();
    }

    // sign out btn
//    public void onSignOutBtnClick(View view) {
//        client.setStatus("offline");
//        clientCollection.document(client.getId() + "")
//                .set(client.toMap())
//                .addOnSuccessListener(unused -> {
//                    Log.d(TAG, "DocumentSnapshot successfully updated offline status! ");
//                    FirebaseAuth.getInstance().signOut();
//                    finish();
//                })
//                .addOnFailureListener(e -> Log.d(TAG, "DocumentSnapshot fail updated status!"));
//    }

    // load fragment with backstack
    public void loadFragmentWithBackStack(Fragment fragment) {
        try {
            FragmentManager fm = getSupportFragmentManager();
            Log.i(TAG, "Fragment stack size : " + fm.getBackStackEntryCount());

            for (int entry = 0; entry < fm.getBackStackEntryCount(); entry++) {
                Log.i(TAG, "Found fragment: " + fm.getBackStackEntryAt(entry).getId());
                fm.popBackStackImmediate(null, POP_BACK_STACK_INCLUSIVE);
                Log.i(TAG, "Pop successfully : " + fm.getBackStackEntryAt(entry).getId());

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        FragmentManager fm = getSupportFragmentManager();

        Log.i(TAG, "Fragment stack size : " + fm.getBackStackEntryCount());

        transactionFragment = getSupportFragmentManager().beginTransaction();
        transactionFragment.replace(R.id.fragment_container, fragment);
        transactionFragment.addToBackStack(null);
        transactionFragment.commit();
    }

    // toggle
    private void toggleStatus(String status) {
        client.setStatus(status);
        clientCollection.document(client.getId() + "")
                .set(client.toMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(@NonNull Void unused) {
                        Log.d(TAG, "DocumentSnapshot successfully updated status! " + status);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "DocumentSnapshot fail updated status!");

                    }
                });
    }

    @Override
    protected void onResume() {
        toggleStatus("online");
        super.onResume();
    }

    @Override
    protected void onDestroy() {
//        Toast.makeText(this, "Destroyed", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        try {
            FragmentManager fm = getSupportFragmentManager();

            Log.i(TAG, "Fragment stack size : " + fm.getBackStackEntryCount());


        } catch (Exception e) {

        }
//        // validate the back button in the device
//        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
//            finish();
//        } else {
        toggleStatus("offline");
        super.onBackPressed();
//        }
    }

    // on profile btn click
//    public void onProfileBtnClick(View view) {
//        Fragment fragment = new ProfileFragment();
//        if (client != null) {
//            Bundle bundle = new Bundle();
//            bundle.putParcelable("client", client);
//            Log.d(TAG, "onProfileBtnClick: client=" + client);
//            fragment.setArguments(bundle);
//        }
//        loadFragmentWithBackStack(fragment);
//    }


    // on chat btn click
    public void onChatBtnClick(View view) {
        Intent intent = new Intent(this, MainChatActivity.class);
        intent.putExtra("client", client);
        startActivity(intent);
    }

    // round up price
    public static double round(double d) {
        BigDecimal bd = new BigDecimal(d);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);return bd.doubleValue();
    }


    // order btn
    public void onOrderBtnClick(View view) {
        List<Item> cartList = viewModel.getListItem();

        // validate order size = 0
        try {
            if (cartList.size() == 0){
                return;
            }
        } catch (Exception e){
            return;
        }

        // sort the list
        Collections.sort(cartList);

        // create multimap and store the value of list
        Map<Integer, List<Item>>
                multimap = cartList
                .stream()
                .collect(
                        Collectors
                                .groupingBy(
                                        Item::getVendorID,
                                        Collectors
                                                .toList()));


        // init the necessary list
        List<Item> itemOrder = new ArrayList<>();
        List<Integer> quantity = new ArrayList<>();
        Order order;
        int occurrences;

        double price = 0;
        // iterate through the list of the cart
        for (List<Item> list : multimap.values()) {

            // iterate through the item in the list
            for (int i = 0; i < list.size(); i++) {
                // take the frequency
                occurrences = Collections.frequency(list, list.get(i));
                // add to the list
                itemOrder.add(list.get(i));
                quantity.add(occurrences);
                price += list.get(i).getPrice() * occurrences;

                // skip to occurrence
                i += occurrences - 1;

            }
            orderSize++;
            order = new Order(orderSize, filterDateOrder(LocalDateTime.now().toString()), false, itemOrder, quantity, list.get(0).getVendorID(), client.getId(), round(price));

            Log.d(TAG, "order: orderDATE: " + LocalDateTime.now().toString());
            Order finalOrder = order;
            orderCollection.document(order.getId() + "")
                    .set(order.toMap())
                    .addOnSuccessListener(unused -> {
                        Log.d(TAG, "Successfully added Order to FireStore: " + finalOrder.toString());

                        // reset the cart
                        viewModel.resetMutableItemList();

                        //TODO: add notification here (use broadcast)
                        Intent intent = new Intent(ORDER_NOTIFICATION);
                        intent.putExtra("client", client);
                        sendBroadcast(intent);

                    })
                    .addOnFailureListener(e -> Log.d(TAG, "Fail to add order to FireStore: " + finalOrder.toString()));


            // re-init the variables
            itemOrder = new ArrayList<>();
            quantity = new ArrayList<>();
            price = 0;
        }

    }

    private void registerService() {
        notificationReceiver = new NotificationReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(ORDER_NOTIFICATION);
        intentFilter.addAction(CANCEL_NOTIFICATION);
        intentFilter.addAction(PROCESS_NOTIFICATION);
        intentFilter.addAction(NEW_MESSAGE);
        this.registerReceiver(notificationReceiver, intentFilter);
    }



    // listen message
    private void listenMessage() {

        messageCollection = fireStore.collection(MESSAGE_COLLECTION);
        vendorCollection = fireStore.collection(VENDOR_COLLECTION);

        // listen for messages
        messageCollection
                .orderBy("id")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        // check not null

//                        for (DocumentSnapshot ds: value.getDocuments()
//                             ) {
//                            MessageObject messageObject = ds.toObject(MessageObject.class);
//                            Log.d(TAG, "message newest: " + messageObject.toString());
//
//                        }
                        try {

                            Log.d(TAG, "message size: " + value.getDocuments().size());
                            int size = value.getDocuments().size() - 1;

                            DocumentSnapshot ds = value.getDocuments().get(size);
                            if (ds != null) {
                                MessageObject messageObject = ds.toObject(MessageObject.class);
                                Log.d(TAG, "message newest: " + messageObject.toString());

                                try {
                                    if (messageObject.isNewestMessage()) {


                                            // get the vendor object
                                        vendorCollection.whereEqualTo(Vendor.VENDOR_USERNAME + "", messageObject.getSender() + "")
                                                .get()
                                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                                    @Override
                                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                        try {
                                                            Vendor vendor = queryDocumentSnapshots.getDocuments().get(0).toObject(Vendor.class);

                                                            // TODO: send notification here
                                                            Log.d(TAG, "New noti");
                                                            if (messageObject.getReceiver().equals(client.getUserName()) && messageObject.getSender().equals(vendor.getUserName()) ||
                                                                    messageObject.getReceiver().equals(vendor.getUserName()) && messageObject.getSender().equals(client.getUserName())) {

                                                                // create new intent
                                                                Intent intent = new Intent(NEW_MESSAGE);
                                                                intent.putExtra("message", messageObject.getMessage());
                                                                intent.putExtra("client", client);
                                                                intent.putExtra("vendor", vendor);
                                                                sendBroadcast(intent);
                                                            }
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                });


                                    }

                                    // validate the error
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                });
    }

    // filter the string date
    public String filterDateOrder(String rawString) {

        // initialize the new string
        char[] filterString = new char[rawString.length()];


        // iterate through each character in the string
        for (int i = 0; i < rawString.length(); i++) {

            // check if the character is T then replace it with T
            if (rawString.charAt(i) == 'T') {
                filterString[i] = ' ';
                continue;
            }

            // check if the character is :
            if (rawString.charAt(i) == '.') {
                return String.valueOf(filterString).trim();
            }

            filterString[i] = rawString.charAt(i);
        }

        return null;
    }


    // load order list
    private void loadOrderList() {
        // init fireStore db
        fireStore = FirebaseFirestore.getInstance();

        // setting to keep the fireStore fetching data without the internet
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        fireStore.setFirestoreSettings(settings);

        orderCollection = fireStore.collection(ORDER_COLLECTION);
        clientCollection = fireStore.collection(CLIENT_COLLECTION);

        // load items
        orderCollection.addSnapshotListener((value, error) -> {

            try {
                orderSize = value.size();

            } catch (Exception e) {
                orderSize = 1;
            }

        });

        cartList = new ArrayList<>(); //Reset value of cart List
        List<Order> orderList = new ArrayList<>();


        // load orders
        orderCollection
                .whereEqualTo("clientID", client.getId())
                .addSnapshotListener((value, error) -> {

                    if (error != null){
                        error.printStackTrace();
                        return;
                    }

                    Log.d(TAG, "loadCart: value.getDocumentChanges size: " + value.getDocumentChanges().size());

                    // loop through the document change
                    Order orderModified = null;
                    for (DocumentChange documentChange : value.getDocumentChanges()) {
                        if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                            orderModified = documentChange.getDocument().toObject(Order.class);
                            Log.d(TAG, "order changed: " + orderModified.toString());
                            break;
                        }
                    }

                    // clear to list
                    cartList = new ArrayList<>();
                    cartViewModel.resetMutableCartList();

                    // init necessary variable for doing logic
                    int countCart = 0;
                    Cart currentCart;
                    int countProcessed;
                    int countCancel;
                    boolean isModified;

                    assert value != null;
                    Log.d(TAG, "loadCart: value size: " + value.getDocuments().size());

                    //scan the value from db
                    for (int i = value.size() - 1; i >= 0; i--) {
                        orderList.add(value.getDocuments().get(i).toObject(Order.class));
                    }

                    // sort reverse way
                    orderList.sort((o1, o2) -> {
                        // reverse sort
                        if (o1.getId() < o2.getId()) {
                            return 1; // normal will return -1
                        } else if (o1.getId() > o2.getId()) {
                            return -1; // reverse
                        }
                        return 0;
                    });

                    Log.d(TAG, "loadCart: orderList.size(): " + orderList.size());

                    for (int i = 0; i < orderList.size(); i++) {

                        Log.d(TAG, "loadCart: order: " + orderList.get(i).toString());
                        Log.d(TAG, "loadCart: iTh: " + i);


                        String time = filterDate(orderList.get(i).getDate());
                        Log.d(TAG, "loadCart: time: " + time);

                        // take the list of order in the same time
                        List<Order> orderByDate = orderList.stream().filter(order -> {
//                            Log.d(TAG, "filter: " + order.getDate().trim().equals(time));
//                            Log.d(TAG, "filter: isProcessed: " + order.getIsProcessed());
//                            Log.d(TAG, "filter: object " + order.toString() + " orderList size: " + orderList.size());
                            return order.getDate().trim().equals(time);
                        }).collect(Collectors.toList());

                        Log.d(TAG, "loadCart: orderByDate size: " + orderByDate.size());


                        // create cart object
                        currentCart = new Cart(countCart, time, orderByDate);


                        // init count processed
                        countProcessed = 0;
                        countCancel = 0;
                        isModified = false;
                        for (Order order : orderByDate) {

                            // validate if the orderModified is null
                            try {
                                if (orderModified != null && order != null)
                                    if (orderModified.getId() == order.getId()) {
                                        isModified = true;
                                        Log.d(TAG, "loadCart: orderModified : " + orderModified.toString());
                                    }

                            } catch (Exception e) {
                                e.printStackTrace();
                                isModified = false;
                            }

                            // validate if the order is cancel
                            if (order.getIsCancelled()) {
                                countCancel++;
                                continue;
                            }

//                            Log.d(TAG, "filter-condition: isProcess: " + order.getIsProcessed());
                            // check if processed yet ?
                            if (order.getIsProcessed()) {
                                countProcessed++;
                            }
                        }
//                        Log.d(TAG, "loadCart: orderByDate size: " +orderByDate.size());
//                        Log.d(TAG, "loadCart: countProcessed : " +countProcessed);


                        // validate if the order is already processed.
                        if ((countProcessed + countCancel) == orderByDate.size()) {
                            currentCart.setIsFinished(true);
                        }

                        countCart++;

                        // validate if the order is changed
                        if (isModified) {

                            Log.d(TAG, "isModified: currentCart : " + currentCart.toString());
                            Log.d(TAG, "isModified: orderModified.getIsProcessed() : " + orderModified.getIsProcessed());
                            Log.d(TAG, "isModified: orderModified.getIsCancelled() : " + orderModified.getIsCancelled());

                            // add notification if the order is cancel
                            //        id = in.readInt();
                            //        date = in.readString();
                            //        orderList = in.createTypedArrayList(Order.CREATOR);
                            //        price = in.readDouble();
                            //        isFinished = in.readByte() != 0;
//                            Intent intent = new Intent(orderModified.getIsProcessed()? PROCESS_NOTIFICATION : orderModified.getIsCancelled()? CANCEL_NOTIFICATION : null);
//
//                            intent.putExtra("client", client);
//                            intent.putExtra("cart", currentCart);
//                            sendBroadcast(intent);

                            /** Using service*/
                            if (orderModified.isNewestOrder()){
                                if (orderModified.getIsProcessed() || orderModified.getIsCancelled()) {
                                    Intent intent = new Intent(this, NotificationService.class);
                                    intent.putExtra("message", orderModified.getIsProcessed() ? PROCESS_NOTIFICATION : CANCEL_NOTIFICATION);
                                    intent.putExtra("client", client);
                                    intent.putExtra("cart", currentCart);
                                    intent.setPackage(this.getPackageName());
                                    startService(intent);
                                }
                            }



                            isModified = false;
                        }


                        // add cart to cartList
                        cartList.add(currentCart);


                        i += orderByDate.size() - 1;


                    }


                    // reset the list
                    orderList.clear();
//                    Log.d(TAG, "loadCart: cardList size: " +cartList.size());


                    boolean successAddCart = cartViewModel.addListCarts(cartList);
                    Log.d(TAG, "loadCart: add successfully ? " + successAddCart);
                    Log.d(TAG, "loadCart: cartViewModel size:  " + cartViewModel.getListCart().size());

                });
    }

    // filter the string date
    public String filterDate(String rawString) {

        // initialize the new string
        char[] filterString = new char[rawString.length()];

        int countColon = 0;

        // iterate through each character in the string
        for (int i = 0; i < rawString.length(); i++) {


            // check if the character is :
            if (rawString.charAt(i) == ':') {
                countColon++;
                if (countColon == 2) {
                    filterString[i] = rawString.charAt(i);
                    filterString[i + 1] = rawString.charAt(i + 1);
                    filterString[i + 2] = rawString.charAt(i + 2);
                    return String.valueOf(filterString).trim();
                }

            }

            filterString[i] = rawString.charAt(i);
        }

        return null;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public void setSelectedCategory(String selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    @SuppressLint("NonConstantResourceId")
    public void onProfileBtnClick(View view) {
        try {
            PopupMenu popupMenu = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_profile, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.profileEditMenuItem:
                        handleProfileBtnClick();
                        break;
                    case R.id.signOutMenuItem:
                        handleSignOutBtnClick();
                        break;
                    default:
                        break;
                }

                return false;
            });
            popupMenu.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleProfileBtnClick() {
        Fragment fragment = new ProfileFragment();
        if (client != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable("client", client);
            Log.d(TAG, "onProfileBtnClick: client=" + client);
            fragment.setArguments(bundle);
        }
        loadFragmentWithBackStack(fragment);
    }

    private void handleSignOutBtnClick() {
        client.setStatus("offline");
        clientCollection.document(client.getId() + "")
                .set(client.toMap())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "DocumentSnapshot successfully updated offline status! ");
                    FirebaseAuth.getInstance().signOut();
                    finish();
                })
                .addOnFailureListener(e -> Log.d(TAG, "DocumentSnapshot fail updated status!"));
    }

    @Override
    public void onReceiveReminding(Boolean isReminding, int remindInterval) {

        isNotifying=isReminding;
        interval = remindInterval;
    }

}

class BottomNavigationBehavior extends CoordinatorLayout.Behavior<BottomNavigationView> {

    public BottomNavigationBehavior() {
        super();
    }

    public BottomNavigationBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, BottomNavigationView child, View dependency) {
        boolean dependsOn = dependency instanceof FrameLayout;
        return dependsOn;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, BottomNavigationView child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, BottomNavigationView child, View target, int dx, int dy, int[] consumed) {
        if (dy < 0) {
            showBottomNavigationView(child);
        } else if (dy > 0) {
            hideBottomNavigationView(child);
        }
    }

    private void hideBottomNavigationView(BottomNavigationView view) {
        view.animate().translationY(view.getHeight());
    }

    private void showBottomNavigationView(BottomNavigationView view) {
        view.animate().translationY(0);
    }
}