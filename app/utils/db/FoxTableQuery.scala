package utils.db

import cats.data.Xor
import failures.{Failure, Failures}
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}
import utils.aliases._
import utils.db.DbResultT._
import utils.db.UpdateReturning._

abstract class FoxTableQuery[M <: FoxModel[M], T <: FoxTable[M]]
  (construct: Tag ⇒ T)
  extends TableQuery[T](construct)
  with SearchById[M, T]
  with ReturningTableQuery[M, T] {

  import ExceptionWrapper._

  def tableName: String = baseTableRow.tableName

  private val compiledById = this.findBy(_.id)

  def findById(i: M#Id) = compiledById(i)

  def findOneById(i: M#Id): DBIO[Option[M]] =
    findById(i).result.headOption

  def findAllByIds(ids: Set[M#Id]): QuerySeq =
    filter(_.id.inSet(ids))

  private def returningTable: Returning[M, Ret] = this.returning(returningQuery)

  def createAll(unsaved: Iterable[M])(implicit ec: EC): DbResult[Option[Int]] = (for {
    prepared ← * <~ beforeSaveBatch(unsaved)
    returned ← * <~ wrapDbio(this ++= prepared)
  } yield returned).value

  def createAllReturningIds(unsaved: Iterable[M])(implicit ec: EC): DbResult[Seq[M#Id]] = (for {
    prepared ← * <~ beforeSaveBatch(unsaved)
    returned ← * <~ wrapDbio(this.returning(map(_.id)) ++= prepared)
  } yield returned).value

  def createAllReturningModels(unsaved: Iterable[M])(implicit ec: EC): DbResult[Seq[M]] = (for {
    prepared ← * <~ beforeSaveBatch(unsaved)
    returned ← * <~ wrapDbio(returningTable ++= prepared)
  } yield for ((m,r) ← prepared.zip(returned)) yield returningLens.set(m)(r)).value

  def create(unsaved: M)(implicit ec: EC): DbResult[M] = (for {
    prepared ← * <~ beforeSave(unsaved)
    returned ← * <~ wrapDbio(returningTable += prepared)
  } yield returningLens.set(prepared)(returned)).value

  def update(oldModel: M, newModel: M)(implicit ec: EC): DbResult[M] = (for {
    _        ← * <~ oldModel.mustBeCreated
    prepared ← * <~ beforeSave(newModel)
    _        ← * <~ oldModel.updateTo(prepared)
    _        ← * <~ findById(oldModel.id).update(prepared)
  } yield prepared).value

  def updateReturning(oldModel: M, newModel: M)(implicit ec: EC): DbResult[M] = (for {
    _        ← * <~ oldModel.mustBeCreated
    prepared ← * <~ beforeSave(newModel)
    _        ← * <~ oldModel.updateTo(prepared)
    returned ← * <~ findById(oldModel.id).extract.updateReturningHead(returningQuery, prepared)
  } yield returningLens.set(prepared)(returned)).value

  private def beforeSave(model: M): Failures Xor M =
    model
      .sanitize
      .validate
      .toXor

  private def beforeSaveBatch(unsaved: Iterable[M])(implicit ec: EC): DbResultT[Seq[M]] = DbResultT.sequence {
    unsaved.map(m ⇒ DbResultT.fromXor(beforeSave(m)))
  }.map(_.toSeq)

  def deleteById[A](id: M#Id, onSuccess: ⇒ DbResult[A], onFailure: M#Id ⇒ Failure)(implicit ec: EC): DbResult[A] = {
    val deleteResult = findById(id).delete.flatMap {
      case 0 ⇒ DbResult.failure(onFailure(id))
      case _ ⇒ onSuccess
    }
    wrapDbResult(deleteResult)
  }

  def refresh(model: M)(implicit ec: EC): DBIO[M] =
    findOneById(model.id).safeGet

  type QuerySeq = Query[T, M, Seq]
  type QuerySeqWithMetadata = QueryWithMetadata[T, M, Seq]

  implicit class EnrichedTableQuery(q: QuerySeq) {

    def deleteAll[A](onSuccess: ⇒ DbResult[A], onFailure: ⇒ DbResult[A])(implicit ec: EC): DbResult[A] = {
      q.delete.flatMap {
        case 0 ⇒ onFailure
        case _ ⇒ onSuccess
      }
    }
  }
}
