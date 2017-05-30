package phoenix.utils

object aliases {
  type AC           = phoenix.models.activity.EnrichedActivityContext
  type EC           = scala.concurrent.ExecutionContext
  type DB           = core.db.DB
  type OC           = objectframework.models.ObjectContext
  type SL           = sourcecode.Line
  type SF           = sourcecode.File
  type Mat          = akka.stream.Materializer
  type Json         = org.json4s.JsonAST.JValue
  type CsvData      = List[(String, String)] // sequence of column name -> value
  type ActivityType = String
  type AU           = phoenix.services.Authenticator.AuthData[phoenix.models.account.User]

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
    type StripeToken    = Token
  }
}
