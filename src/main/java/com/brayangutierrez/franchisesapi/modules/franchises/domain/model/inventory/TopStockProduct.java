package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

/**
 * Producto con más stock de una sucursal, junto con la sucursal a la que pertenece.
 */
public record TopStockProduct(Branch branch, Product product) {
}
