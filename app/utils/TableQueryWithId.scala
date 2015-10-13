package utils

import scala.concurrent.ExecutionContext

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

  def _findById(i: M#Id) = compiledById(i)

  def findById(i: M#Id): DBIO[Option[M]] =
    _findById(i).result.headOption

  def save(model: M)(implicit ec: ExecutionContext): DBIO[M] = for {
    id ← returningId += model
  } yield idLens.set(id)(model)

  def deleteById(i: M#Id): DBIO[Int] =
    _findById(i).delete

  type QuerySeq = Query[T, M, Seq]

  implicit class TableQuerySeqConversions(q: QuerySeq) {

    protected def selectInner[R](dbio: DBIO[Option[M]])(checks: Option[M] ⇒ Failures Xor M)(action: M ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      dbio.map(checks).flatMap {
        case Xor.Right(value) ⇒ action(value)
        case failures @ Xor.Left(_) ⇒ lift(failures)
      }.transactionally.run()
    }

    def selectOne[R](action: M ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(q.result.headOption)(selectOneResultChecks)(action)
    }

    def selectOneForUpdate[R](action: M ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(appendForUpdate(q.result.headOption))(selectOneResultChecks)(action)
    }

    def select[R](action: Seq[M] ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      q.result.flatMap(action).transactionally.run()
    }

    def selectForUpdate[R](action: Seq[M] ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      appendForUpdate(q.result).flatMap(action).transactionally.run()
    }

    protected def selectOneResultChecks(maybe: Option[M])
      (implicit ec: ExecutionContext, db: Database): Xor[Failures, M] = {
      Xor.fromOption(maybe, NotFoundFailure("Not found").single)
    }
  }
}

abstract class TableQueryWithLock[M <: ModelWithLockParameter, T <: GenericTable.TableWithLock[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQueryWithId[M, T](idLens)(construct) {

  implicit class TableWithLockQuerySeqConversions(q: QuerySeq) extends TableQuerySeqConversions(q) {

    def selectOneForUpdateIgnoringLock[R](action: M ⇒ DbResult[R])
      (implicit ec: ExecutionContext, db: Database): Result[R] = {
      selectInner(appendForUpdate(q.result.headOption))(super.selectOneResultChecks)(action)
    }

    override def selectOneResultChecks(maybe: Option[M])
      (implicit ec: ExecutionContext, db: Database): Xor[Failures, M] = {
      maybe match {
        case Some(lockable) if lockable.locked ⇒
          Xor.left(GeneralFailure(s"Model is locked").single)
        case Some(lockable) if !lockable.locked ⇒
          Xor.right(lockable)
        case None ⇒
          Xor.left(NotFoundFailure("Not found").single)
      }
    }
  }

}
