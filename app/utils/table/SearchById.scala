package utils.table

import scala.concurrent.ExecutionContext

import services.{Failure, NotFoundFailure400, NotFoundFailure404}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Strings._
import utils.{GenericTable, ModelWithIdParameter}

trait SearchById[M <: ModelWithIdParameter[M], T <: GenericTable.TableWithId[M]] {

  def primarySearchTerm = "id"

  def tableName: String

  def findOneById(id: M#Id): DBIO[Option[M]]

  protected def notFound404K[K](searchKey: K) =
    NotFoundFailure404(s"${tableName.tableNameToCamel} with $primarySearchTerm=$searchKey not found")

  protected def notFound400K[K](searchKey: K) =
    NotFoundFailure400(s"${tableName.tableNameToCamel} with $primarySearchTerm=$searchKey not found")

  def mustFindById(id: M#Id, notFoundFailure: M#Id ⇒ Failure = notFound404K)
    (implicit ec: ExecutionContext, db: Database): DbResult[M] = {

    this.findOneById(id).flatMap {
      case Some(model) ⇒ DbResult.good(model)
      case None ⇒ DbResult.failure(notFoundFailure(id))
    }
  }
}

trait SearchByRefNum[M <: ModelWithIdParameter[M], T <: GenericTable.TableWithId[M]] extends SearchById[M, T] {

  override def primarySearchTerm = "referenceNumber"

  def findOneByRefNum(refNum: String): DBIO[Option[M]]

  def mustFindByRefNum(refNum: String, notFoundFailure: String ⇒ Failure = notFound404K)
    (implicit ec: ExecutionContext, db: Database): DbResult[M] = {
    findOneByRefNum(refNum).flatMap {
      case Some(model) ⇒ DbResult.good(model)
      case None ⇒ DbResult.failure(notFoundFailure(refNum))
    }
  }
}

trait SearchByCode[M <: ModelWithIdParameter[M], T <: GenericTable.TableWithId[M]] extends SearchById[M, T] {

  override def primarySearchTerm = "code"

  def findOneByCode(code: String): DBIO[Option[M]]

  def mustFindByCode(code: String, notFoundFailure: String ⇒ Failure = notFound404K)
    (implicit ec: ExecutionContext, db: Database): DbResult[M] = {
    findOneByCode(code).flatMap {
      case Some(model) ⇒ DbResult.good(model)
      case None ⇒ DbResult.failure(notFoundFailure(code))
    }
  }
}
