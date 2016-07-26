package models.objects

import java.time.Instant

import utils.aliases._
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

  abstract class ObjectHeadLinkQueries[M <: ObjectHeadLink[M],
                                       T <: ObjectHeadLinks[M],
                                       L <: ObjectHead[L],
                                       R <: ObjectHead[R]](construct: Tag ⇒ T,
                                                           leftQuery: FoxTableQuery[L, _],
                                                           rightQuery: FoxTableQuery[R, _])
      extends FoxTableQuery[M, T](construct) {

    def filterLeft(left: L): QuerySeq               = filterLeftId(left.id)
    private def filterLeftId(leftId: Int): QuerySeq = filter(_.leftId === leftId)

    def queryRightByLeft(left: L)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[R]]] =
      rightByLeftId(left.id, rightQuery.mustFindById404)

    def queryRightByLeftId(leftId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[R]]] =
      rightByLeftId(leftId, rightQuery.mustFindById404)

    private def rightByLeftId[J <: ObjectHead[J]](leftId: Int, readHead: (Int) ⇒ DbResultT[J])(
        implicit ec: EC,
        db: DB): DbResultT[Seq[FullObject[J]]] =
      for {
        links         ← * <~ filterLeftId(leftId).result
        linkedObjects ← * <~ links.map(link ⇒ ObjectUtils.getFullObject(readHead(link.rightId)))
      } yield linkedObjects

    def syncLinks(left: L, rights: Seq[R])(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        _             ← * <~ filterLeft(left).filter(!_.rightId.inSet(rights.map(_.id))).delete
        existingLinks ← * <~ filterLeft(left).result
        linkedRightIds = existingLinks.map(_.rightId)
        _ ← * <~ rights.collect {
             case right if !linkedRightIds.contains(right.id) ⇒
               create(mkLink(left, right))
           }
      } yield {}

    def mkLink(left: L, right: R): M
  }
}
