package models.objects

import failures.ObjectFailures.{LinkAtPositionCannotBeFound, LinkCannotBeFound}
import models.objects.ObjectHeadLinks.{ObjectHeadLink, ObjectHeadLinkQueries, ObjectHeadLinks}
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

trait OrderedObjectHeadLink[M <: OrderedObjectHeadLink[M]] extends ObjectHeadLink[M] { self: M ⇒
  def position: Int
  def withPosition(newPosition: Int): M
}

abstract class OrderedObjectHeadLinks[M <: OrderedObjectHeadLink[M]](tag: Tag, table: String)
    extends ObjectHeadLinks[M](tag, table) {

  def position = column[Int]("position")
}

abstract class OrderedObjectHeadLinkQueries[M <: OrderedObjectHeadLink[M],
                                            T <: OrderedObjectHeadLinks[M],
                                            L <: ObjectHead[L],
                                            R <: ObjectHead[R]](
    construct: Tag ⇒ T,
    leftQuery: ObjectHeadsQueries[L, _],
    rightQuery: ObjectHeadsQueries[R, _])
    extends ObjectHeadLinkQueries[M, T, L, R](construct, leftQuery, rightQuery) {

  override def filterLeftId(leftId: Int): QuerySeq = super.filterLeftId(leftId).sortBy(_.position)

  def getNextPositionFor(left: L)(implicit ec: EC): DbResultT[Int] =
    filterLeft(left).map(_.position).max.map(_ + 1).getOrElse(0).result.dbresult

  def createLast(left: L, right: R)(implicit ec: EC): DbResultT[M] =
    for {
      nextPosition ← * <~ getNextPositionFor(left)
      link         ← * <~ create(buildOrdered(left, right, nextPosition))
    } yield link

  def updatePosition(left: L, right: R, newPosition: Int)(implicit ec: EC): DbResultT[M] = {
    val allLefts = filterLeft(left)
    for {
      link ← * <~ allLefts
              .filter(_.rightId === right.id)
              .mustFindOneOr(LinkCannotBeFound(baseTableRow.getClass, left.id, right.id))
      replacedLink ← * <~ allLefts
                      .filter(_.position === newPosition)
                      .mustFindOneOr(
                          LinkAtPositionCannotBeFound(baseTableRow.getClass, left.id, newPosition))
      newLinks ← * <~ (if (link.position == newPosition) DbResultT.good((link, replacedLink))
                       else swapLinkPositions(link, replacedLink))
      (updatedLink, _) = newLinks
    } yield updatedLink
  }

  private def swapLinkPositions(link1: M, link2: M)(implicit ec: EC): DbResultT[(M, M)] =
    for {
      newLink1 ← * <~ update(link1, link1.withPosition(link2.position))
      newLink2 ← * <~ update(link2, link2.withPosition(link1.position))
    } yield (newLink1, newLink2)

  def buildOrdered(left: L, right: R, position: Int): M

  def build(left: L, right: R) = buildOrdered(left, right, 0)
}
