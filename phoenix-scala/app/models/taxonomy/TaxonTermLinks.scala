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

trait TermPosition {
  def taxonomyId: Int
  def position: Int
  def path: LTree
}

case class TaxonTermLink(id: Int = 0,
                         index: Int,
                         taxonomyId: Int,
                         taxonomyTermId: Int,
                         position: Int,
                         path: LTree,
                         updatedAt: Instant = Instant.now,
                         createdAt: Instant = Instant.now,
                         archivedAt: Option[Instant] = None)
    extends FoxModel[TaxonTermLink]
    with Validation[TaxonTermLink]
    with TermPosition
    with ObjectHeadLink[TaxonTermLink] {
  override def leftId: Id = taxonomyId

  override def rightId: Id = taxonomyTermId

  def childPath: LTree = LTree(path.value ::: List(index.toString))
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

  private def shiftPositions(taxonId: Int, path: LTree, position: Int): DBIO[Int] =
    sqlu"update taxonomy_term_links set position = position + 1 where taxonomy_id = $taxonId and path ~ ${path.toString} and position >= $position"

  private def shrinkPositions(taxonId: Int, path: LTree, position: Int): DBIO[Int] =
    sqlu"update taxonomy_term_links set position = position - 1 where taxonomy_id = $taxonId and path ~ ${path.toString} and position >= $position"

  def moveTermAfter(link: TaxonTermLink, after: TaxonTermLink)(
      implicit ec: EC): DbResultT[TaxonTermLink] = {
    require(link.id != after.id)
    require(link.position < 0)
    require(after.position >= 0)
    require(link.path == after.path)

    val newPosition: Int = after.position + 1
    for {
      _       ← * <~ shiftPositions(link.taxonomyId, link.path, newPosition)
      newLink ← * <~ TaxonTermLinks.update(link, link.copy(position = newPosition))
      _ = assert(newLink.id == link.id && newLink.position == after.position + 1)
    } yield newLink
  }

  def archivate(link: TaxonTermLink)(implicit ec: EC): DbResultT[Unit] =
    for {
      _ ← * <~ TaxonTermLinks.update(link.copy(archivedAt = Some(Instant.now)))
      _ ← * <~ shrinkPositions(link.taxonomyId, link.path, link.position)
    } yield {}

  def updatePath(taxonId: Int, oldPrefix: LTree, newPrefix: LTree): DBIO[Int] = {
    val patternLength = oldPrefix.value.length
    sqlu"""UPDATE taxonomy_term_links AS t
             SET path = (text2ltree(${newPrefix.toString}) || subpath(t.path, $patternLength))
             WHERE t.taxonomy_id=$taxonId AND t.path <@= text2ltree(${oldPrefix.toString})"""
  }

  def getNextPosition(taxonId: Int, path: LTree): Rep[Int] =
    filter(link ⇒ link.taxonId === taxonId && (link.path ~ path.toString()))
      .map(_.position)
      .max
      .map(_ + 1)
      .getOrElse(0)

  def mustFindByTaxonAndTermFormId(taxon: Taxon, termFormId: ObjectForm#Id)(implicit oc: OC,
    ec: EC) =
    filterByTaxonAndTermFormId(taxon.id, termFormId)
      .mustFindOneOr(NoTermInTaxonomy(taxon.formId, termFormId))

  def nextIndex(taxonId: Taxon#Id)(implicit ec: EC): Rep[Int] =
    filter(_.taxonId === taxonId).map(_.index).max.map(_ + 1).getOrElse(0)

  def filterByTermFormId(termFormId: ObjectForm#Id)(implicit oc: OC): QuerySeq =
    join(Terms).on { case (link, term) ⇒ link.taxonTermId === term.id }.filter {
      case (_, term) ⇒
        term.formId ===
          termFormId && term.contextId === oc.id
    }.map { case (link, _) ⇒ link }

  def filterByTaxonAndTermFormId(taxonId: Int, termFormId: ObjectForm#Id)(
      implicit oc: OC): QuerySeq =
    filterByTermFormId(termFormId).filter(_.taxonId === taxonId)

  def build(left: Taxon, right: Term): TaxonTermLink =
    TaxonTermLink(0, 0, left.id, right.id, 0, LTree(""))
}
