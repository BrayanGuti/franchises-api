package com.brayangutierrez.franchisesapi.modules.franchises.concurrency;

import java.time.Duration;
import java.util.List;
import java.util.function.IntFunction;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Lanza peticiones HTTP realmente simultáneas contra la aplicación levantada en un puerto real.
 * <p>
 * {@code WebTestClient.exchange()} espera cada respuesta antes de continuar; aquí se usa
 * {@link WebClient} reactivo con {@code flatMap(..., concurrencia)} para tener muchas peticiones en
 * vuelo a la vez. Cada petición devuelve solo su código de estado.
 */
final class ConcurrentClient {

	static final int CONCURRENCY = 50;
	private static final Duration TIMEOUT = Duration.ofSeconds(60);

	private final WebClient client;

	ConcurrentClient(int port) {
		this.client = WebClient.create("http://localhost:" + port);
	}

	/** Ejecuta {@code count} peticiones con hasta {@link #CONCURRENCY} en paralelo. */
	List<HttpStatusCode> run(int count, IntFunction<Mono<HttpStatusCode>> request) {
		return Flux.range(1, count)
				.flatMap(request::apply, CONCURRENCY)
				.collectList()
				.block(TIMEOUT);
	}

	/** Ejecuta varios grupos de peticiones a la vez, todos mezclados. */
	@SafeVarargs
	final List<HttpStatusCode> runTogether(Flux<HttpStatusCode>... groups) {
		return Flux.merge(CONCURRENCY, groups).collectList().block(TIMEOUT);
	}

	Mono<HttpStatusCode> post(String uri, String json) {
		return send(client.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(json));
	}

	Mono<HttpStatusCode> patch(String uri, String json) {
		return send(client.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(json));
	}

	Mono<HttpStatusCode> delete(String uri) {
		return send(client.delete().uri(uri));
	}

	private static Mono<HttpStatusCode> send(WebClient.RequestHeadersSpec<?> request) {
		return request.exchangeToMono(response -> response.releaseBody().thenReturn(response.statusCode()));
	}

}
