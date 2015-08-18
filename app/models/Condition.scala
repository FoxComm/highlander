package models

import com.pellucid.sealerate
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import slick.lifted.Tag
import utils.{TableQueryWithId, GenericTable, RichTable, ADT}

final case class Condition(id: Int = 0, subject: String, field: String, operator: String,
  valInt: Option[Int] = None, valString: Option[String] = None)

object Condition {
  sealed trait Operator

  case object Equals extends Operator
  case object NotEquals extends Operator

  object Operator extends ADT[Operator] {
    def types = sealerate.values[Operator]
  }

  implicit val operatorColumnType = Operator.slickColumn
}

class Conditions(tag: Tag) extends GenericTable.TableWithId[Condition](tag, "conditions") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def subject = column[String]("subject")
  def field = column[String]("field")
  def operator = column[Condition.Operator]("operator")
  def valInt = column[Int]("valInt")
  def valString = column[String]("valString")

  def * = (id, subject, field, operator, valInt, valString) <> ((Condition.apply _).tupled, Condition.unapply)
}

object Conditions extends TableQueryWithId[Condition, Conditions](
  idLens = GenLens[Condition](_.id)
)(new Conditions(_)) {

}