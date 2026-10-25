package neoflix;

import java.lang.reflect.Type;
import java.util.*;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import neoflix.routes.*;

import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.path;

public class NeoflixApp {

    public static void main(String[] args) {
        AppUtils.loadProperties();

        // tag::driver[]
        var driver = AppUtils.initDriver();
        // end::driver[]

        var jwtSecret = AppUtils.getJwtSecret();
        var port = AppUtils.getServerPort();

        var gson = GsonUtils.gson();
        var server = Javalin
            .create(config -> {
                config.staticFiles.add("/", Location.CLASSPATH);
                config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = "/public";
                    staticFiles.location = Location.CLASSPATH;
                });
                var jsonMapper = new JsonMapper() {
                    @Override
                    public String toJsonString(Object obj, Type type) {
                        return gson.toJson(obj);
                    }

                    @Override
                    public <T> T fromJsonString(String json, Type targetType) {
                        return gson.fromJson(json, targetType);
                    }
                };
                config.jsonMapper(jsonMapper);
                config.routes.apiBuilder(() -> {
                    before(ctx -> AppUtils.handleAuthAndSetUser(ctx.req(), jwtSecret));
                    path("/api", () -> {
                        path("/movies", new MovieRoutes(driver));
                        path("/genres", new GenreRoutes(driver));
                        path("/auth", new AuthRoutes(driver, jwtSecret));
                        path("/account", new AccountRoutes(driver));
                        path("/people", new PeopleRoutes(driver));
                    });
                });
                config.routes.exception(ValidationException.class, (exception, ctx) -> {
                    var body = Map.of("message", exception.getMessage(), "details", exception.getDetails());
                    ctx.status(422).contentType("application/json").result(gson.toJson(body));
                });
            })
            .start(port);
        System.out.printf("Server listening on http://localhost:%d/%n", port);
    }
}