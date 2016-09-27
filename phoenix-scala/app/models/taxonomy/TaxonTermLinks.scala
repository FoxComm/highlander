package models.taxonomy

import java.time.Instant

import _root_.utils.aliases.{EC, OC}
import failures.TaxonomyFailures.NoTermInTaxonomy
import models.objects.ObjectForm
import models.objects.ObjectHeadLinks._
import shapeless._
import utils.db.ExPostgresDriver.api._
import slick.lifted.Tag
import utils.Validation
import utils.db._
import com.github.tminglei.slickpg.LTree

case class TaxonTermLink(id: Int = 0,
                         index: Int,
                         taxonId: Int,
                         taxonTermId: Int,
                         position: Int,
                         path: LTree,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[TaxonTermLink]
    with Validation[TaxonTermLink]
    with ObjectHeadLink[TaxonTermLink] {
  override def leftId: Id = taxonId

  override def rightId: Id = taxonTermId

  def childPath: LTree =
    if (path.value == List("")) LTree(List(index.toString))
    else LTree(path.value ::: List(index.toString))

  def parentIndex: Option[Int] = path.value.lastOption.filter(!_.isEmpty).map(_.toInt)
}

class TaxonTermLinks(tag: Tag) extends ObjectHeadLinks[TaxonTermLink](tag, "taxon_term_links") {

  def archivedAt  = column[Option[Instant]]("archived_at")
  def index       = column[Int]("index")
  def position    = column[Int]("position")
  def path        = column[LTree]("path")
  def taxonId     = column[Int]("taxon_id")
  def taxonTermId = column[Int]("taxon_term_id")

  override def leftId  = taxonId
  override def rightId = taxonTermId

  def * =
    (id, index, taxonId, taxonTermId, position, path, updatedAt, createdAt, archivedAt) <> ((TaxonTermLink.apply _).tupled, TaxonTermLink.unapply)
}

object TaxonTermLinks
    extends ObjectHeadLinkQueries[TaxonTermLink, TaxonTermLinks, Taxon, Term](
        new TaxonTermLinks(_),
        Taxons,
        Terms)
    with ReturningId[TaxonTermLink, TaxonTermLinks] {

  val returningLens: Lens[TaxonTermLink, Int] = lens[TaxonTermLink].id

  private def shiftPositions(taxonId: Int, path: LTree, position: Int): DBIO[Int] = {
    val pathString = path.toString()
    sqlu"UPDATE taxon_term_links SET position = position + 1 WHERE taxon_id = $taxonId AND path=text2ltree($pathString) AND position >= $position and archived_at is null"
  }

  private def shrinkPositions(taxonId: Int, path: LTree, position: Int): DBIO[Int] = {
    val pathString = path.toString()
    sqlu"update taxon_term_links set position = position - 1 where taxon_id = $taxonId and path=text2ltree($pathString) and position >= $position and archived_at is null"
  }

  def moveTermAfter(link: TaxonTermLink, after: TaxonTermLink)(
      implicit ec: EC): DbResultT[TaxonTermLink] = {
    require(link.id != after.id)
    require(link.position < 0)
    require(after.position >= 0)
    require(link.parentIndex == after.parentIndex)

    val newPosition: Int = after.position + 1
    for {
      _       ← * <~ shiftPositions(link.taxonId, link.path, newPosition)
      newLink ← * <~ TaxonTermLinks.update(link, link.copy(position = newPosition))
      _ = assert(newLink.id == link.id && newLink.position == after.position + 1)
    } yield newLink
  }

  def archivate(link: TaxonTermLink)(implicit ec: EC): DbResultT[Unit] =
    for {
      _ ← * <~ TaxonTermLinks.update(link, link.copy(archivedAt = Some(Instant.now)))
      _ ← * <~ shrinkPositions(link.taxonId, link.path, link.position)
    } yield {}

  def updatePath(taxonId: Int, oldPrefix: LTree, newPrefix: LTree): DBIO[Int] = {
    val patternLength = oldPrefix.value.length
    sqlu"""UPDATE taxon_term_links AS t
             SET path = (text2ltree(${newPrefix.toString}) || subpath(t.path, $patternLength))
             WHERE t.taxon_id=$taxonId
             AND (t.path <@ text2ltree(${oldPrefix.toString})
             and not t.path = text2ltree(${oldPrefix.toString}))
             and t.archived_at is null""".andThen {
      sqlu"""UPDATE taxon_term_links AS t
             SET path = text2ltree(${newPrefix.toString})
             WHERE t.taxon_id=$taxonId
             AND t.path = text2ltree(${oldPrefix.toString})
             and t.archived_at is null"""
    }
  }

  def nonArchived: QuerySeq = filter(_.archivedAt.isEmpty)

  def getNextPosition(taxonId: Int, path: LTree): Rep[Int] =
    nonArchived
      .filter(link ⇒ link.taxonId === taxonId && (link.path ~ path.toString()))
      .map(_.position)
      .max
      .map(_ + 1)
      .getOrElse(0)

  def mustFindByTaxonAndTermFormId(taxon: Taxon, termFormId: ObjectForm#Id)(implicit oc: OC,
                                                                            ec: EC) =
    filterByTaxonAndTermFormId(taxon.id, termFormId)
      .mustFindOneOr(NoTermInTaxonomy(taxon.formId, termFormId))

  def nextIndex(taxonId: Taxon#Id)(implicit ec: EC): Rep[Int] =
    nonArchived.filter(_.taxonId === taxonId).map(_.index).max.map(_ + 1).getOrElse(0)

  def filterByTermFormId(termFormId: ObjectForm#Id)(implicit oc: OC): QuerySeq =
    nonArchived
      .join(Terms)
      .on { case (link, term) ⇒ link.taxonTermId === term.id }
      .filter {
        case (_, term) ⇒
          term.formId ===
            termFormId && term.contextId === oc.id
      }
      .map { case (link, _) ⇒ link }

  def filterByTaxonAndTermFormId(taxonId: Int, termFormId: ObjectForm#Id)(
      implicit oc: OC): QuerySeq =
    filterByTermFormId(termFormId).filter(_.taxonId === taxonId)

  def build(left: Taxon, right: Term): TaxonTermLink =
    TaxonTermLink(0, 0, left.id, right.id, 0, LTree(""))

  override protected def filterLeftId(leftId: Int): TaxonTermLinks.QuerySeq =
    super.filterLeftId(leftId).filter(_.archivedAt.isEmpty)

  override protected def filterRightId(rightId: Int): TaxonTermLinks.QuerySeq =
    super.filterRightId(rightId).filter(_.archivedAt.isEmpty)
}
