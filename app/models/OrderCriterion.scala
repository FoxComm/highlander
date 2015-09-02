package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId}


final case class OrderCriterion(id:Int = 0, name: String) extends ModelWithIdParameter


object OrderCriterion{
  sealed trait CriterionType
  // So, each of the below has it's own table.  I wonder if the right move is to just call this object and centralize all the finding and logic here?
  case object Destination extends CriterionType
  case object Weight extends CriterionType
  case object Price extends CriterionType // before discount
  case object Dimensions extends CriterionType
}

class OrderCriteria(tag: Tag) extends GenericTable.TableWithId[OrderCriterion](tag, "order_criteria") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> ((OrderCriterion.apply _).tupled, OrderCriterion.unapply)
}

object OrderCriteria extends TableQueryWithId[OrderCriterion, OrderCriteria](
  idLens = GenLens[OrderCriterion](_.id)
)(new OrderCriteria(_))
