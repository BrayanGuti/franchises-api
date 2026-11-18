package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.Key;

/**
 * Formato de las claves de la tabla única. Toda la información de una franquicia comparte
 * la misma partition key, así que una sola consulta trae sus sucursales y productos.
 *
 * <pre>
 * Franquicia: PK = F#{franchiseId}   SK = META
 * Sucursal:   PK = F#{franchiseId}   SK = B#{branchId}
 * Producto:   PK = F#{franchiseId}   SK = B#{branchId}#P#{productId}
 * </pre>
 */
public final class DynamoKeys {

	public static final String FRANCHISE_SK = "META";
	/** Prefijo de la sort key que comparten sucursales y productos. */
	public static final String BRANCH_PREFIX = "B#";

	private static final String FRANCHISE_PREFIX = "F#";
	private static final String PRODUCT_SEPARATOR = "#P#";

	private DynamoKeys() {
	}

	public static String franchisePk(String franchiseId) {
		return FRANCHISE_PREFIX + franchiseId;
	}

	public static String branchSk(String branchId) {
		return BRANCH_PREFIX + branchId;
	}

	public static String productSk(String branchId, String productId) {
		return branchSk(branchId) + PRODUCT_SEPARATOR + productId;
	}

	public static Key franchiseKey(String franchiseId) {
		return key(franchisePk(franchiseId), FRANCHISE_SK);
	}

	public static Key key(String pk, String sk) {
		return Key.builder().partitionValue(pk).sortValue(sk).build();
	}

	/** Extrae el ID quitando el prefijo de la clave, por ejemplo {@code F#123 -> 123}. */
	public static String franchiseIdFrom(String pk) {
		return pk.substring(FRANCHISE_PREFIX.length());
	}

}
