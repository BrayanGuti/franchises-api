package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;

/**
 * Reemplaza el stock de un producto por un valor absoluto.
 * <p>
 * Con valores absolutos, si dos peticiones cambian el stock a la vez gana la última escritura,
 * lo cual es aceptable porque cada una representa un conteo completo del inventario.
 */
public class UpdateProductStockUseCase {

	private final ProductRepository productRepository;

	public UpdateProductStockUseCase(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public Mono<Product> execute(String franchiseId, String branchId, String productId, int stock) {
		if (stock < 0) {
			return Mono.error(new BusinessException(FranchiseError.INVALID_STOCK, stock));
		}
		return productRepository.updateStock(franchiseId, branchId, productId, stock);
	}

}
