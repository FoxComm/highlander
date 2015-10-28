package utils

import scala.concurrent.{Future, ExecutionContext}

import cats.data.{Xor, ValidatedNel}
import monocle.Lens
import services._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick._
import utils.Slick.implicits._
import utils.Strings._

trait Model {
  def modelName: String = getClass.getCanonicalName.lowerCaseFirstLetter
}

trait NewModel extends Model {
  // override me in ModelWithIdParameter with isNew = id == 0
  def isNew: Boolean

  def validate: ValidatedNel[Failure, Model]
}

trait ModelWithIdParameter extends Model {
  type Id = Int

  def id: Id
}

trait ModelWithLockParameter extends ModelWithIdParameter {
  def locked: Boolean
}

trait TableWithIdColumn[I] {
  def id: Rep[I]
}

trait TableWithLockColumn[I] extends TableWithIdColumn[I] {
  def locked: Rep[Boolean]
}

private[utils] abstract class TableWithIdInternal[M <: ModelWithIdParameter, I](tag: Tag, name: String)
  extends Table[M](tag, name) with TableWithIdColumn[I]

private[utils] abstract class TableWithLockInternal[M <: ModelWithLockParameter, I](tag: Tag, name: String)
  extends TableWithIdInternal[M, I](tag, name) with TableWithLockColumn[I]

object GenericTable {
  /** This allows us to enforce that tables have the same ID column as their case class. */
  type TableWithId[MODEL <: ModelWithIdParameter] = TableWithIdInternal[MODEL, MODEL#Id]
  type TableWithLock[MODEL <: ModelWithLockParameter] = TableWithLockInternal[MODEL, MODEL#Id]
}

abstract class TableQueryWithId[M <: ModelWithIdParameter, T <: GenericTable.TableWithId[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) {

  def tableName: String = baseTableRow.tableName

  val returningId =
    this.returning(map(_.id))

  private val compiledById = this.findBy(_.id)

  def findById(i: M#Id) = compiledById(i)

  def findOneById(i: M#Id): DBIO[Option[M]] =
    findById(i).result.headOption

  def save(model: M)(implicit ec: ExecutionContext): DBIO[M] = for {
    id ← returningId += model
  } yield idLens.set(id)(model)

  def deleteById(i: M#Id): DBIO[Int] =
    findById(i).delete

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
        case Xor.Right(value) ⇒ action(value)
        case failures @ Xor.Left(_) ⇒ lift(failures)
      }.transactionally.run()
    }

    // TODO: try to run ResultWithMetadata.result in the same transaction
    // TODO: until than do not use this for transactional updates
    protected def selectInnerWithMetadata[R](dbio: DBIO[Option[M]])(action: M ⇒ ResultWithMetadata[R],
      checks: Checks = checks,  notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Future[ResultWithMetadata[R]] = {
      dbio.map(maybe ⇒ applyAllChecks(checks, maybe, notFoundFailure)).flatMap {
        case Xor.Right(value)   ⇒ lift(action(value))
        case Xor.Left(failures) ⇒ lift(ResultWithMetadata.fromFailures[R](failures))
      }.transactionally.run()
    }

    def selectOne[R](action: M ⇒ DbResult[R], checks: Checks = checks, notFoundFailure: Failure = notFound404)
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(q.result.headOption)(action, checks)
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

    protected def querySearchKey: Option[Any] = QueryErrorInfo.searchKeyForQuery(q, primarySearchTerm)

    def queryError: String = querySearchKey.map(key ⇒ s"${tableName.tableNameToCamel} with $primarySearchTerm=$key")
      .getOrElse(s"${tableName.tableNameToCamel}")

    def notFound404 = NotFoundFailure404(s"$queryError not found")
    def notFound400 = NotFoundFailure400(s"$queryError not found")

    protected def mustExist(maybe: Option[M], notFoundFailure: Failure): Failures Xor M =
      Xor.fromOption(maybe, notFoundFailure.single)
  }
}

abstract class TableQueryWithLock[M <: ModelWithLockParameter, T <: GenericTable.TableWithLock[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQueryWithId[M, T](idLens)(construct) {

  implicit class LockableQueryWrappers(q: QuerySeq) extends TableQueryWrappers(q) {

    override def checks: Checks = super.checks + mustNotBeLocked

    def mustNotBeLocked(model: M): Failures Xor M =
      if (model.locked) Xor.left(LockedFailure(s"$queryError is locked").single) else Xor.Right(model)
  }

}
