import scala.collection.immutable
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._

import util.IntegrationTestBase

class JsonExceptionHandlerIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  import Extensions._

  val exceptionText = "A test exception"

  override protected def additionalRoutes = immutable.Seq(
    path("testThrowAnExcepton") {
      complete(throw new Exception(exceptionText))
    }
  )

  "return a valid JSON exception on an exception" in {
    val response = GET("testThrowAnExcepton")

    response.status must ===(StatusCodes.InternalServerError)
    response.entity.contentType must ===(ContentTypes.`application/json`)
    response.errors.head must startWith(exceptionText)
  }
}

