package utils.db

import failures.{Failure, NotFoundFailure400, NotFoundFailure404}
import slick.driver.PostgresDriver.api._
import utils.Strings._
import utils.aliases._

trait SearchById[M <: FoxModel[M], T <: FoxTable[M]] {

  def primarySearchTerm = "id"

  def tableName: String

  def findOneById(id: M#Id): DBIO[Option[M]]

  def notFound404K[K](searchKey: K) =
    NotFoundFailure404(
        s"${tableName.tableNameToCamel} with $primarySearchTerm=$searchKey not found")

  protected def notFound400K[K](searchKey: K) =
    NotFoundFailure400(
        s"${tableName.tableNameToCamel} with $primarySearchTerm=$searchKey not found")

  def mustFindById404(id: M#Id)(implicit ec: EC, db: DB): DbResultT[M] = mustFindById(id)

  def mustFindById400(id: M#Id)(implicit ec: EC, db: DB): DbResultT[M] =
    mustFindById(id, notFound400K)

  private def mustFindById(id: M#Id, notFoundFailure: M#Id ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] = {

    this.findOneById(id).dbresult.flatMap {
      case Some(model) ⇒ DbResultT.good(model)
      case None        ⇒ DbResultT.failure(notFoundFailure(id))
    }
  }
}

trait SearchByRefNum[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  override def primarySearchTerm = "referenceNumber"

  def findOneByRefNum(refNum: String): DBIO[Option[M]]

  def mustFindByRefNum(refNum: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] = {
    findOneByRefNum(refNum).dbresult.flatMap {
      case Some(model) ⇒ DbResultT.good(model)
      case None        ⇒ DbResultT.failure(notFoundFailure(refNum))
    }
  }
}

trait SearchByCode[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  override def primarySearchTerm = "code"

  def findOneByCode(code: String): DBIO[Option[M]]

  def mustFindByCode(code: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] = {
    findOneByCode(code).dbresult.flatMap {
      case Some(model) ⇒ DbResultT.good(model)
      case None        ⇒ DbResultT.failure(notFoundFailure(code))
    }
  }
}

trait SearchByIdAndName[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  override def primarySearchTerm = "name"

  def findOneByIdAndName(id: Int, name: String): DBIO[Option[M]]

  def mustFindByName(id: Int, name: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] = {
    findOneByIdAndName(id, name).dbresult.flatMap {
      case Some(model) ⇒ DbResultT.good(model)
      case None        ⇒ DbResultT.failure(notFoundFailure(name))
    }
  }
}
