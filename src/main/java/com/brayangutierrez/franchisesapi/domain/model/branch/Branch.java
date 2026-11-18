package com.brayangutierrez.franchisesapi.domain.model.branch;

/**
 * Sucursal: pertenece a una franquicia y agrupa productos.
 */
public record Branch(String id, String franchiseId, String name) {
}
