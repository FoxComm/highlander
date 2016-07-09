package utils

import akka.http.scaladsl.model.StatusCodes

import models.cord.Cart
import util.TestBase
import http.Http._
import failures.CreditCardFailures.CustomerHasDefaultCreditCard
import failures.{Failures, GeneralFailure, NotFoundFailure404}

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
