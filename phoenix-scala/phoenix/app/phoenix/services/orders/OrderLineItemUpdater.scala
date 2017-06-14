package phoenix.services.orders

import cats.implicits._
import core.db._
import phoenix.failures.OrderFailures.OrderLineItemNotFound
import phoenix.models.account.User
import phoenix.models.cord.Orders
import phoenix.models.cord.lineitems.OrderLineItems
import phoenix.payloads.LineItemPayloads.UpdateOrderLineItemsPayload
import phoenix.responses.cord.OrderResponse
import phoenix.utils.aliases.{AC, OC}
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._

object OrderLineItemUpdater {

  def updateOrderLineItems(
      admin: User,
      payload: Seq[UpdateOrderLineItemsPayload],
      refNum: String)(implicit ec: EC, apis: Apis, db: DB, ac: AC, ctx: OC): DbResultT[OrderResponse] =
    for {
      _             ← * <~ runOrderLineItemUpdates(payload)
      orderUpdated  ← * <~ Orders.mustFindByRefNum(refNum)
      orderResponse ← * <~ OrderResponse.fromOrder(orderUpdated, grouped = true)
    } yield orderResponse

  private def runOrderLineItemUpdates(
      payload: Seq[UpdateOrderLineItemsPayload])(implicit ec: EC, apis: Apis, db: DB, ac: AC, ctx: OC) =
    DbResultT.seqCollectFailures(payload.map { updatePayload ⇒
      for {
        orderLineItem ← * <~ OrderLineItems
                         .filter(_.referenceNumber === updatePayload.referenceNumber)
                         .mustFindOneOr(OrderLineItemNotFound(updatePayload.referenceNumber))
        patch = orderLineItem.copy(state = updatePayload.state, attributes = updatePayload.attributes)
        updatedItem ← * <~ OrderLineItems.update(orderLineItem, patch)
      } yield updatedItem
    }.toList)

}
