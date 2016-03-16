package utils

import cats.data.Validated.Valid
import cats.data.{ValidatedNel, Xor}
import monocle.Lens
import services.{DatabaseFailure, Failure, Failures, GeneralFailure}
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}
import utils.Strings._
import utils.table.SearchById
import utils.aliases._

trait ModelWithIdParameter[T <: ModelWithIdParameter[T]] extends Validation[T] { self: T ⇒
  type Id = Int

  def id: Id

  def isNew: Boolean = id == 0

  def searchKey(): Option[String] = None

  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter

  def validate: ValidatedNel[Failure, T] = Valid(this)

  def sanitize: T = this

  def updateTo(newModel: T): Failures Xor T = Xor.right(newModel)

  // Read-only lens that returns String representation of primary search key value
  def primarySearchKeyLens: Lens[T, String] = Lens[T, String](_.id.toString)(_ ⇒ _ ⇒ this)

  def mustBeCreated: Failures Xor T =
    if (id == 0) Xor.Left(GeneralFailure("Refusing to update unsaved model").single) else Xor.right(this)
}

trait ModelWithLockParameter[T <: ModelWithLockParameter[T]] extends ModelWithIdParameter[T] { self: T ⇒
  def isLocked: Boolean
}

trait TableWithIdColumn[I] {
  def id: Rep[I]
}

trait TableWithLockColumn[I] extends TableWithIdColumn[I] {
  def isLocked: Rep[Boolean]
}

private[utils] abstract class TableWithIdInternal[M <: ModelWithIdParameter[M], I](tag: Tag, name: String)
  extends Table[M](tag, name) with TableWithIdColumn[I]

private[utils] abstract class TableWithLockInternal[M <: ModelWithLockParameter[M], I](tag: Tag, name: String)
  extends TableWithIdInternal[M, I](tag, name) with TableWithLockColumn[I]

object GenericTable {
  /** This allows us to enforce that tables have the same ID column as their case class. */
  type TableWithId[MODEL <: ModelWithIdParameter[MODEL]] = TableWithIdInternal[MODEL, MODEL#Id]
  type TableWithLock[MODEL <: ModelWithLockParameter[MODEL]] = TableWithLockInternal[MODEL, MODEL#Id]
}

abstract class TableQueryWithId[M <: ModelWithIdParameter[M], T <: GenericTable.TableWithId[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) with SearchById[M, T] {

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

  def update(oldModel: M, newModel: M)(implicit ec: EC, db: DB): DbResult[M] = (for {
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

object ExceptionWrapper {
  def wrapDbio[A](dbio: DBIO[A])(implicit ec: EC): DbResult[A] = {
    import scala.util.{Failure, Success}

    import services.DatabaseFailure

    dbio.asTry.flatMap {
      case Success(value) ⇒ DbResult.good(value)
      case Failure(e) ⇒ DbResult.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResult[A](dbresult: DbResult[A])(implicit ec: EC): DbResult[A] = {
    import scala.util.{Failure, Success}

    dbresult.asTry.flatMap {
      case Success(value) ⇒ lift(value)
      case Failure(e) ⇒ DbResult.failure(DatabaseFailure(e.getMessage))
    }
  }
}

abstract class TableQueryWithLock[M <: ModelWithLockParameter[M], T <: GenericTable.TableWithLock[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQueryWithId[M, T](idLens)(construct) {

}
