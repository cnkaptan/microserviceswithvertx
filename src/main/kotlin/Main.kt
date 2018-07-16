import io.vertx.core.AsyncResult
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.launch

fun main(args: Array<String>) {
    val vertx = Vertx.vertx()

    // Router lets you specify handlers for different HTTP methods and URLs.
    val router = Router.router(vertx)

    router.get("/alive").asyncHandler {
        // Some response comes here
        // We now can use any suspending function in this context
       val json = json {
            obj("alive" to true)
        }
        it.respond(json.toString())
    }

    vertx.createHttpServer()
            .requestHandler(router::accept).listen(8080)

}

// But, by default, it doesn't support coroutines. Let's fix that by creating an extension function:
fun Route.asyncHandler(fn: suspend (RoutingContext) -> Unit){
    handler {ctx->
        launch(ctx.vertx().dispatcher()) {
               try {
                   fn(ctx)
               }catch (e: Exception){
                   ctx.fail(e)
               }
        }
    }
}

fun RoutingContext.respond(responseBody: String ="", status: Int = 200){
    this.response()
            .setStatusCode(status)
            .end(responseBody)
}