package com.objectcomputing;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.StreamingHttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GithubControllerTest {

    private static Pattern MICRONAUT_RELEASE =
            Pattern.compile("[Micronaut|Micronaut Framework] [0-9].[0-9].[0-9]([0-9])?( (RC|M)[0-9])?");

    @Test
    void verifyGithubReleasesCanBeFetchedHttpClient() {

        //This test mocks an HTTP Server for GitHub with an extra Micronaut Embedded Server.
        // This allows you to test how your application behaves with a specific JSON response
        // or avoid issues such as rate limits which can make your tests flaky.
        EmbeddedServer github = ApplicationContext.run(EmbeddedServer.class,
                Collections.singletonMap("spec.name", "GithubControllerTest"));

        //Start up the application with the embadded HTTP server and override
        // the configuration to target the mock GitHub server
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer.class,
                Collections.singletonMap("micronaut.http.services.github.url",
                        "http://localhost:" + github.getPort()));

        //Create a streaming HTTP client
        StreamingHttpClient streamingClient = embeddedServer.getApplicationContext()
                .createBean(StreamingHttpClient.class, embeddedServer.getURL());
        HttpRequest<Object> request = HttpRequest.GET("/github/releases");
        Stream<GithubRelease> githubReleases = Flux.from(streamingClient.jsonStream(request, GithubRelease.class)).toStream();
        assertReleases(githubReleases);
        streamingClient.close();

        //Shutdown both servers
        embeddedServer.close();
        github.close();
    }

    private static void assertReleases(Stream<GithubRelease> releases) {
        assertTrue(releases
                .map(GithubRelease::getName)
                .allMatch(name -> MICRONAUT_RELEASE.matcher(name).find()));
    }

    //Mock controller for test - will only be instantiated for the mock GitHub context
    // (because requires `spec.name`)
    @Requires(property = "spec.name", value = "GithubControllerTest")
    @Controller
    static class GithubReleases {
        private final ResourceLoader resourceLoader;
        GithubReleases(ResourceLoader resourceLoader) {
            this.resourceLoader = resourceLoader;
        }

        @Get("/repos/micronaut-projects/micronaut-core/releases")
        Optional<InputStream> coreReleases() {
            return resourceLoader.getResourceAsStream("releases.json");
        }
    }
}
