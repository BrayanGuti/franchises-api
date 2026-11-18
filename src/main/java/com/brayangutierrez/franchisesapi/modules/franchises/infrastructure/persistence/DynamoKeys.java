package com.brayangutierrez.franchisesapi.modules.franchises.infrastructure.persistence;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import software.amazon.awssdk.enhanced.dynamodb.Key;

/**
 * Formato de las claves de la tabla única. Toda la información de una franquicia comparte
 * la misma partition key, así que una sola consulta trae sus sucursales y productos.
 *
 * <pre>
 * Franquicia:          PK = F#{franchiseId}   SK = META
 * Sucursal:            PK = F#{franchiseId}   SK = B#{branchId}
 * Producto:            PK = F#{franchiseId}   SK = B#{branchId}#P#{productId}
 * Reserva de nombre:   PK = F#{franchiseId}   SK = B#{branchId}#NAME#{nombre normalizado}
 * </pre>
 *
 * La reserva de nombre garantiza que una sucursal no tenga dos productos con el mismo nombre:
 * DynamoDB no tiene restricciones de unicidad, así que se crea en la misma transacción que el
 * producto con {@code attribute_not_exists}.
 */
public final class DynamoKeys {

	public static final String FRANCHISE_SK = "META";
	/** Prefijo de la sort key que comparten sucursales, productos y reservas de nombre. */
	public static final String BRANCH_PREFIX = "B#";

	private static final String FRANCHISE_PREFIX = "F#";
	private static final String PRODUCT_SEPARATOR = "#P#";
	private static final String PRODUCT_NAME_SEPARATOR = "#NAME#";
	private static final Pattern ACCENTS = Pattern.compile("\\p{M}");
	private static final Pattern SPACES = Pattern.compile("\\s+");

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

	/**
	 * Sort key de la reserva del nombre de un producto. El nombre se normaliza para que
	 * "Café", "cafe" y "  CAFÉ " ocupen la misma reserva.
	 */
	public static String productNameSk(String branchId, String productName) {
		return branchSk(branchId) + PRODUCT_NAME_SEPARATOR + normalizeName(productName);
	}

	public static Key franchiseKey(String franchiseId) {
		return key(franchisePk(franchiseId), FRANCHISE_SK);
	}

	public static Key branchKey(String franchiseId, String branchId) {
		return key(franchisePk(franchiseId), branchSk(branchId));
	}

	public static Key productKey(String franchiseId, String branchId, String productId) {
		return key(franchisePk(franchiseId), productSk(branchId, productId));
	}

	public static Key productNameKey(String franchiseId, String branchId, String productName) {
		return key(franchisePk(franchiseId), productNameSk(branchId, productName));
	}

	public static Key key(String pk, String sk) {
		return Key.builder().partitionValue(pk).sortValue(sk).build();
	}

	/** Extrae el ID quitando el prefijo de la clave, por ejemplo {@code F#123 -> 123}. */
	public static String franchiseIdFrom(String pk) {
		return pk.substring(FRANCHISE_PREFIX.length());
	}

	/** Extrae el ID de sucursal de su sort key: {@code B#456 -> 456}. */
	public static String branchIdFrom(String sk) {
		return sk.substring(BRANCH_PREFIX.length());
	}

	/** Extrae el ID de sucursal de la sort key de un producto: {@code B#456#P#789 -> 456}. */
	public static String branchIdFromProductSk(String productSk) {
		return productSk.substring(BRANCH_PREFIX.length(), productSk.indexOf(PRODUCT_SEPARATOR));
	}

	/** Extrae el ID de producto de su sort key: {@code B#456#P#789 -> 789}. */
	public static String productIdFrom(String productSk) {
		return productSk.substring(productSk.indexOf(PRODUCT_SEPARATOR) + PRODUCT_SEPARATOR.length());
	}

	/** Sin espacios en los extremos, espacios internos simples, sin tildes y en minúsculas. */
	private static String normalizeName(String name) {
		String withoutAccents = ACCENTS.matcher(Normalizer.normalize(name.strip(), Normalizer.Form.NFD)).replaceAll("");
		return SPACES.matcher(withoutAccents).replaceAll(" ").toLowerCase(Locale.ROOT);
	}

}
