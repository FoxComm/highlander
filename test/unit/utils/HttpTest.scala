package utils

import akka.http.scaladsl.model.StatusCodes

import models.Order
import util.TestBase
import Http._
import services.{NotFoundFailure404, CustomerHasDefaultCreditCard, GeneralFailure}

class HttpTest extends TestBase {

  "renderFailure" - {
    "returns a notFoundResponse if any failure is a NotFoundFailure" in {
      val failures = services.Failures(GeneralFailure("general"), CustomerHasDefaultCreditCard, NotFoundFailure404(Order, "ABC-123"))
      renderFailure(failures).status must === (StatusCodes.NotFound)
    }
  }
}
