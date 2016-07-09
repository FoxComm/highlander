package models.cord

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class Cord(id: Int = 0, referenceNumber: String, cartId: Int, orderId: Option[Int])
    extends FoxModel[Cord]

object Cord {
  def cordRefNumRegex = """([a-zA-Z0-9-_]*)""".r
}

class Cords(tag: Tag) extends FoxTable[Cord](tag, "cords") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cartId          = column[Int]("cart_id")
  def orderId         = column[Option[Int]]("order_id")

  def * = (id, referenceNumber, cartId, orderId) <> ((Cord.apply _).tupled, Cord.unapply)

  val cart  = foreignKey(Carts.tableName, cartId, Carts)(_.id)
  val order = foreignKey(Orders.tableName, orderId, Orders)(_.id.?)
}

object Cords
    extends FoxTableQuery[Cord, Cords](new Cords(_))
    with ReturningId[Cord, Cords]
    with SearchByRefNum[Cord, Cords] {

  def findOneByRefNum(refNum: String) =
    filter(_.referenceNumber === refNum).one

  val returningLens: Lens[Cord, Int] = lens[Cord].id
}
