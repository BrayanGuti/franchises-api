package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Cambia el nombre de un producto. El nombre nuevo debe seguir siendo único en la sucursal; eso lo
 * garantiza el repositorio en la misma escritura atómica que mueve la reserva del nombre.
 */
public class RenameProductUseCase {

	private final ProductRepository productRepository;

	public RenameProductUseCase(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Mono<Product> execute(String franchiseId, String branchId, String productId, String name) {
		return NameRules.normalize(name)
				.flatMap(validName -> productRepository.rename(franchiseId, branchId, productId, validName));
	}

}
