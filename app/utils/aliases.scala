package utils

object aliases {
  type AC           = models.activity.ActivityContext
  type EC           = scala.concurrent.ExecutionContext
  type ES           = utils.ElasticsearchApi
  type DB           = slick.driver.PostgresDriver.api.Database
  type OC           = models.objects.ObjectContext
  type Mat          = akka.stream.Materializer
  type Json         = org.json4s.JsonAST.JValue
  type ActivityType = String

  object stripe {
    import com.stripe.model._
    type StripeCustomer = Customer
    type StripeCard     = Card
    type StripeCharge   = Charge
  }
}
