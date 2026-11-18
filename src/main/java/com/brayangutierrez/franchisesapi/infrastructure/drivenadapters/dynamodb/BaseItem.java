package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Atributos comunes a todos los ítems de la tabla única: la clave compuesta ({@code PK} +
 * {@code SK}, ver {@link DynamoKeys}) y el tipo de entidad. Cada entidad extiende esta clase
 * con sus propios atributos (por ejemplo, solo los productos tienen {@code stock}).
 */
public abstract class BaseItem {

	private String pk;
	private String sk;
	private String entityType;

	@DynamoDbPartitionKey
	@DynamoDbAttribute("PK")
	public String getPk() {
		return pk;
	}

	public void setPk(String pk) {
		this.pk = pk;
	}

	@DynamoDbSortKey
	@DynamoDbAttribute("SK")
	public String getSk() {
		return sk;
	}

	public void setSk(String sk) {
		this.sk = sk;
	}

	/** Distingue el tipo de ítem cuando una consulta trae entidades mezcladas. */
	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

}
