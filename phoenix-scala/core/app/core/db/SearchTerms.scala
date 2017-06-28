package core.db

import cats.implicits._
import core.failures.{Failure, NotFoundFailure400, NotFoundFailure404}
import core.utils.Strings._
import slick.jdbc.PostgresProfile.api._

trait SearchById[M <: FoxModel[M], T <: FoxTable[M]] {

  def tableName: String

  def findOneById(id: M#Id): DBIO[Option[M]]

  def notFound404K[K](searchKey: K) =
    NotFoundFailure404(s"${tableName.tableNameToCamel} with key=$searchKey not found")

  protected def notFound400K[K](searchKey: K) =
    NotFoundFailure400(s"${tableName.tableNameToCamel} with key=$searchKey not found")

  def mustFindById404(id: M#Id)(implicit ec: EC, db: DB): DbResultT[M] = mustFindById(id)

  def mustFindById400(id: M#Id)(implicit ec: EC, db: DB): DbResultT[M] =
    mustFindById(id, notFound400K)

  private def mustFindById(id: M#Id, notFoundFailure: M#Id ⇒ Failure = notFound404K)(implicit ec: EC,
                                                                                     db: DB): DbResultT[M] =
    this.findOneById(id).dbresult.flatMap {
      case Some(model) ⇒ model.pure[DbResultT]
      case None        ⇒ DbResultT.failure(notFoundFailure(id))
    }
}

trait SearchByRefNum[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  def findOneByRefNum(refNum: String): DBIO[Option[M]]

  def mustFindByRefNum(refNum: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] =
    findOneByRefNum(refNum).dbresult.flatMap {
      case Some(model) ⇒ model.pure[DbResultT]
      case None        ⇒ DbResultT.failure(notFoundFailure(refNum))
    }
}

trait SearchByCode[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  def findOneByCode(code: String): DBIO[Option[M]]

  def mustFindByCode(code: String, notFoundFailure: String ⇒ Failure = notFound404K)(implicit ec: EC,
                                                                                     db: DB): DbResultT[M] =
    findOneByCode(code).dbresult.flatMap {
      case Some(model) ⇒ model.pure[DbResultT]
      case None        ⇒ DbResultT.failure(notFoundFailure(code))
    }
}

trait SearchByIdAndName[M <: FoxModel[M], T <: FoxTable[M]] extends SearchById[M, T] {

  def findOneByIdAndName(id: Int, name: String): DBIO[Option[M]]

  def mustFindByName(id: Int, name: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[M] =
    findOneByIdAndName(id, name).dbresult.flatMap {
      case Some(model) ⇒ model.pure[DbResultT]
      case None        ⇒ DbResultT.failure(notFoundFailure(name))
    }
}
