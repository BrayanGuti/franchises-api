package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.brayangutierrez.franchisesapi.modules.franchises.domain.model.product.Product;

class ProductItemMapperTest {

	private final Product product = new Product("p1", "f1", "b1", "Café Americano", 25);

	@Test
	void mapsProductToItemInsideItsBranch() {
		ProductItem item = ProductItemMapper.toItem(product);

		assertThat(item.getPk()).isEqualTo("F#f1");
		assertThat(item.getSk()).isEqualTo("B#b1#P#p1");
		assertThat(item.getEntityType()).isEqualTo(ProductItem.ENTITY_TYPE);
		assertThat(item.getName()).isEqualTo("Café Americano");
		assertThat(item.getStock()).isEqualTo(25);
	}

	@Test
	void nameReservationPointsToTheProductAndUsesTheNormalizedName() {
		ProductNameItem item = ProductItemMapper.toNameItem(product);

		assertThat(item.getPk()).isEqualTo("F#f1");
		assertThat(item.getSk()).isEqualTo("B#b1#NAME#cafe americano");
		assertThat(item.getEntityType()).isEqualTo(ProductNameItem.ENTITY_TYPE);
		assertThat(item.getProductId()).isEqualTo("p1");
	}

	@Test
	void mapsItemBackToTheSameProduct() {
		assertThat(ProductItemMapper.toDomain(ProductItemMapper.toItem(product))).isEqualTo(product);
	}

}
