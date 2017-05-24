package core.db

import cats.implicits._
import core.db.UpdateReturning._
import core.failures.{Failure, Failures}
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}

abstract class FoxTableQuery[M <: FoxModel[M], T <: FoxTable[M]](construct: Tag ⇒ T)
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

  def createAll(unsaved: Iterable[M])(implicit ec: EC): DbResultT[Option[Int]] =
    for {
      prepared ← * <~ beforeSaveBatch(unsaved)
      returned ← * <~ wrapDbio(this ++= prepared)
    } yield returned

  def createAllReturningIds(unsaved: Iterable[M])(implicit ec: EC): DbResultT[Seq[M#Id]] =
    for {
      prepared ← * <~ beforeSaveBatch(unsaved)
      returned ← * <~ wrapDbio(this.returning(map(_.id)) ++= prepared)
    } yield returned

  def createAllReturningModels(unsaved: Iterable[M])(implicit ec: EC): DbResultT[Seq[M]] =
    for {
      prepared          ← * <~ beforeSaveBatch(unsaved)
      returned          ← * <~ wrapDbio(returningTable ++= prepared)
    } yield for ((m, r) ← prepared.zip(returned)) yield returningLens.set(m)(r)

  def create(unsaved: M)(implicit ec: EC): DbResultT[M] =
    for {
      prepared ← * <~ beforeSave(unsaved)
      returned ← * <~ wrapDbio(returningTable += prepared)
    } yield returningLens.set(prepared)(returned)

  def update(oldModel: M, newModel: M)(implicit ec: EC): DbResultT[M] =
    for {
      _        ← * <~ oldModel.mustBeCreated
      prepared ← * <~ beforeSave(newModel)
      _        ← * <~ oldModel.updateTo(prepared)
      _        ← * <~ wrapDbio(findById(oldModel.id).update(prepared))
    } yield prepared

  def updateReturning(oldModel: M, newModel: M)(implicit ec: EC): DbResultT[M] =
    for {
      _        ← * <~ oldModel.mustBeCreated
      prepared ← * <~ beforeSave(newModel)
      _        ← * <~ oldModel.updateTo(prepared)
      returned ← * <~ findById(oldModel.id).extract.updateReturningHead(returningQuery, prepared)
    } yield returningLens.set(prepared)(returned)

  protected def beforeSave(model: M): Either[Failures, M] =
    model.sanitize.validate.toEither

  private def beforeSaveBatch(unsaved: Iterable[M])(implicit ec: EC): DbResultT[List[M]] =
    DbResultT.seqCollectFailures {
      unsaved.toList.map(m ⇒ DbResultT.fromEither(beforeSave(m)))
    }

  def deleteById[A](id: M#Id, onSuccess: ⇒ DbResultT[A], onFailure: M#Id ⇒ Failure)(
      implicit ec: EC): DbResultT[A] = {
    val deleteResult = findById(id).delete.dbresult.flatMap {
      case 0 ⇒ DbResultT.failure[A](onFailure(id))
      case _ ⇒ onSuccess
    }
    wrapDbResultT(deleteResult)
  }

  def refresh(model: M)(implicit ec: EC): DBIO[M] =
    findOneById(model.id).safeGet

  type QuerySeq = Query[T, M, Seq]

  implicit class EnrichedTableQuery(q: QuerySeq) {
    def deleteAll(implicit ec: EC): DbResultT[Int] = wrapDbio(q.delete)

    def deleteAll[A](onSuccess: ⇒ DbResultT[A], onFailure: ⇒ DbResultT[A])(
        implicit ec: EC): DbResultT[A] = {
      val deleteResult = q.delete.dbresult.flatMap {
        case 0 ⇒ onFailure
        case _ ⇒ onSuccess
      }
      wrapDbResultT(deleteResult)
    }

    def deleteAllWithRowsBeingAffected(implicit ec: EC): DbResultT[Boolean] =
      deleteAll.map(_ > 0)
  }
}
