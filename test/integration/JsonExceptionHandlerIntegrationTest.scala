import scala.collection.immutable
import akka.http.scaladsl.model.StatusCodes.ClientError
import akka.http.scaladsl.model.{IllegalRequestException, ErrorInfo, ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._

import util.IntegrationTestBase

class JsonExceptionHandlerIntegrationTest extends IntegrationTestBase with HttpSupport with AutomaticAuth {

  import Extensions._

  val illegalRequestExceptionText = "A test IllegalRequestException"
  val exceptionText = "A test exception"

  override protected def additionalRoutes = immutable.Seq(
    path("testThrowAnExcepton") {
      complete(throw new Exception(exceptionText))
    } ~
    path("testThrowAnIllegalRequestException") {
      complete(throw new IllegalRequestException(new ErrorInfo(illegalRequestExceptionText), StatusCodes.custom(400, "test")
        .asInstanceOf[ClientError]))
    }
  )

  "return a valid JSON exception on an IllegalRequestException" in {
    val response = GET("testThrowAnIllegalRequestException")

    response.status must ===(StatusCodes.BadRequest)
    response.entity.contentType must ===(ContentTypes.`application/json`)
    response.errors.head must startWith(illegalRequestExceptionText)
  }

  "return a valid JSON exception on an other exception" in {
    val response = GET("testThrowAnExcepton")

    response.status must ===(StatusCodes.InternalServerError)
    response.entity.contentType must ===(ContentTypes.`application/json`)
    response.errors.head must startWith(exceptionText)
  }
}

