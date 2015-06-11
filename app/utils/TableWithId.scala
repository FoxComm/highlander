package utils

import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext

abstract class TableWithId[M, I](tag: Tag, name: String) extends Table[M](tag, name) {
  def id: Rep[I]
}

/**
 * TODO: M and I should be well known and we should be able to infer them
 */
abstract class TableWithIdQuery[T <: TableWithId[M, I], M, I]
  (cons: Tag ⇒ T)
  (implicit ev: BaseTypedType[I]) extends TableQuery(cons) {

  def copyEntityId(m: M, id: I): M

  private val idQuery = map(_.id)
  private val byId    = this.findBy(_.id)

  val returningId = this.returning(idQuery)

  def findById(i: I): DBIO[Option[M]] =
    byId(i).result.headOption

  def insert(entity: M)
            (implicit ec: ExecutionContext): DBIO[M] =
    (returningId += entity).
      map(id ⇒ copyEntityId(entity, id))
}
