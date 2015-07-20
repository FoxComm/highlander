package models

import utils.{RichTable, RunOnDbIO, Model}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class State(id: Int, name: String, abbreviation: String) extends Model {
  val abbrev = this.abbreviation
}

class States(tag: Tag) extends Table[State](tag, "states") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def abbreviation = column[String]("abbreviation")

  def * = (id, name, abbreviation) <> ((State.apply _).tupled, State.unapply)
}

object States {
  val table = TableQuery[States]

  def findByAbbrev(abbrev: String)(implicit db: Database): Future[Option[State]] =
    _findByAbbrev(abbrev).run()

  def _findByAbbrev(abbrev: Rep[String]) =
    table.filter(_.abbreviation === abbrev).take(1).result.headOption

  def findByName(name: String)
                (implicit db: Database) = { db.run(_findByName(name)) }

  def _findByName(name: String) = { table.filter(_.name === name).result.headOption }
}
