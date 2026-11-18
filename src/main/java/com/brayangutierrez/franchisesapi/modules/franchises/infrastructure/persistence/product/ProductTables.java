package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import org.springframework.stereotype.Component;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/**
 * Vistas tipadas de la tabla única que comparten las operaciones de productos: la misma tabla
 * física, leída como producto o como reserva de nombre.
 */
@Component
class ProductTables {

	private final DynamoDbEnhancedAsyncClient client;
	private final DynamoDbAsyncTable<ProductItem> products;
	private final DynamoDbAsyncTable<ProductNameItem> productNames;

	ProductTables(DynamoDbEnhancedAsyncClient client, DynamoDbProperties properties) {
		this.client = client;
		this.products = client.table(properties.tableName(), TableSchema.fromBean(ProductItem.class));
		this.productNames = client.table(properties.tableName(), TableSchema.fromBean(ProductNameItem.class));
	}

	DynamoDbEnhancedAsyncClient client() {
		return client;
	}

	DynamoDbAsyncTable<ProductItem> products() {
		return products;
	}

	DynamoDbAsyncTable<ProductNameItem> productNames() {
		return productNames;
	}

}
