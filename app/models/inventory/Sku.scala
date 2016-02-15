package models.inventory

import scala.concurrent.ExecutionContext

import com.pellucid.sealerate
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class Sku(id: Int = 0, sku: String, name: Option[String] = None, isHazardous: Boolean = false, price: Int,
  isActive: Boolean = true, `type`: Sku.Type = Sku.Sellable)
  extends ModelWithIdParameter[Sku]

object Sku {
  sealed trait Type
  case object Sellable extends Type
  case object Preorder extends Type
  case object Backorder extends Type
  case object NonSellable extends Type
  object Type extends ADT[Type] { def types = sealerate.values[Type] }
  implicit val typeColumnType: JdbcType[Type] with BaseTypedType[Type] = Type.slickColumn
}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def sku = column[String]("sku")
  def name = column[Option[String]]("name")
  def isHazardous = column[Boolean]("is_hazardous")
  def price = column[Int]("price")
  def isActive = column[Boolean]("is_active")
  def `type` = column[Sku.Type]("type")

  def * = (id, sku, name, isHazardous, price, isActive, `type`) <> ((Sku.apply _).tupled, Sku.unapply)
}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id)
)(new Skus(_)) {

  def isAvailableOnHand(id: Int)(implicit ec: ExecutionContext, db: Database): Rep[Boolean] = {
    //TODO: Use inventory system here
    val HARD_CODED_WAREHOUSE_ID = 1
    InventorySummaries.findBySkuId(HARD_CODED_WAREHOUSE_ID, id).filter(s => (s.onHand - s.reserved) > 0).exists
  }

  def qtyAvailableForSkus(skus: Seq[String])(implicit ec: ExecutionContext, db: Database): DBIO[Map[Sku, Int]] = {
    //TODO: Use inventory system here
    (for {
      sku  ← Skus.filter(_.sku inSet skus)
      summ ← InventorySummaries if summ.skuId === sku.id
    } yield (sku, summ.onHand)).result.map(_.toMap)
  }
}
