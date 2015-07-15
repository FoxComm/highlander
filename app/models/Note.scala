package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


case class Note(id: Int = 0, orderId: Int, storeAdminId: Int, noteText: String) extends ModelWithIdParameter

object Note

class Notes(tag: Tag) extends GenericTable.TableWithId[Note](tag, "notes") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def storeAdminId = column[Int]("store_admin_id")
  def noteText = column[String]("note_text")

  def * = (id, orderId, storeAdminId, noteText) <> ((Note.apply _).tupled, Note.unapply)

  def author = foreignKey("store_admins", storeAdminId, TableQuery[StoreAdmins])(_.id) // what does this do? =]
}


object Notes extends TableQueryWithId[Note, Notes](
  idLens = GenLens[Note](_.id)
)(new Notes(_)) {

  def filterByOrderId(id: Int)
                      (implicit ec: ExecutionContext, db:Database): Future[Option[Seq[Note]]] = {
    _filterByOrderId(id).run().map{Some(_)}
  }

  def _filterByOrderId(id: Int) = {
    filter(_.orderId === id).result
  }

}
