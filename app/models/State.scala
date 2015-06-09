package models

import utils.RichTable

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class State(id: Int, name: String, abbreviation: String) {
  val abbrev = this.abbreviation
}

class States(tag: Tag) extends Table[State](tag, "states") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def abbreviation = column[String]("abbreviation")

  def * = (id, name, abbreviation) <> ((State.apply _).tupled, State.unapply)
}