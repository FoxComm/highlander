package concepts.inventory

import services.Result
import cats.data.Xor
import scala.concurrent.{ExecutionContext, Future}
import com.pellucid.sealerate
import utils.ADT

object Identifiable {
    type Id = Int
}
import Identifiable.Id

trait Identifiable { 
    def id() : Id
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

trait OnHand extends Identifiable with SingularUnit {
    def onHand() : Int
}

trait Holdable extends Identifiable with SingularUnit {
    def held(): Int
}

trait Reservable extends Identifiable with SingularUnit {
    def reserved() : Int
}

trait NonSellable extends Identifiable with SingularUnit {
    def nonSellable(): Int
}

trait Handy { 
    def findOnHand(id: Identifiable.Id)(implicit ec: ExecutionContext) : Result[OnHand]
}

trait Holdables {
    def findHoldable(id: Id)(implicit ec: ExecutionContext) : Result[Holdable]
}

trait Reservables { 
    def findReservable(id: Id)(implicit ec: ExecutionContext) : Result[Reservable]
}

trait NonSellables { 
    def findNonSellable(id: Id)(implicit ec: ExecutionContext) : Result[NonSellable]
}

object Adjustments {
  sealed trait Adjustment
  case class OnHand(id: Id, amount: Int) extends Adjustment
  case class Held(id: Id, amount: Int) extends Adjustment
  case class Reserved(id: Id, amount: Int) extends Adjustment
  case class NonSellable(id: Id, amount: Int) extends Adjustment
  case class And(left: Adjustment, right: Adjustment)

  type Ledger = Seq[Adjustment]
}

trait InventoryItem extends 
  OnHand with
  Holdable with 
  Reservable with
  NonSellable {
}

trait InventoryItems extends
  Handy with
  Reservables with
  Holdables with
  NonSellables { 
    def findInventoryItem(id: Id)(implicit ec: ExecutionContext) : Result[InventoryItem]
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
