package utils

import scala.concurrent.{Future, ExecutionContext}

import cats.data.Validated.Valid
import cats.data.{Xor, ValidatedNel}
import monocle.Lens
import services._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick._
import utils.Slick.implicits._
import utils.Strings._

trait ModelWithIdParameter[T <: ModelWithIdParameter[T]] extends Validation[T] { self: T ⇒
  type Id = Int

  def id: Id

  def isNew: Boolean = id == 0

  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter

  def validate: ValidatedNel[Failure, T] = Valid(this)

  def sanitize: T = this

  def updateTo(newModel: T): Failures Xor T = Xor.right(newModel)

  // Read-only lens that returns String representation of primary search key value
  def primarySearchKeyLens: Lens[T, String] = Lens[T, String](_.id.toString)(_ ⇒ _ ⇒ this)
}

trait ModelWithLockParameter[T <: ModelWithLockParameter[T]] extends ModelWithIdParameter[T] { self: T ⇒
  def locked: Boolean
}

trait TableWithIdColumn[I] {
  def id: Rep[I]
}

trait TableWithLockColumn[I] extends TableWithIdColumn[I] {
  def locked: Rep[Boolean]
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
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) {

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

  /** DEPRECATION WARNING **
  * This method will soon be deprecated in favor of `create` which provides model sanitization,
  * validation and exception handling.
  */
  def saveNew[R](model: M, returning: Returning[R] = returningId, action: R ⇒ M ⇒ M = returningIdAction _)
  (implicit ec: ExecutionContext): DBIO[M] =
    (returning += model).map(ret ⇒ action(ret)(model))

  def create[R](model: M, returning: Returning[R] = returningId, action: R ⇒ M ⇒ M = returningIdAction _)
  (implicit ec: ExecutionContext): DbResult[M] =
    beforeSave(model).fold(DbResult.failures, { good ⇒
      wrapDbio((returning += good).map(ret ⇒ action(ret)(good)))
    })

  def update(oldModel: M, newModel: M)(implicit ec: ExecutionContext, db: Database): DbResult[M] = {
    val mightUpdate = for {
      checked ← beforeSave(newModel)
      updateable ← oldModel.updateTo(checked)
    } yield this.findById(updateable.id).update(updateable)
    mightUpdate.fold(DbResult.failures, good ⇒ wrapDbio(good >> lift(newModel)))
  }

  private def beforeSave(model: M): Failures Xor M =
    model
      .sanitize
      .validate
      .toXor

  def deleteById[A](id: M#Id, onSuccess: ⇒ DbResult[A], onFailure: ⇒ DbResult[A])
    (implicit ec: ExecutionContext): DbResult[A] = {
    val deleteResult = findById(id).delete.flatMap {
      case 0 ⇒ onFailure
      case _ ⇒ onSuccess
    }
    wrapDbResult(deleteResult)
  }

  type QuerySeq = Query[T, M, Seq]
  type QuerySeqWithMetadata = QueryWithMetadata[T, M, Seq]

  def primarySearchTerm: String = "id"

  implicit class TableQueryWrappers(q: QuerySeq) {

    type Checks = Set[M ⇒ Failures Xor M]

    def checks: Checks = Set()

    private def applyAllChecks(checks: Checks, maybe: Option[M], notFoundFailure: Failure): Failures Xor M = {
      mustExist(maybe, notFoundFailure).flatMap { model ⇒
        checks.view.map(check ⇒ check(model)).find(_.isLeft)
          .getOrElse(Xor.right[Failures, M](model))
      }
    }

    protected def selectInner[R](dbio: DBIO[Option[M]])(action: M ⇒ DbResult[R], checks: Checks = checks,
      notFoundFailure: Failure = notFound404)(implicit ec: ExecutionContext, db: Database): Result[R] = {
      dbio.map(maybe ⇒ applyAllChecks(checks, maybe, notFoundFailure)).flatMap {
        case Xor.Right(value) ⇒ wrapDbResult(action(value))
        case failures @ Xor.Left(_) ⇒ lift(failures)
      }.transactionally.run()
    }

    // TODO: try to run ResultWithMetadata.result in the same transaction
    // TODO: until than do not use this for transactional updates
    protected def selectInnerWithMetadata[R](dbio: DBIO[Option[M]])(action: M ⇒ ResultWithMetadata[R],
      checks: Checks = checks,  notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Future[ResultWithMetadata[R]] = {
      dbio.map(maybe ⇒ applyAllChecks(checks, maybe, notFoundFailure) match {
        case Xor.Right(value)   ⇒ action(value).wrapExceptions
        case Xor.Left(failures) ⇒ ResultWithMetadata.fromFailures[R](failures)
      }).run()
    }

    def selectOne[R](action: M ⇒ DbResult[R], checks: Checks = checks, notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(q.result.headOption)(action, checks)
    }

    def mustFindById(id: M#Id)(notFoundFailure: M#Id ⇒ Failure = (id: M#Id) ⇒ notFound404)
      (implicit ec: ExecutionContext, db: Database): DbResult[M] = {
      findOneById(id).flatMap {
        case Some(model) ⇒ DbResult.good(model)
        case None ⇒ DbResult.failure(notFoundFailure(id))
      }
    }

    def selectOneWithMetadata[R](action: M ⇒ ResultWithMetadata[R], checks: Checks = checks,
      notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Future[ResultWithMetadata[R]] = {
      selectInnerWithMetadata(q.result.headOption)(action, checks)
    }

    def selectOneForUpdate[R](action: M ⇒ DbResult[R], checks: Checks = checks, notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(appendForUpdate(q.result.headOption))(action, checks)
    }

    def select[R](action: Seq[M] ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      q.result.flatMap(action).transactionally.run()
    }

    def selectForUpdate[R](action: Seq[M] ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      appendForUpdate(q.result).flatMap(action).transactionally.run()
    }

    def deleteAll[A](onSuccess: ⇒ DbResult[A], onFailure: ⇒ DbResult[A])(implicit ec: ExecutionContext): DbResult[A] = {
      q.delete.flatMap {
        case 0 ⇒ onFailure
        case _ ⇒ onSuccess
      }
    }

    protected def querySearchKey: Option[Any] = QueryErrorInfo.searchKeyForQuery(q, primarySearchTerm)

    def queryError: String = querySearchKey.map(key ⇒ s"${tableName.tableNameToCamel} with $primarySearchTerm=$key")
      .getOrElse(s"${tableName.tableNameToCamel}")

    def notFound404 = NotFoundFailure404(s"$queryError not found")
    def notFound400 = NotFoundFailure400(s"$queryError not found")

    protected def mustExist(maybe: Option[M], notFoundFailure: Failure): Failures Xor M =
      Xor.fromOption(maybe, notFoundFailure.single)
  }
}

object ExceptionWrapper {
  def wrapDbio[A](dbio: DBIO[A])(implicit ec: ExecutionContext): DbResult[A] = {
    import scala.util.{Failure, Success}
    import services.DatabaseFailure

    dbio.asTry.flatMap {
      case Success(value) ⇒ DbResult.good(value)
      case Failure(e) ⇒ DbResult.failure(DatabaseFailure(e.getMessage))
    }
  }

  def wrapDbResult[A](dbresult: DbResult[A])(implicit ec: ExecutionContext): DbResult[A] = {
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

  implicit class LockableQueryWrappers(q: QuerySeq) extends TableQueryWrappers(q) {

    override def checks: Checks = super.checks + mustNotBeLocked

    def mustNotBeLocked(model: M): Failures Xor M =
      if (model.locked) Xor.left(LockedFailure(s"$queryError is locked").single) else Xor.Right(model)
  }

}
