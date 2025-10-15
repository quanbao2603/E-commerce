package com.womtech.service;

import java.util.List;

import com.womtech.entity.Cart;
import com.womtech.entity.CartItem;
import com.womtech.entity.Product;
import com.womtech.entity.User;

public interface CartService extends BaseService<Cart, String> {

	void addToCart(User user, Product product, int quantity);

	Cart findByUser(User user);

	void updateQuantity(CartItem cartItem, int quantity);

	void clearCart(User user);

	void removeItem(String cartItemID);

	List<CartItem> findAllByUser(User user);

}