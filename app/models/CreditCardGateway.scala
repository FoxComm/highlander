package models

import com.wix.accord.dsl.{validator => createValidator}
import payloads.CreateCustomerPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import org.scalactic._

case class CreditCardGateway(id: Int = 0, customerId: Option[Int] = None, gatewayAccountId: Int,
                             lastFour: String, expMonth: String, expYear: String) extends Validation[CreditCardGateway] {
  override def validator = createValidator[CreditCardGateway] { m =>
    m.lastFour should matchRegex("[0-9]{4}")
    // p.expMonth.toInt b/t 1-12
    //m.expYear should matchRegex("[0-9]+")
  }
}

class CreditCardGateways(tag: Tag) extends Table[CreditCardGateway](tag, "credit_card_gateways") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Option[Int]]("customer_id")
  def gatewayAccountId = column[Int]("gateway")
  def lastFour = column[String]("last_four")
  def expMonth = column[String]("exp_month")
  def expYear = column[String]("exp_year")

  def * = (id, customerId, gatewayAccountId,
    lastFour, expMonth, expYear) <> ((CreditCardGateway.apply _).tupled, CreditCardGateway.unapply)
}

object CreditCardGateways {
  val table = TableQuery[CreditCardGateways]
  val returningId = table.returning(table.map(_.id))

  def findById(id: Int)(implicit db: Database): Future[Option[CreditCardGateway]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { table.filter(_.id === id) }
}

