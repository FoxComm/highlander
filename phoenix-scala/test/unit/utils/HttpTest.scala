package utils

import akka.http.scaladsl.model.StatusCodes

import failures.CreditCardFailures.CustomerHasDefaultCreditCard
import failures.{Failures, GeneralFailure, NotFoundFailure404}
import models.cord.Cart
import testutils.TestBase
import utils.http.Http._

class HttpTest extends TestBase {

  "renderFailure" - {
    "returns a notFoundResponse if any failure is a NotFoundFailure" in {
      val failures = Failures(GeneralFailure("general"),
                              CustomerHasDefaultCreditCard,
                              NotFoundFailure404(Cart, "ABC-123")).value
      renderFailure(failures).status must === (StatusCodes.NotFound)
    }
  }
}
