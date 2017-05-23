package phoenix.utils

object aliases {
  type AC           = phoenix.models.activity.ActivityContext
  type EC           = scala.concurrent.ExecutionContext
  type DB           = utils.db.DB
  type OC           = models.objects.ObjectContext
  type SL           = sourcecode.Line
  type SF           = sourcecode.File
  type Mat          = akka.stream.Materializer
  type Json         = org.json4s.JsonAST.JValue
  type ActivityType = String
  type AU           = phoenix.services.Authenticator.AuthData[phoenix.models.account.User]

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
  }
}
