package utils

import models.Customers
import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.UpdateReturning._

class SlickTest extends IntegrationTestBase {
  import concurrent.ExecutionContext.Implicits.global

  "supports update with returning query" in {
    val customer = Customers.save(Factories.customer).run().futureValue
    val update = Customers.filter(_.id === 1).map(_.firstName).
      updateReturning(Customers.map(_.firstName), ("blah"))

    val firstName = db.run(update.headOption).futureValue.get
    firstName must === ("blah")
  }
}
