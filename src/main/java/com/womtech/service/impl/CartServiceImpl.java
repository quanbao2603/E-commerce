package com.womtech.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import com.womtech.entity.Cart;
import com.womtech.entity.CartItem;
import com.womtech.entity.Product;
import com.womtech.entity.User;
import com.womtech.repository.CartItemRepository;
import com.womtech.repository.CartRepository;
import com.womtech.service.CartService;

@Service
public class CartServiceImpl extends BaseServiceImpl<Cart, String> implements CartService {
	@Autowired
	CartRepository cartRepository;
	@Autowired
	CartItemRepository cartItemRepository;
	
	public CartServiceImpl(JpaRepository<Cart, String> repo) {
		super(repo);
	}

	@Override
	public Cart findByUser(User user) {
		return cartRepository.findByUser(user).orElseGet(() -> {
			Cart cart = Cart.builder()
							.user(user)
							.createAt(LocalDateTime.now())
							.build();
			return cartRepository.save(cart);
		});
	}
	
    @Override
	public void addToCart(User user, Product product, int quantity) {
    	Cart cart = findByUser(user);
		
		CartItem item;
        Optional<CartItem> existingItem = cartItemRepository.findByCartAndProduct(cart, product);
        if (existingItem.isEmpty()) {
            item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
        } else {
            item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        }
        cartItemRepository.save(item);
    }
    
    @Override
	public void removeItem(String cartItemID) {
        cartItemRepository.deleteById(cartItemID);
    }
    
    @Override
	public void clearCart(User user) {
    	cartItemRepository.deleteByCart(findByUser(user));
    }
    
    @Override
	public void updateQuantity(String cartItemID, int quantity) {
    	Optional<CartItem> cartItemOpt = cartItemRepository.findById(cartItemID);
    	if (cartItemOpt.isEmpty())
    		return;
    	
    	CartItem cartItem = cartItemOpt.get();
    	if (quantity <= 0) {
    		cartItemRepository.delete(cartItem);
    	} else {
    		cartItem.setQuantity(quantity);
    		cartItemRepository.save(cartItem);
    	}
    }
    
    @Override
	public BigDecimal totalPrice(Cart cart) {
    	BigDecimal total = BigDecimal.ZERO;
    	List<CartItem> items = cartItemRepository.findByCart(cart);
        if (items.isEmpty()) {
            return total;
        }
        
        for (CartItem item : items) {
        	BigDecimal itemTotal = item.getProduct().getPrice()
        							   .multiply(BigDecimal.valueOf(item.getQuantity()));
        	item.setItemTotal(itemTotal);
        	
			total = total.add(itemTotal);
        }
        return total;
    }
    
    @Override
	public int totalQuantity(Cart cart) {
    	int total = 0;
    	List<CartItem> items = cartItemRepository.findByCart(cart);
        if (items.isEmpty()) {
            return total;
        }
        
        for (CartItem item : items) {
			total += item.getQuantity();
        }
        return total;
    }
}