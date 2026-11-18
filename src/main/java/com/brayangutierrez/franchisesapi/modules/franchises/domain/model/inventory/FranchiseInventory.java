package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory;

import java.util.List;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

/**
 * Foto completa de una franquicia leída de una vez: sus sucursales y los productos de cada una.
 */
public record FranchiseInventory(Franchise franchise, List<Branch> branches, List<Product> products) {

	public FranchiseInventory {
		branches = List.copyOf(branches);
		products = List.copyOf(products);
	}

}
