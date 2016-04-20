package utils.db

import cats.data.Xor
import failures.{Failure, Failures}
import monocle.Lens
import slick.dbio.DBIO
import slick.driver.PostgresDriver.api._
import slick.lifted.{TableQuery, Tag}
import utils.aliases._
import utils.db.DbResultT._

abstract class FoxTableQuery[M <: FoxModel[M], T <: FoxTable[M]]
(idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  extends TableQuery[T](construct) with SearchById[M, T] {

  import ExceptionWrapper._

  def tableName: String = baseTableRow.tableName

  private val compiledById = this.findBy(_.id)

  def findById(i: M#Id) = compiledById(i)

  def findOneById(i: M#Id): DBIO[Option[M]] =
    findById(i).result.headOption

  def findAllByIds(ids: Set[M#Id]): QuerySeq =
    filter(_.id.inSet(ids))

  type Returning[R] = slick.driver.JdbcActionComponent#ReturningInsertActionComposer[M, R]
  val returningId: Returning[M#Id] = this.returning(map(_.id))
  def returningIdAction(id: M#Id)(model: M): M = idLens.set(id)(model)

  def createAll(values: Iterable[M])(implicit ec: EC): DbResult[Option[Int]] = wrapDbResult((for {
    saveUs ← * <~ beforeSaveBatch(values)
    result ← * <~ (this ++= saveUs).toXor
  } yield result).value)

  def createAllReturningIds[R](values: Seq[M], returning: Returning[R] = returningId)
    (implicit ec: EC): DbResult[Seq[R]] = wrapDbResult((for {
    saveUs ← * <~ beforeSaveBatch(values)
    result ← * <~ (returning ++= saveUs).toXor
  } yield result).value)

  private def beforeSaveBatch(values: Iterable[M])(implicit ec: EC): DbResultT[Iterable[M]] =
    DbResultT.sequence(values.map(beforeSave).map(DbResultT.fromXor))

  def create[R](model: M, returning: Returning[R] = returningId, action: R ⇒ M ⇒ M = returningIdAction _)
    (implicit ec: EC): DbResult[M] =
    beforeSave(model).fold(DbResult.failures, { good ⇒
      wrapDbio((returning += good).map(ret ⇒ action(ret)(good)))
    })

  def update(oldModel: M, newModel: M)(implicit ec: EC): DbResult[M] = (for {
    _        ← * <~ oldModel.mustBeCreated
    prepared ← * <~ beforeSave(newModel)
    _        ← * <~ oldModel.updateTo(prepared)
    _        ← * <~ wrapDbio(this.findById(oldModel.id).update(prepared))
  } yield newModel).value

  private def beforeSave(model: M): Failures Xor M =
    model
      .sanitize
      .validate
      .toXor

  def deleteById[A](id: M#Id, onSuccess: ⇒ DbResult[A], onFailure: M#Id ⇒ Failure)
    (implicit ec: EC): DbResult[A] = {
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

  implicit class TableQueryWrappers(q: QuerySeq) {

    def deleteAll[A](onSuccess: ⇒ DbResult[A], onFailure: ⇒ DbResult[A])(implicit ec: EC): DbResult[A] = {
      q.delete.flatMap {
        case 0 ⇒ onFailure
        case _ ⇒ onSuccess
      }
    }
  }
}
