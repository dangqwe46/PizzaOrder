package com.marty.yummy.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.marty.yummy.dbutilities.AppDatabase;
import com.marty.yummy.model.CartItem;
import com.marty.yummy.utility.ObservableObject;

import java.util.List;

import static com.marty.yummy.ui.HomeScreenActivity.INTENT_UPDATE_LIST;

public class CartViewModel extends AndroidViewModel {

    private AppDatabase db;
    private Double totalCost=0.0,discount=0.0,deliveryCost=0.0;
    private MutableLiveData<Double> grandTotal = new MutableLiveData<>();
    private MediatorLiveData<List<CartItem>> mediatorLiveData = new MediatorLiveData<>();
    private String couponApplied="";
    private MutableLiveData<String> errorString = new MutableLiveData<>();

    public CartViewModel(@NonNull Application application) {
        super(application);
        init();
    }

    private void init() {
        db = AppDatabase.getDatabase(getApplication().getApplicationContext());
        subscribeToCartChanges();
    }

    private void subscribeToCartChanges() {
        LiveData<List<CartItem>> cartItemsLiveData = db.cartItemDao().getCartItems();
        mediatorLiveData.addSource(cartItemsLiveData, new Observer<List<CartItem>>() {
            @Override
            public void onChanged(@Nullable List<CartItem> cartItems) {
                mediatorLiveData.setValue(cartItems);
                calculateGrandTotalCost();
            }
        });
    }

    private void calculateGrandTotalCost() {
        List<CartItem> cartItemList = mediatorLiveData.getValue();
        totalCost = 0.0;
        if(cartItemList!=null) {
            for (CartItem cartItem : cartItemList) {
                totalCost = totalCost+(cartItem.getPrice()*cartItem.getQuantity());
            }
            discount = calculateDiscount(couponApplied);
            deliveryCost = calculateDeliveryCost(couponApplied);
            grandTotal.setValue(totalCost - discount + deliveryCost);
        }
    }

    private Double calculateDeliveryCost(String couponApplied) {
        if(couponApplied.equals("FREESHIP") && totalCost>100000){
            errorString.setValue("");
            return 0.0;
        }else if(couponApplied.equals("FREESHIP")) {
            errorString.setValue("Đơn hàng của bạn phải lớn hơn > 100.000 VNĐ");
        }
        return 15000.0;
    }

    private Double calculateDiscount(String couponApplied) {
        if(couponApplied.equals("THAYCANHDEPTRAI") && totalCost>400000){
            errorString.setValue("");
            return (totalCost*20)/100;
        }else if(couponApplied.equals("THAYCANHDEPTRAI")){
            errorString.setValue("Đơn hàng của bạn phải lớn hơn > 400.000 VNĐ");
        }
        return 0.0;
    }

    public Double getDiscountAmt(){
        return discount;
    }

    public Double getDeliveryCost(){
        return deliveryCost;
    }

    public Double getTotalCost(){
        return totalCost;
    }

    public MutableLiveData<Double> getGrandTotal(){
        return grandTotal;
    }


    public void applyCoupon(String coupon) {
        couponApplied = coupon;
        calculateGrandTotalCost();
    }

    public MediatorLiveData<List<CartItem>> getCartItemsLiveData() {
        return mediatorLiveData;
    }

    public void removeItem(String name){
        db.cartItemDao().deleteCartItem(name);
        ObservableObject.getInstance().updateValue(new Intent(INTENT_UPDATE_LIST));
    }

    public MutableLiveData<String> getErrorString(){
        return errorString;
    }
}
