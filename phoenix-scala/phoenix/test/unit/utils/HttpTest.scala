package utils

import akka.http.scaladsl.model.StatusCodes
import core.failures.{Failures, GeneralFailure, NotFoundFailure404}
import phoenix.failures.CreditCardFailures.CustomerHasDefaultCreditCard
import phoenix.models.cord.Cart
import phoenix.utils.http.Http._
import testutils.TestBase

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
