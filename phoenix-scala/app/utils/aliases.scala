package utils

import com.github.levkhomich.akka.tracing.TracingExtensionImpl

object aliases {
  type AC           = models.activity.ActivityContext
  type EC           = scala.concurrent.ExecutionContext
  type ES           = utils.ElasticsearchApi
  type DB           = slick.driver.PostgresDriver.api.Database
  type OC           = models.objects.ObjectContext
  type SL           = sourcecode.Line
  type SF           = sourcecode.File
  type Mat          = akka.stream.Materializer
  type Json         = org.json4s.JsonAST.JValue
  type ActivityType = String
  type AU           = services.Authenticator.AuthData[models.account.User]
  type TR           = utils.http.CustomDirectives.TracingRequest
  type TEI          = TracingExtensionImpl

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
  }
}
