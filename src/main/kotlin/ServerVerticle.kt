import com.sun.management.jmx.Trace.send
import io.netty.util.internal.SocketUtils.accept
import io.vertx.core.AsyncResult
import io.vertx.core.MultiMap
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.Router.router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.CoroutineVerticle

// We use multiline strings here, with | and trimMargin() to re-align them.
private val insert = """insert into cats (name, age)
            |values (?, ?::integer) RETURNING *""".trimMargin()

class ServerVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val router = router()
        vertx.createHttpServer().requestHandler(router::accept).listen(8080)
    }

    private fun router(): Router {
        val router = Router.router(vertx)
        val dbClient = getDbClient()

        router.route("/*").handler(BodyHandler.create())
        router.get("/alive").asyncHandler {
            val dbAlive = dbClient.query("select true as alive")
            // Some response comes here
            // We now can use any suspending function in this context
            val json = json {
                obj(
                        "alive" to true,
                        "db" to dbAlive.await()["rows"]
                )
            }
            it.respond(json.toString())
        }

        router.mountSubRouter("/api/v1", apiRouter())

        return router
    }

    private fun apiRouter(): Router {
        val router = Router.router(vertx)
        val db = getDbClient()
        router.post("/cats").asyncHandler { ctx ->
            // Some code of adding a cat comes here
            db.queryWithParams(insert, ctx.bodyAsJson.toCat(), {
                it.handle({
                    if (it.result().rows.size > 0) {
                        ctx.respond(it.result().rows[0].toString(), 201)
                    }
                    else {
                        ctx.respond(status=404)
                    }

                }, {
                    ctx.respond(status=500)
                })
            })
        }
        router.get("/cats").asyncHandler { ctx ->
            /**
             * Notice that we're reusing the same constant we defined earlier, called CATS.
             *
             * That way, we can easily check who can send this event and who consumes it.
             * If it's successful, we'll return a JSON. Otherwise, we'll return an HTTP error code.
             */
            send(CATS, ctx.queryParams().toJson()) {
                it.handle({
                    val responseBody = it.result().body()
                    ctx.respond(responseBody.get<JsonArray>("rows").toString())
                }, {
                    ctx.respond(status=500)
                })
            }
        }
        router.get("/cats/:id").asyncHandler { ctx ->
            // Fetches specific cat
        }
        return router
    }
}

/**
 * Notice that we have two different versions of this here. One refers to the inner scope of the apply() function.
 * The other refers to the outer scope of the toCat() function. To refer to outer scopes, we use the @scopeName notation.
 */
private fun JsonObject.toCat() = JsonArray().apply {
    add(this@toCat.getString("name"))
    add(this@toCat.getInteger("age"))
}


/**
 * What's left is only to call the cat from the ServerVerticle. For that, we'll add another method to our CoroutineVerticle:
 */

fun <T> CoroutineVerticle.send(address: String,
                               message: T,
                               callback: (AsyncResult<Message<T>>) -> Unit) {
    this.vertx.eventBus().send(address, message, callback)
}

/**
 * Another method that we add is toJson() on MultiMap. MultiMap is an object that holds our query parameters:
 */
private fun MultiMap.toJson(): JsonObject {
    val json = JsonObject()

    for (k in this.names()) {
        json.put(k, this[k])
    }

    return json
}