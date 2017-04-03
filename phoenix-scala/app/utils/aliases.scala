package utils

object aliases {
  type AC           = models.activity.ActivityContext
  type EC           = scala.concurrent.ExecutionContext
  type DB           = slick.driver.PostgresDriver.api.Database
  type OC           = models.objects.ObjectContext
  type SL           = sourcecode.Line
  type SF           = sourcecode.File
  type Mat          = akka.stream.Materializer
  type Json         = org.json4s.JsonAST.JValue
  type ActivityType = String
  type AU           = services.Authenticator.AuthData[models.account.User]

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
  }
}
