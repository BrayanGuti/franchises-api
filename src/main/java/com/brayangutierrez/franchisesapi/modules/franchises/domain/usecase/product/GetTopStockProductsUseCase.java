package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.FranchiseInventory;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.TopStockProduct;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.gateways.InventoryRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Producto con más stock de cada sucursal de una franquicia.
 * <ul>
 * <li>Empates: se devuelven todos los productos con el stock máximo de la sucursal.</li>
 * <li>Sucursales sin productos: se omiten.</li>
 * <li>Orden estable: sucursal (nombre, ID) y luego producto (nombre, ID).</li>
 * </ul>
 */
public class GetTopStockProductsUseCase {

	private static final Comparator<TopStockProduct> STABLE_ORDER = Comparator
			.comparing((TopStockProduct top) -> top.branch().name())
			.thenComparing(top -> top.branch().id())
			.thenComparing(top -> top.product().name())
			.thenComparing(top -> top.product().id());

	private final InventoryRepository inventoryRepository;

	public GetTopStockProductsUseCase(InventoryRepository inventoryRepository) {
		this.inventoryRepository = inventoryRepository;
	}

	public Flux<TopStockProduct> execute(String franchiseId) {
		return inventoryRepository.findByFranchiseId(franchiseId)
				.switchIfEmpty(Mono.error(() -> new BusinessException(FranchiseError.FRANCHISE_NOT_FOUND, franchiseId)))
				.flatMapMany(GetTopStockProductsUseCase::topProductsPerBranch);
	}

	private static Flux<TopStockProduct> topProductsPerBranch(FranchiseInventory inventory) {
		Map<String, Branch> branchesById = inventory.branches().stream()
				.collect(Collectors.toMap(Branch::id, Function.identity()));
		return Flux.fromIterable(inventory.products())
				.filter(product -> branchesById.containsKey(product.branchId()))
				.groupBy(Product::branchId)
				.flatMap(branchProducts -> branchProducts.collectList()
						.flatMapIterable(GetTopStockProductsUseCase::withHighestStock)
						.map(product -> new TopStockProduct(branchesById.get(branchProducts.key()), product)))
				.sort(STABLE_ORDER);
	}

	/** Todos los productos con el stock máximo: más de uno si hay empate. */
	private static List<Product> withHighestStock(List<Product> products) {
		int highest = products.stream().mapToInt(Product::stock).max().orElseThrow();
		return products.stream().filter(product -> product.stock() == highest).toList();
	}

}
