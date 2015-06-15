package utils

import monocle.Lens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._

import scala.concurrent.{Future, ExecutionContext}

trait ModelWithIdParameter {
  type Id = Int
}

trait TableWithIdColumn[I] {
  def id: Rep[I]
}

private[utils] abstract class TableWithIdInternal[M <: ModelWithIdParameter, I](tag: Tag, name: String)
  extends Table[M](tag, name) with TableWithIdColumn[I]

object GenericTable {
  /** This allows us to enforce that tables have the same ID column as their case class. */
  type TableWithId[MODEL <: ModelWithIdParameter] = TableWithIdInternal[MODEL, MODEL#Id]
}

abstract class TableQueryWithId[M <: ModelWithIdParameter, T <: GenericTable.TableWithId[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) {

  val returningId =
    this.returning(map(_.id))

  def _findById(i: M#Id): Query[T, M, Seq] = filter(_.id === i)

  def findById(i: M#Id)(implicit db: Database, ec: ExecutionContext): Future[Option[M]] =
    db.run(_findById(i).result.headOption)

  def save(model: M)(implicit db: Database, ec: ExecutionContext): Future[M] = db.run(for {
    id ← returningId += model
  } yield idLens.set(id)(model))

  def deleteById(i: M#Id)(implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(_findById(i).delete)
}

