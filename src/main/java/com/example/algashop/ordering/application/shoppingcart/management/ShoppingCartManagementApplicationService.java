package com.example.algashop.ordering.application.shoppingcart.management;

import com.example.algashop.ordering.domain.model.commons.Quantity;
import com.example.algashop.ordering.domain.model.customer.CustomerId;
import com.example.algashop.ordering.domain.model.product.Product;
import com.example.algashop.ordering.domain.model.product.ProductCatalogService;
import com.example.algashop.ordering.domain.model.product.ProductId;
import com.example.algashop.ordering.domain.model.product.ProductNotFoundException;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCart;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartId;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartItemId;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCartNotFoundException;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingCarts;
import com.example.algashop.ordering.domain.model.shoppingcart.ShoppingService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShoppingCartManagementApplicationService {

	private final ShoppingCarts shoppingCarts;
	private final ProductCatalogService productCatalogService;
	private final ShoppingService shoppingService;

	@Transactional
	public void addItem(@NonNull ShoppingCartItemInput input) {
		ShoppingCartId shoppingCartId = new ShoppingCartId(input.getShoppingCartId());
		ProductId productId = new ProductId(input.getProductId());

		ShoppingCart shoppingCart = shoppingCarts.ofId(shoppingCartId)
				.orElseThrow(ShoppingCartNotFoundException::new);

		Product product = productCatalogService.ofId(productId)
				.orElseThrow(ProductNotFoundException::new);

		shoppingCart.addItem(product, new Quantity(input.getQuantity()));

		shoppingCarts.add(shoppingCart);
	}

	@Transactional
	public UUID createNew(@NonNull UUID rawCustomerId) {
		ShoppingCart shoppingCart = shoppingService.startShopping(new CustomerId(rawCustomerId));
		shoppingCarts.add(shoppingCart);
		return shoppingCart.id().value();
	}

	@Transactional
	public void removeItem(@NonNull UUID rawShoppingCartId, @NonNull UUID rawShoppingCartItemId) {
		ShoppingCartId shoppingCartId = new ShoppingCartId(rawShoppingCartId);
		ShoppingCart shoppingCart = shoppingCarts.ofId(shoppingCartId)
				.orElseThrow(ShoppingCartNotFoundException::new);
		shoppingCart.removeItem(new ShoppingCartItemId(rawShoppingCartItemId));
		shoppingCarts.add(shoppingCart);
	}

	@Transactional
	public void empty(@NonNull UUID rawShoppingCartId) {
		ShoppingCartId shoppingCartId = new ShoppingCartId(rawShoppingCartId);
		ShoppingCart shoppingCart = shoppingCarts.ofId(shoppingCartId)
				.orElseThrow(ShoppingCartNotFoundException::new);
		shoppingCart.empty();
		shoppingCarts.add(shoppingCart);
	}

	@Transactional
	public void delete(@NonNull UUID rawShoppingCartId) {
		ShoppingCartId shoppingCartId = new ShoppingCartId(rawShoppingCartId);
		ShoppingCart shoppingCart = shoppingCarts.ofId(shoppingCartId)
				.orElseThrow(ShoppingCartNotFoundException::new);
		shoppingCarts.remove(shoppingCart);
	}
}
