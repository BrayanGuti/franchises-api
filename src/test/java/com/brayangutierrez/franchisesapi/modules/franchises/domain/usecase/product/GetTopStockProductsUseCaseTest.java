package com.brayangutierrez.franchisesapi.modules.franchises.domain.usecase.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.brayangutierrez.franchisesapi.core.domain.exception.BusinessException;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.branch.Branch;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.exception.FranchiseError;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.franchise.Franchise;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.FranchiseInventory;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.TopStockProduct;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.inventory.gateways.InventoryRepository;
import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetTopStockProductsUseCaseTest {

	private static final String FRANCHISE_ID = "f-1";
	private static final Franchise FRANCHISE = new Franchise(FRANCHISE_ID, "Cafeterías");
	private static final Branch CENTRO = new Branch("b-centro", FRANCHISE_ID, "Centro");
	private static final Branch NORTE = new Branch("b-norte", FRANCHISE_ID, "Norte");
	private static final Branch SUR = new Branch("b-sur", FRANCHISE_ID, "Sur");

	@Mock
	private InventoryRepository inventoryRepository;

	@InjectMocks
	private GetTopStockProductsUseCase useCase;

	@Test
	void returnsTheProductWithMostStockOfEachBranch() {
		givenInventory(List.of(CENTRO, NORTE), List.of(
				product("p1", CENTRO, "Café", 10), product("p2", CENTRO, "Té", 50),
				product("p3", NORTE, "Pan", 30), product("p4", NORTE, "Leche", 5)));

		assertThat(topNames()).containsExactly("Centro:Té:50", "Norte:Pan:30");
	}

	@Test
	void returnsAllProductsTiedWithTheHighestStock() {
		givenInventory(List.of(CENTRO), List.of(
				product("p1", CENTRO, "Té", 50), product("p2", CENTRO, "Café", 50), product("p3", CENTRO, "Latte", 10)));

		assertThat(topNames()).containsExactly("Centro:Café:50", "Centro:Té:50");
	}

	@Test
	void omitsBranchesWithoutProducts() {
		givenInventory(List.of(CENTRO, SUR), List.of(product("p1", CENTRO, "Café", 3)));

		assertThat(topNames()).containsExactly("Centro:Café:3");
	}

	@Test
	void ordersByBranchNameEvenIfProductsArriveMixed() {
		givenInventory(List.of(SUR, CENTRO, NORTE), List.of(
				product("p1", SUR, "Agua", 1), product("p2", NORTE, "Pan", 2), product("p3", CENTRO, "Café", 3)));

		assertThat(topNames()).containsExactly("Centro:Café:3", "Norte:Pan:2", "Sur:Agua:1");
	}

	@Test
	void zeroStockProductsAreStillTheTopWhenThereIsNothingElse() {
		givenInventory(List.of(CENTRO), List.of(product("p1", CENTRO, "Agotado", 0)));

		assertThat(topNames()).containsExactly("Centro:Agotado:0");
	}

	@Test
	void franchiseWithoutProductsReturnsAnEmptyResult() {
		givenInventory(List.of(CENTRO), List.of());

		StepVerifier.create(useCase.execute(FRANCHISE_ID)).verifyComplete();
	}

	@Test
	void missingFranchiseFailsWithNotFound() {
		when(inventoryRepository.findByFranchiseId(FRANCHISE_ID)).thenReturn(Mono.empty());

		StepVerifier.create(useCase.execute(FRANCHISE_ID))
				.expectErrorSatisfies(error -> assertThat(((BusinessException) error).error())
						.isEqualTo(FranchiseError.FRANCHISE_NOT_FOUND))
				.verify();
	}

	private void givenInventory(List<Branch> branches, List<Product> products) {
		when(inventoryRepository.findByFranchiseId(FRANCHISE_ID))
				.thenReturn(Mono.just(new FranchiseInventory(FRANCHISE, branches, products)));
	}

	private List<String> topNames() {
		return useCase.execute(FRANCHISE_ID)
				.map(GetTopStockProductsUseCaseTest::describe)
				.collectList()
				.block();
	}

	private static String describe(TopStockProduct top) {
		return top.branch().name() + ":" + top.product().name() + ":" + top.product().stock();
	}

	private static Product product(String id, Branch branch, String name, int stock) {
		return new Product(id, FRANCHISE_ID, branch.id(), name, stock);
	}

}
