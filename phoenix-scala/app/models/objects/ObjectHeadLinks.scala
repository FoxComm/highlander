package models.objects

import java.time.Instant

import services.objects.ObjectManager
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

    type QueryFilter = QuerySeq ⇒ QuerySeq

    def filterLeft(left: L): QuerySeq = filterLeftId(left.id)

    protected def filterLeftId(leftId: Int): QuerySeq = filter(_.leftId === leftId)

    def filterRight(right: R): QuerySeq = filterRightId(right.id)

    protected def filterRightId(rightId: Int): QuerySeq = filter(_.rightId === rightId)

    def queryLeftByRight(right: R)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[L]]] =
      queryLeftByRightIdWithLink(right.id).map(_.map(_._1))

    def queryRightByLeft(left: L, customFilter: QueryFilter = identity)(
        implicit ec: EC,
        db: DB): DbResultT[Seq[FullObject[R]]] =
      queryRightByLeftIdWithLink(left.id, customFilter).map(_.map(_._1))

    def queryRightByLeftWithLinks(left: L, customFilter: QueryFilter = identity)(
        implicit ec: EC,
        db: DB): DbResultT[Seq[(FullObject[R], M)]] =
      queryRightByLeftIdWithLink(left.id, customFilter)

    private def queryRightByLeftIdWithLink(leftId: Int, customFilter: QueryFilter)(
        implicit ec: EC,
        db: DB): DbResultT[Seq[(FullObject[R], M)]] =
      for {
        links         ← * <~ customFilter(filterLeftId(leftId)).result
        linkedObjects ← * <~ links.map(link ⇒ queryLinkedObject(link))
      } yield linkedObjects.zip(links)

    private def queryLeftByRightIdWithLink(
        rightId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[(FullObject[L], M)]] =
      for {
        links         ← * <~ filterRightId(rightId).result
        linkedObjects ← * <~ links.map(queryLeftLinkedObject)
      } yield linkedObjects.zip(links)

    def queryLinkedObject(link: M)(implicit ec: EC, db: DB): DbResultT[FullObject[R]] =
      ObjectManager.getFullObject(rightQuery.mustFindById404(link.rightId))

    def queryLeftLinkedObject(link: M)(implicit ec: EC, db: DB): DbResultT[FullObject[L]] =
      ObjectManager.getFullObject(leftQuery.mustFindById404(link.leftId))

    def ensureLinked(left: L, right: Seq[R])(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        existingLinks ← * <~ filterLeft(left).result
        _             ← * <~ linkAllExceptExisting(left, right, existingLinks)
      } yield {}

    def linkAllExceptExisting(left: L, rightObjects: Seq[R], existingLinks: Seq[M])(
        implicit ec: EC,
        db: DB): DbResultT[Unit] = {
      val linkedRightIds = existingLinks.map(_.rightId)
      val newLinks = rightObjects
        .filterNot(right ⇒ linkedRightIds.contains(right.id))
        .map(right ⇒ create(build(left, right)))
      DbResultT.sequence(newLinks).map(_ ⇒ {})
    }

    def unlinkAllExcept(left: L, rights: Seq[R])(implicit ec: EC, db: DB): DbResultT[Unit] =
      filterLeft(left).filterNot(_.rightId inSet rights.map(_.id)).delete.map(_ ⇒ {}).dbresult

    def syncLinks(left: L, rights: Seq[R])(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        _ ← * <~ unlinkAllExcept(left, rights)
        _ ← * <~ ensureLinked(left, rights)
      } yield {}

    def createIfNotExist(left: L, right: R)(implicit ec: EC, db: DB): DbResultT[Unit] =
      for {
        linkExists ← * <~ filterLeft(left).filter(_.rightId === right.id).exists.result
        _          ← * <~ doOrMeh(!linkExists, create(build(left, right)))
      } yield {}

    def build(left: L, right: R): M
  }
}
