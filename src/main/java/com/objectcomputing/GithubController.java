package com.objectcomputing;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.reactivestreams.Publisher;

@Controller("/github")
public class GithubController {

    private final GithubClient githubClient;

    public GithubController(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

    @Get(uri = "/releases", produces = MediaType.APPLICATION_JSON_STREAM)
    Publisher<GithubRelease> fetchReleases() {
        return githubClient.fetchReleases();
    }
}
