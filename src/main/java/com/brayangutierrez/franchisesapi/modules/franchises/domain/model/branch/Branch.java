package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch;

/**
 * Sucursal: pertenece a una franquicia y agrupa productos.
 */
public record Branch(String id, String franchiseId, String name) {
}
