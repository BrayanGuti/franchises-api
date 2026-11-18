package com.brayangutierrez.franchisesapi.infrastructure.drivenadapters.dynamodb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClientBuilder;

/**
 * Crea los clientes asíncronos (no bloqueantes) de DynamoDB que usan los adaptadores.
 * <p>
 * El destino no depende del nombre del perfil sino de si {@code app.dynamodb.endpoint} tiene valor:
 * <ul>
 * <li><b>Con endpoint</b> (perfiles {@code dev} y {@code test}): se conecta a DynamoDB Local en esa
 * URL con credenciales ficticias, que DynamoDB Local no valida.</li>
 * <li><b>Sin endpoint</b> (AWS): usa el endpoint regional y la cadena estándar de credenciales del
 * SDK (rol IAM de la tarea ECS, variables de entorno o perfil de {@code ~/.aws}). Nunca hay llaves
 * en el código ni en la configuración.</li>
 * </ul>
 * Cada adaptador crea su propia vista tipada de la tabla a partir del cliente enhanced, así que
 * agregar una entidad no requiere tocar esta clase.
 */
@Configuration
public class DynamoDbConfig {

	private static final AwsBasicCredentials LOCAL_CREDENTIALS = AwsBasicCredentials.create("local", "local");

	@Bean(destroyMethod = "close")
	DynamoDbAsyncClient dynamoDbAsyncClient(DynamoDbProperties properties) {
		DynamoDbAsyncClientBuilder builder = DynamoDbAsyncClient.builder()
				.region(Region.of(properties.region()))
				.httpClientBuilder(NettyNioAsyncHttpClient.builder());

		if (properties.endpoint() != null) {
			builder.endpointOverride(properties.endpoint())
					.credentialsProvider(StaticCredentialsProvider.create(LOCAL_CREDENTIALS));
		} else {
			builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
		}

		return builder.build();
	}

	/** Cliente de alto nivel: convierte automáticamente entre objetos Java e ítems de DynamoDB. */
	@Bean
	DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient(DynamoDbAsyncClient client) {
		return DynamoDbEnhancedAsyncClient.builder().dynamoDbClient(client).build();
	}

}
