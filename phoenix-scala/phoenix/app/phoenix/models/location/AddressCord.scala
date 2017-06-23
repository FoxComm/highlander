package phoenix.models.location

import core.db._
import phoenix.models.location.Addresses.{filter, QuerySeq}
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class AddressCord(id: Int = 0, cordRef: String, addressId: Int) extends FoxModel[AddressCord] {}

class AddressCords(tag: Tag) extends FoxTable[AddressCord](tag, "address_cord") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef   = column[String]("cord_ref")
  def addressId = column[Int]("address_id")

  def * = (id, cordRef, addressId) <> ((AddressCord.apply _).tupled, AddressCord.unapply)

  def address = foreignKey(Addresses.tableName, addressId, Countries)(_.id)
}

object AddressCords
    extends FoxTableQuery[AddressCord, AddressCords](new AddressCords(_))
    with ReturningId[AddressCord, AddressCords] {
  val returningLens: Lens[AddressCord, Int] = lens[AddressCord].id

  def findByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def findByAddressId(addressId: Int): QuerySeq =
    filter(_.addressId === addressId)
}
