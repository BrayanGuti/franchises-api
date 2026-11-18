package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Repository;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;

import reactor.core.publisher.Mono;

/**
 * Implementación de {@link ProductRepository} sobre la tabla única de DynamoDB. Cada operación
 * vive en su propia clase; este repositorio solo las expone a través del gateway del dominio.
 */
@Repository
public class ProductDynamoRepository implements ProductRepository {

	private final AddProductOperation addProduct;
	private final RemoveProductOperation removeProduct;
	private final UpdateProductStockOperation updateStock;
	private final RenameProductOperation renameProduct;

	ProductDynamoRepository(AddProductOperation addProduct, RemoveProductOperation removeProduct,
			UpdateProductStockOperation updateStock, RenameProductOperation renameProduct) {
		this.addProduct = addProduct;
		this.removeProduct = removeProduct;
		this.updateStock = updateStock;
		this.renameProduct = renameProduct;
	}

	@Override
	public Mono<Product> save(Product product) {
		return addProduct.execute(product);
	}

	@Override
	public Mono<Void> delete(String franchiseId, String branchId, String productId) {
		return removeProduct.execute(franchiseId, branchId, productId);
	}

	@Override
	public Mono<Product> updateStock(String franchiseId, String branchId, String productId, int stock) {
		return updateStock.execute(franchiseId, branchId, productId, stock);
	}

	@Override
	public Mono<Product> rename(String franchiseId, String branchId, String productId, String name) {
		return renameProduct.execute(franchiseId, branchId, productId, name);
	}

}
