package objectframework.models

import java.time.Instant

import core.db.ExPostgresDriver.api._
import core.db._
import objectframework.services.ObjectManager

object ObjectHeadLinks {

  trait ObjectHeadLink[M <: ObjectHeadLink[M]] extends FoxModel[M] { self: M ⇒
    def leftId: Int
    def rightId: Int
    def createdAt: Instant
    def updatedAt: Instant
    def archivedAt: Option[Instant]
  }

  abstract class ObjectHeadLinks[M <: ObjectHeadLink[M]](tag: Tag, table: String)
      extends FoxTable[M](tag, table) {

    def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def leftId     = column[Int]("left_id")
    def rightId    = column[Int]("right_id")
    def createdAt  = column[Instant]("created_at")
    def updatedAt  = column[Instant]("updated_at")
    def archivedAt = column[Option[Instant]]("archived_at")
  }

  abstract class ObjectHeadLinkQueries[M <: ObjectHeadLink[M],
                                       T <: ObjectHeadLinks[M],
                                       L <: ObjectHead[L],
                                       R <: ObjectHead[R]](construct: Tag ⇒ T,
                                                           leftQuery: FoxTableQuery[L, _],
                                                           rightQuery: FoxTableQuery[R, _])
      extends FoxTableQuery[M, T](construct) {

    def filterLeft(left: L): QuerySeq = filterLeftId(left.id)

    protected def filterLeftId(leftId: Int): QuerySeq =
      filter(l ⇒ l.leftId === leftId && l.archivedAt.isEmpty)

    def filterRight(right: R): QuerySeq = filterRightId(right.id)

    protected def filterRightId(rightId: Int): QuerySeq =
      filter(r ⇒ r.rightId === rightId && r.archivedAt.isEmpty)

    def queryLeftByRight(right: R)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[L]]] =
      queryLeftByRightIdWithLink(right.id).map(_.map(_._1))

    def queryRightByLeft(left: L)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[R]]] =
      queryRightByLeftIdWithLink(left.id).map(_.map(_._1))

    def queryRightByLeftWithLinks(left: L)(implicit ec: EC, db: DB): DbResultT[Seq[(FullObject[R], M)]] =
      queryRightByLeftIdWithLink(left.id)

    private def queryRightByLeftIdWithLink(leftId: Int)(implicit ec: EC,
                                                        db: DB): DbResultT[Seq[(FullObject[R], M)]] =
      for {
        links         ← * <~ filterLeftId(leftId).result
        linkedObjects ← * <~ links.map(link ⇒ queryLinkedObject(link))
      } yield linkedObjects.zip(links)

    private def queryLeftByRightIdWithLink(rightId: Int)(implicit ec: EC,
                                                         db: DB): DbResultT[Seq[(FullObject[L], M)]] =
      for {
        links         ← * <~ filterRightId(rightId).result
        linkedObjects ← * <~ links.map(queryLeftLinkedObject)
      } yield linkedObjects.zip(links)

    def queryLinkedObject(link: M)(implicit ec: EC, db: DB): DbResultT[FullObject[R]] =
      ObjectManager.getFullObject(rightQuery.mustFindById404(link.rightId))

    def queryLeftLinkedObject(link: M)(implicit ec: EC, db: DB): DbResultT[FullObject[L]] =
      ObjectManager.getFullObject(leftQuery.mustFindById404(link.leftId))

    def syncLinks(left: L, rights: Seq[R])(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        _             ← * <~ filterLeft(left).filter(!_.rightId.inSet(rights.map(_.id))).delete
        existingLinks ← * <~ filterLeft(left).result
        linkedRightIds = existingLinks.map(_.rightId)
        _ ← * <~ rights.collect {
             case right if !linkedRightIds.contains(right.id) ⇒
               create(build(left, right))
           }
      } yield {}

    def createIfNotExist(left: L, right: R)(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        linkExists ← * <~ filterLeft(left).filter(_.rightId === right.id).exists.result
        _          ← * <~ doOrMeh(!linkExists, create(build(left, right)))
      } yield {}

    def build(left: L, right: R): M
  }
}
