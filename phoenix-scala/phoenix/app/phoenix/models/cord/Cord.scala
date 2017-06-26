package phoenix.models.cord

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class Cord(id: Int = 0, referenceNumber: String = "", isCart: Boolean = true) extends FoxModel[Cord]

object Cord {
  def cordRefNumRegex = """([a-zA-Z0-9-_]*)""".r
}

class Cords(tag: Tag) extends FoxTable[Cord](tag, "cords") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def isCart          = column[Boolean]("is_cart")

  def * = (id, referenceNumber, isCart) <> ((Cord.apply _).tupled, Cord.unapply)

  val cart  = foreignKey(Carts.tableName, referenceNumber, Carts)(_.referenceNumber)
  val order = foreignKey(Orders.tableName, referenceNumber, Orders)(_.referenceNumber)
}

object Cords
    extends FoxTableQuery[Cord, Cords](new Cords(_))
    with ReturningId[Cord, Cords]
    with SearchByRefNum[Cord, Cords] {

  def findOneByRefNum(refNum: String) =
    filter(_.referenceNumber === refNum).one

  val returningLens: Lens[Cord, Int] = lens[Cord].id
}
