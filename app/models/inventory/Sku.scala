package models.inventory

import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.table.SearchByCode
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class Sku(id: Int = 0, code: String, name: Option[String] = None, isHazardous: Boolean = false, price: Int,
  isActive: Boolean = true)
  extends ModelWithIdParameter[Sku]

object Sku {
  val skuCodeRegex = """([a-zA-Z0-9-_]*)""".r
}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def code = column[String]("code")
  def name = column[Option[String]]("name")
  def isHazardous = column[Boolean]("is_hazardous")
  def price = column[Int]("price")
  def isActive = column[Boolean]("is_active")

  def * = (id, code, name, isHazardous, price, isActive) <> ((Sku.apply _).tupled, Sku.unapply)
}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id)
  )(new Skus(_))
  with SearchByCode[Sku, Skus] {

  val HARD_CODED_WAREHOUSE_ID = 1

  def findOneByCode(code: String): DBIO[Option[Sku]] = filter(_.code === code).one

  def isAvailableOnHand(id: Int)(implicit ec: ExecutionContext, db: Database): Rep[Boolean] = {
    InventorySummaries.findBySkuIdInWarehouse(HARD_CODED_WAREHOUSE_ID, id).filter(s => (s.onHand - s.reserved) > 0)
      .exists
  }

  def qtyAvailableForSkus(skus: Seq[String])(implicit ec: ExecutionContext, db: Database): DBIO[Map[Sku, Int]] = {
    (for {
      sku  ← Skus.filter(_.code inSet skus)
      summ ← InventorySummaries if summ.skuId === sku.id
    } yield (sku, summ.onHand)).result.map(_.toMap)
  }
}
