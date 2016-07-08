package models.objects

import java.time.Instant

import utils.db.ExPostgresDriver.api._
import utils.db._

object ObjectHeadLinks {

  trait ObjectHeadLink[M <: ObjectHeadLink[M]] extends FoxModel[M] { self: M ⇒
    def leftId: Int
    def rightId: Int
    def createdAt: Instant
    def updatedAt: Instant
  }

  abstract class ObjectHeadLinks[M <: ObjectHeadLink[M]](tag: Tag, table: String)
      extends FoxTable[M](tag, table) {

    def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def leftId    = column[Int]("left_id")
    def rightId   = column[Int]("right_id")
    def createdAt = column[Instant]("created_at")
    def updatedAt = column[Instant]("updated_at")
  }

  abstract class ObjectHeadLinkQueries[M <: ObjectHeadLink[M], T <: ObjectHeadLinks[M]](
      construct: Tag ⇒ T)
      extends FoxTableQuery[M, T](construct) {

    def filterLeft(leftId: Int): QuerySeq       = filter(_.leftId === leftId)
    def filterLeft(leftIds: Seq[Int]): QuerySeq = filter(_.leftId.inSet(leftIds))
  }
}
