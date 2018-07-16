import com.fasterxml.jackson.databind.util.JSONPObject
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.experimental.launch

const val CATS = "cats:get"
private const val QUERY_ALL = """select * from cats"""

class CatVerticle: CoroutineVerticle(){
    /**
     * Why are we putting one inside the class and the other outside it?
     *
     * QUERY_ALL is a short query and it fits on one line. We can allow ourselves to make it a constant.
     * On the other hand, QUERY_WITH_ID is a longer query and it requires some indentation.
     * Since we remove the indentation only at runtime, we can't make it a constant.
     * So, instead, we use a member value. In real-life projects, most of your queries will probably have to be private values.
     * But it's important to know the difference between the two approaches.
     */
    private val QUERY_WITH_ID = """select * from cats
                     where id = ?::integer""".trimIndent()

    override suspend fun start() {
        val db = getDbClient()
        /**
         * The type specifies which object we expect to receive our message. In this case, it's JsonObject.
         * Constant CATS is the key we subscribe for. It can be any string. By using a namespace,
         * we ensure that there won't be a collision in the future.
         */
        vertx.eventBus().consumer<JsonObject>(CATS) { req ->
            try {
                val body = req.body()
                val id: Int? = body["id"]
                val result = if (id != null) {
                    db.query(QUERY_WITH_ID, id)
                } else {
                    db.query(QUERY_ALL)
                }
                launch {
                    req.reply(result.await())
                }
            }
            catch (e: Exception) {
                req.fail(0, e.message)
            }
        }
    }
}