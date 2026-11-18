package com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.gateways;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.FranchiseInventory;

import reactor.core.publisher.Mono;

/**
 * Lectura del inventario completo de una franquicia.
 */
public interface InventoryRepository {

	/**
	 * Lee la franquicia con todas sus sucursales y productos en una lectura fuertemente
	 * consistente: refleja todas las escrituras confirmadas antes de la consulta.
	 *
	 * @return el inventario, o vacío si la franquicia no existe
	 */
	Mono<FranchiseInventory> findByFranchiseId(String franchiseId);

}
