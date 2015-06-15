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
  type TableWithId[MODEL <: ModelWithIdParameter] = TableWithIdInternal[MODEL, MODEL#Id]
}

abstract class TableQueryWithId[M <: ModelWithIdParameter, T <: GenericTable.TableWithId[M]]
  (idLens: Lens[M, M#Id])
  (construct: Tag ⇒ T)
  (implicit ev: BaseTypedType[M#Id]) extends TableQuery[T](construct) {

  val byId = for {
    id     ← Parameters[M#Id]
    entity ← filter(_.id === id)
  } yield entity

  def _findById(i: M#Id) = filter(_.id === i)

  def findById(i: M#Id)(implicit db: Database, ec: ExecutionContext): Future[Option[M]] =
    db.run(_findById(i).result.headOption)

  val returningId =
    this.returning(map(_.id))

  def save(model: M)(implicit ec: ExecutionContext): DBIO[M] = for {
    id ← returningId += model
  } yield idLens.set(id)(model)


  def deleteById(i: M#Id): DBIO[Int] =
    byId(i).delete
}

