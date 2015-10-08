package utils

import akka.http.scaladsl.model.StatusCodes

import util.TestBase
import Http._
import services.{OrderNotFoundFailure, CustomerHasDefaultCreditCard, GeneralFailure, Result}

class HttpTest extends TestBase {

  "renderFailure" - {
    "returns a notFoundResponse if any failure is a NotFoundFailure" in {
      val failures = services.Failures(GeneralFailure("general"), CustomerHasDefaultCreditCard, OrderNotFoundFailure("ABC-123"))
      renderFailure(failures).status must === (StatusCodes.NotFound)
    }
  }
}
