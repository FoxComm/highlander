package models

import slick.driver.PostgresDriver.api._
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.seeds.Seeds
import Seeds.Factories
import utils.Slick.implicits._

class OrderAssignmentsIntegrationTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "order assignees" - {

    "finds all admins assigned to order" in new Fixture {
      assign(admin1, order3)
      assign(admin2, order3)

      assign(admin2, order2)

      assigneesFor(order1) mustBe empty
      assigneesFor(order2) must === (Seq(admin2))
      assigneesFor(order3) must === (Seq(admin1, admin2))
    }

    "finds all orders assigned to admin" in new Fixture {
      assign(admin2, order1)
      assign(admin3, order1)
      assign(admin3, order2)

      assignedTo(admin1) mustBe empty
      assignedTo(admin2) must === (Seq(order1))
      assignedTo(admin3) must === (Seq(order1, order2))
    }

    trait Fixture {
      val (order1, order2, order3, admin1, admin2, admin3) = (for {
        order1 ← * <~ Orders.create(Factories.order)
        order2 ← * <~ Orders.create(Factories.order.copy(id = 2, referenceNumber = "foo"))
        order3 ← * <~ Orders.create(Factories.order.copy(id = 3, referenceNumber = "bar"))
        admin1 ← * <~ StoreAdmins.create(Factories.storeAdmin)
        admin2 ← * <~ StoreAdmins.create(Factories.storeAdmin.copy(id = 2, email = "foo@foo.foo"))
        admin3 ← * <~ StoreAdmins.create(Factories.storeAdmin.copy(id = 3, email = "bar@bar.bar"))
      } yield (order1, order2, order3, admin1, admin2, admin3)).runT().futureValue.rightVal
    }
  }

  def assigneesFor(order: Order): Seq[StoreAdmin] = OrderAssignments.assigneesFor(order).result.run().futureValue

  def assignedTo(admin: StoreAdmin): Seq[Order] = OrderAssignments.assignedTo(admin).result.run.futureValue

  def assign(admin: StoreAdmin, order: Order) =
    OrderAssignments.create(OrderAssignment(orderId = order.id, assigneeId = admin.id)).run().futureValue.rightVal
}
