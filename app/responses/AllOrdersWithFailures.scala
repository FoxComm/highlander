package responses

import services.OrderUpdateFailure

final case class AllOrdersWithFailures(orders: Seq[AllOrders.Root], failures: Seq[OrderUpdateFailure])
