package responses

import services.Failure

final case class AllOrdersWithFailures(orders: Seq[AllOrders.Root], failures: Seq[Option[Failure]])
