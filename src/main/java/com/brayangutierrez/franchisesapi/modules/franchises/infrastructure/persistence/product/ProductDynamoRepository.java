package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Repository;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.DynamoConditions;
import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.gateways.ProductRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;

import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ConditionCheck;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactPutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.TransactWriteItemsEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Implementación de {@link ProductRepository} sobre la tabla única de DynamoDB.
 */
@Repository
public class ProductDynamoRepository implements ProductRepository {

	/** Posición de cada operación dentro de la transacción de alta. */
	private static final int BRANCH_CHECK = 0;
	private static final int NAME_RESERVATION = 1;
	private static final int PRODUCT_PUT = 2;

	private final DynamoDbEnhancedAsyncClient enhancedClient;
	private final DynamoDbAsyncTable<ProductItem> products;
	private final DynamoDbAsyncTable<ProductNameItem> productNames;

	public ProductDynamoRepository(DynamoDbEnhancedAsyncClient enhancedClient, DynamoDbProperties properties) {
		this.enhancedClient = enhancedClient;
		this.products = enhancedClient.table(properties.tableName(), TableSchema.fromBean(ProductItem.class));
		this.productNames = enhancedClient.table(properties.tableName(), TableSchema.fromBean(ProductNameItem.class));
	}

	/**
	 * Una sola transacción: la sucursal debe existir, el nombre no debe estar reservado y el
	 * producto no debe existir. Si cualquiera falla, DynamoDB no aplica ninguna escritura: no
	 * quedan productos huérfanos ni reservas sueltas.
	 */
	@Override
	public Mono<Product> save(Product product) {
		TransactWriteItemsEnhancedRequest request = TransactWriteItemsEnhancedRequest.builder()
				.addConditionCheck(products, ConditionCheck.builder()
						.key(DynamoKeys.branchKey(product.franchiseId(), product.branchId()))
						.conditionExpression(DynamoConditions.ITEM_EXISTS)
						.build())
				.addPutItem(productNames, TransactPutItemEnhancedRequest.builder(ProductNameItem.class)
						.item(ProductItemMapper.toNameItem(product))
						.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
						.build())
				.addPutItem(products, TransactPutItemEnhancedRequest.builder(ProductItem.class)
						.item(ProductItemMapper.toItem(product))
						.conditionExpression(DynamoConditions.ITEM_NOT_EXISTS)
						.build())
				.build();
		return Mono.fromFuture(() -> enhancedClient.transactWriteItems(request))
				.thenReturn(product)
				.onErrorMap(TransactionCanceledException.class, e -> translate(e, product));
	}

	private static Throwable translate(TransactionCanceledException exception, Product product) {
		if (DynamoConditions.failedAt(exception, BRANCH_CHECK)) {
			return new BusinessException(FranchiseError.BRANCH_NOT_FOUND, product.branchId());
		}
		if (DynamoConditions.failedAt(exception, NAME_RESERVATION)) {
			return new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, product.name());
		}
		if (DynamoConditions.failedAt(exception, PRODUCT_PUT)) {
			return new BusinessException(FranchiseError.PRODUCT_ALREADY_EXISTS, product.id());
		}
		return exception;
	}

}
