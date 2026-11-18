package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;

/**
 * Elimina un producto de una sucursal. Su nombre queda libre para un producto nuevo.
 */
public class RemoveProductUseCase {

	private final ProductRepository productRepository;

	public RemoveProductUseCase(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Mono<Void> execute(String franchiseId, String branchId, String productId) {
		return productRepository.delete(franchiseId, branchId, productId);
	}

}
