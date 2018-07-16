import com.sun.management.jmx.Trace.send
import io.netty.util.internal.SocketUtils.accept
import io.vertx.core.json.JsonArray
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle

class ServerVerticle: CoroutineVerticle() {

    override suspend fun start() {
        val router = router()
        vertx.createHttpServer().requestHandler(router::accept).listen(8080)
    }

    private fun router(): Router {
        val router = Router.router(vertx)

        router.route("/*").handler(BodyHandler.create())
        router.get("/alive").asyncHandler {
            // Some response comes here
            // We now can use any suspending function in this context
            val json = json {
                obj (
                        "alive" to true
                )
            }
            it.respond(json.toString())
        }

        router.mountSubRouter("/api/v1", apiRouter())

        return router
    }

    private fun apiRouter(): Router{
        val router = Router.router(vertx)
        router.post("/cats").asyncHandler { ctx ->
            // Some code of adding a cat comes here
        }
        router.get("/cats").asyncHandler { ctx ->
        }
        router.get("/cats/:id").asyncHandler { ctx ->
            // Fetches specific cat
        }
        return router
    }
}