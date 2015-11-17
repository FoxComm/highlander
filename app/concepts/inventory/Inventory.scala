package concepts.inventory

import services.Result
import cats.data.Xor
import scala.concurrent.{ExecutionContext, Future}
import com.pellucid.sealerate
import utils.ADT

object Identifiable {
    type Id = Int
    type SkuId = Int
}
import Identifiable.Id
import Identifiable.SkuId

trait Identifiable { 
    def id() : Id
}

trait HasSkuId { 
    def skuId() : SkuId
}

object SingularUnit {
    type UnitType = String
}
import SingularUnit.UnitType

trait SingularUnit {
    def unit(): UnitType
}

object Locatable {
    type Location = String
}
import Locatable.Location

trait Locatable {
    def location() : Location
}

object HasShippingRestrictions { 
    type ShippingRestriction = String
    type ShippingRestrictions = Seq[ShippingRestriction]
}
import HasShippingRestrictions.ShippingRestrictions

trait HasShippingRestrictions { 
    def shippingRestrictions() : ShippingRestrictions
}

trait OnHand extends Identifiable with HasSkuId with SingularUnit {
    def onHand() : Int
}

trait Holdable extends Identifiable with HasSkuId with SingularUnit {
    def held(): Int
}

trait Reservable extends Identifiable with HasSkuId with SingularUnit {
    def reserved() : Int
}

trait NonSellable extends Identifiable with HasSkuId with SingularUnit {
    def nonSellable(): Int
}

trait Handy { 
    def findOnHandById(id: Id)(implicit ec: ExecutionContext) : Result[OnHand]
    def findOnHandBySkuId(id: SkuId)(implicit ec: ExecutionContext) : Result[OnHand]
}

trait Holdables {
    def findHoldableById(id: Id)(implicit ec: ExecutionContext) : Result[Holdable]
    def findHoldableBySkuId(id: SkuId)(implicit ec: ExecutionContext) : Result[Holdable]
}

trait Reservables { 
    def findReservableById(id: Id)(implicit ec: ExecutionContext) : Result[Reservable]
    def findReservableBySkuId(id: SkuId)(implicit ec: ExecutionContext) : Result[Reservable]
}

trait NonSellables { 
    def findNonSellableById(id: Id)(implicit ec: ExecutionContext) : Result[NonSellable]
    def findNonSellableBySkuId(id: SkuId)(implicit ec: ExecutionContext) : Result[NonSellable]
}

object Adjustments {
  sealed trait Adjustment
  final case class OnHand(id: Id, amount: Int) extends Adjustment
  final case class Held(id: Id, amount: Int) extends Adjustment
  final case class Reserved(id: Id, amount: Int) extends Adjustment
  final case class NonSellable(id: Id, amount: Int) extends Adjustment
  final case class And(left: Adjustment, right: Adjustment)

  type Ledger = Seq[Adjustment]
}

trait InventoryItem extends 
  OnHand with
  Holdable with 
  Reservable with
  NonSellable {

    def safetyStock(): Int

}

trait InventoryItems extends
  Handy with
  Reservables with
  Holdables with
  NonSellables { 
    def findInventoryItemById(id: Id)(implicit ec: ExecutionContext) : Result[InventoryItem]
    def findInventoryItemBySkuId(id: SkuId)(implicit ec: ExecutionContext) : Result[InventoryItem]
}

trait Warehouse extends
    Identifiable with
    Locatable with
    HasShippingRestrictions with
    InventoryItems {

      def apply(adjustments: Adjustments.Ledger)(implicit ec: ExecutionContext) : Result[Unit]

    }

trait WarehouseManagementSystem extends Identifiable { 
    def warehouses()(implicit ec: ExecutionContext) : Result[Seq[Warehouse]]
    def update(w: Warehouse)(implicit ec: ExecutionContext): Result[Unit]
}
