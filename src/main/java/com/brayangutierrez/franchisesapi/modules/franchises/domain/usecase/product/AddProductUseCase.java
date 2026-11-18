package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import java.util.UUID;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.common.NameRules;

import reactor.core.publisher.Mono;

/**
 * Agrega un producto a una sucursal existente, con un ID generado por el sistema.
 * <p>
 * La existencia de la sucursal y la unicidad del nombre las garantiza el repositorio en una
 * sola escritura atómica, así que dos peticiones simultáneas con el mismo nombre no pueden
 * crear dos productos.
 */
public class AddProductUseCase {

	private final ProductRepository productRepository;

	public AddProductUseCase(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Mono<Product> execute(String franchiseId, String branchId, String name, int stock) {
		if (stock < 0) {
			return Mono.error(new BusinessException(FranchiseError.INVALID_STOCK, stock));
		}
		return NameRules.normalize(name)
				.map(validName -> new Product(UUID.randomUUID().toString(), franchiseId, branchId, validName, stock))
				.flatMap(productRepository::save);
	}

}
