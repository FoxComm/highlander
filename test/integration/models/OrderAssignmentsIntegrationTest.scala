package models

import util.IntegrationTestBase
import utils.Seeds.Factories
import utils.Slick.implicits._

class OrderAssignmentsIntegrationTest extends IntegrationTestBase {

  import concurrent.ExecutionContext.Implicits.global

  "order assignees" - {

    "finds all admins assigned to order" in new Fixture {
      assign(admin1, order3)
      assign(admin2, order3)

      assign(admin2, order2)

      assigneesFor(order1) mustBe Seq.empty
      assigneesFor(order2) mustBe Seq(admin2)
      assigneesFor(order3) mustBe Seq(admin1, admin2)
    }

    "finds all orders assigned to admin" in new Fixture {
      assign(admin2, order1)
      assign(admin3, order1)
      assign(admin3, order2)

      assignedTo(admin1) mustBe Seq.empty
      assignedTo(admin2) mustBe Seq(order1)
      assignedTo(admin3) mustBe Seq(order1, order2)
    }

    trait Fixture {
      val (order1, order2, order3, admin1, admin2, admin3) = db.run(for {
        order1 ← Orders.save(Factories.order)
        order2 ← Orders.save(Factories.order.copy(id = 2, referenceNumber = "foo"))
        order3 ← Orders.save(Factories.order.copy(id = 3, referenceNumber = "bar"))
        admin1 ← StoreAdmins.save(Factories.storeAdmin)
        admin2 ← StoreAdmins.save(Factories.storeAdmin.copy(id = 2, email = "foo@foo.foo"))
        admin3 ← StoreAdmins.save(Factories.storeAdmin.copy(id = 3, email = "bar@bar.bar"))
      } yield (order1, order2, order3, admin1, admin2, admin3)).futureValue
    }
  }

  def assigneesFor(order: Order): Seq[StoreAdmin] = OrderAssignments.assigneesFor(order).run().futureValue

  def assignedTo(admin: StoreAdmin): Seq[Order] = OrderAssignments.assignedTo(admin).run.futureValue

  def assign(admin: StoreAdmin, order: Order) =
    OrderAssignments.save(OrderAssignment(orderId = order.id, assigneeId = admin.id)).run().futureValue
}
