package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.brayangutierrez.franchisesapi.integrations.dynamodb.config.DynamoDbProperties;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.FranchiseInventory;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.gateways.InventoryRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.DynamoKeys;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch.BranchItem;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.branch.BranchItemMapper;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise.FranchiseItem;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.franchise.FranchiseItemMapper;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductItem;
import com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product.ProductItemMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Lee el inventario de una franquicia con UN solo {@code Query} por su partition key: trae la
 * franquicia ({@code META}), sus sucursales y sus productos. Las reservas de nombre se ignoran.
 * <ul>
 * <li>{@code ConsistentRead}: las lecturas de DynamoDB son eventualmente consistentes por defecto;
 * aquí se pide consistencia fuerte para que la respuesta incluya todo lo confirmado.</li>
 * <li>Paginación: DynamoDB devuelve como máximo 1 MB por página; el paginador las recorre todas.</li>
 * </ul>
 */
@Repository
public class InventoryDynamoRepository implements InventoryRepository {

	private static final TableSchema<FranchiseItem> FRANCHISE_SCHEMA = TableSchema.fromBean(FranchiseItem.class);
	private static final TableSchema<BranchItem> BRANCH_SCHEMA = TableSchema.fromBean(BranchItem.class);
	private static final TableSchema<ProductItem> PRODUCT_SCHEMA = TableSchema.fromBean(ProductItem.class);

	private final DynamoDbAsyncClient client;
	private final String tableName;

	public InventoryDynamoRepository(DynamoDbAsyncClient client, DynamoDbProperties properties) {
		this.client = client;
		this.tableName = properties.tableName();
	}

	@Override
	public Mono<FranchiseInventory> findByFranchiseId(String franchiseId) {
		QueryRequest request = QueryRequest.builder()
				.tableName(tableName)
				.keyConditionExpression("PK = :pk")
				.expressionAttributeValues(Map.of(":pk", AttributeValue.fromS(DynamoKeys.franchisePk(franchiseId))))
				.consistentRead(true)
				.build();
		return Flux.from(client.queryPaginator(request).items())
				.collect(InventoryBuilder::new, InventoryBuilder::add)
				.flatMap(builder -> Mono.justOrEmpty(builder.build()));
	}

	/** Reparte cada fila del Query según su tipo de entidad. */
	private static final class InventoryBuilder {

		private Franchise franchise;
		private final List<Branch> branches = new ArrayList<>();
		private final List<Product> products = new ArrayList<>();

		void add(Map<String, AttributeValue> item) {
			AttributeValue entityType = item.get("entityType");
			switch (entityType == null ? "" : entityType.s()) {
				case FranchiseItem.ENTITY_TYPE -> franchise = FranchiseItemMapper.toDomain(FRANCHISE_SCHEMA.mapToItem(item));
				case BranchItem.ENTITY_TYPE -> branches.add(BranchItemMapper.toDomain(BRANCH_SCHEMA.mapToItem(item)));
				case ProductItem.ENTITY_TYPE -> products.add(ProductItemMapper.toDomain(PRODUCT_SCHEMA.mapToItem(item)));
				default -> {
					// Reservas de nombre u otros ítems auxiliares: no forman parte del inventario.
				}
			}
		}

		/** Sin el ítem META la franquicia no existe, aunque quedaran otros ítems. */
		FranchiseInventory build() {
			return franchise == null ? null : new FranchiseInventory(franchise, branches, products);
		}

	}

}
