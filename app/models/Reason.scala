package models

import scala.concurrent.ExecutionContext

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.CustomDirectives.SortAndPage
import utils.Litterbox._
import utils.Validation

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.Slick.implicits._

final case class Reason(id: Int = 0, storeAdminId: Int, body: String, parentId: Option[Int] = None)
  extends ModelWithIdParameter[Reason]
  with Validation[Reason] {

  import Validation._

  override def validate: ValidatedNel[Failure, Reason] = {
    ( notEmpty(body, "body")
      |@| lesserThanOrEqual(body.length, 255, "bodySize")
      ).map { case _ ⇒ this }
  }

  def isSubReason: Boolean = parentId.isDefined
}

object Reason {
}

class Reasons(tag: Tag) extends GenericTable.TableWithId[Reason](tag, "reasons")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId = column[Int]("store_admin_id")
  def parentId = column[Option[Int]]("parent_id")
  def body = column[String]("body")

  def * = (id, storeAdminId, body, parentId) <> ((Reason.apply _).tupled, Reason.unapply)

  def author = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Reasons extends TableQueryWithId[Reason, Reasons](
  idLens = GenLens[Reason](_.id)
)(new Reasons(_)) {

  def sortedAndPaged(query: QuerySeq)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    val sortedQuery = query.withMetadata.sortAndPageIfNeeded { case (s, reason) ⇒
      s.sortColumn match {
        case "id"           ⇒ if(s.asc) reason.id.asc           else reason.id.desc
        case "storeAdminId" ⇒ if(s.asc) reason.storeAdminId.asc else reason.storeAdminId.desc
        case "parentId"     ⇒ if(s.asc) reason.parentId.asc     else reason.parentId.desc
        case "body"         ⇒ if(s.asc) reason.body.asc         else reason.body.desc
        case other          ⇒ invalidSortColumn(other)
      }
    }

    sortedQuery.paged
  }

  def queryAll(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): QuerySeqWithMetadata =
    this.sortedAndPaged(this)
}
