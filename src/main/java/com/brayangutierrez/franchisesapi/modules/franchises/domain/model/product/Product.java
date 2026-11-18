package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product;

/**
 * Producto de una sucursal, con su cantidad en stock.
 */
public record Product(String id, String franchiseId, String branchId, String name, int stock) {
}
