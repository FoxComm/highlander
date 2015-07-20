package models

import slick.dbio
import slick.dbio.Effect.{Write, Read}
import slick.driver.PostgresDriver
import slick.profile.FixedSqlAction
import utils.{Validation, RichTable, Model}
import payloads.CreateAddressPayload

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class BillingAddress(addressId: Int, paymentId: Int) extends Model

class BillingAddresses(tag: Tag) extends Table[BillingAddress](tag, "billing_addresses") with RichTable {
  def addressId = column[Int]("address_id", O.PrimaryKey)
  def paymentId = column[Int]("payment_id", O.PrimaryKey)

  def * = (addressId, paymentId) <> ((BillingAddress.apply _).tupled, BillingAddress.unapply)

  def address = foreignKey("billing_addresses_address_id_fk", addressId, TableQuery[Addresses])(_.id)
  def payment = foreignKey("billing_addresses_applied_payments_id_fk", paymentId, TableQuery[OrderPayments])(_.id)
}

object BillingAddresses {
  val table = TableQuery[BillingAddresses]

  def save(address: BillingAddress)(implicit ec: ExecutionContext): DBIO[BillingAddress] =
    (table += address).map(_ ⇒ address)

  def _create(address: Address, paymentId: Int)
             (implicit ec: ExecutionContext,
              db: Database): DBIOAction[Address, NoStream, Effect.Write] = {
    for {
      addressId <- Addresses.returningId += address
      _ <- BillingAddresses.table += BillingAddress(addressId = addressId, paymentId = paymentId)
    } yield address.copy(id = addressId)
  }

  def _findByPaymentId(id: Int)(implicit ec: ExecutionContext, db: Database): Query[(Addresses, BillingAddresses), (Address, BillingAddress), Seq] = {
    (for {
      billingAddress ← table.filter(_.paymentId === id)
      address        ← Addresses if address.id === billingAddress.addressId
    } yield (address, billingAddress)).take(1)
  }

  def findByPaymentId(id: Int)(implicit ec: ExecutionContext, db: Database): Future[Option[(Address, BillingAddress)]] = {
    db.run(_findByPaymentId(id).result.headOption)
  }

  def count()(implicit ec: ExecutionContext, db: Database): Future[Int] = {
    db.run(table.length.result)
  }
}
