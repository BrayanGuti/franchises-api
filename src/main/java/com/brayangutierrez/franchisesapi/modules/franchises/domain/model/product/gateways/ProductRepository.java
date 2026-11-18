package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

import reactor.core.publisher.Mono;

/**
 * Persistencia de productos. La implementación vive en la infraestructura.
 */
public interface ProductRepository {

	/**
	 * Guarda un producto nuevo en su sucursal, de forma atómica. Falla con
	 * {@code BRANCH_NOT_FOUND} si la sucursal no existe (sin dejar nada escrito) y con
	 * {@code PRODUCT_ALREADY_EXISTS} si la sucursal ya tiene un producto con el mismo nombre
	 * (sin distinguir mayúsculas, tildes ni espacios sobrantes).
	 */
	Mono<Product> save(Product product);

	/**
	 * Elimina un producto y libera su nombre, de forma atómica. Falla con
	 * {@code PRODUCT_NOT_FOUND} si el producto no existe en esa sucursal.
	 */
	Mono<Void> delete(String franchiseId, String branchId, String productId);

	/**
	 * Reemplaza el stock de un producto existente y devuelve el producto actualizado. Solo
	 * modifica el stock. Falla con {@code PRODUCT_NOT_FOUND} si el producto no existe, sin crearlo.
	 */
	Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock);

}
