package services.taxonomy

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.{Failure, TaxonomyFailures}
import failures.TaxonomyFailures._
import models.objects._
import models.taxonomy._
import models.taxonomy.TaxonomyTaxonLinks.scope._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import services.objects.ObjectManager
import utils.Validation
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

object TaxonomyManager {

  case class MoveSpec(taxon: TaxonomyTaxonLink,
                      parent: Option[TaxonomyTaxonLink],
                      sibling: Option[TaxonomyTaxonLink])
      extends Validation[MoveSpec] {

    def fillLinkWithNewPath: MoveSpec = copy(taxon = taxon.copy(path = newPath))

    def isMoveRequired: Boolean =
      taxon.path != newPath || (sibling.isDefined && sibling.get.position + 1 != taxon.position)

    override def validate: ValidatedNel[Failure, MoveSpec] = {

      def validateSameTaxonomy: ValidatedNel[Failure, Unit] =
        Validation.validExpr(taxon.taxonomyId == parent
                               .orElse(sibling)
                               .map(_.taxonomyId)
                               .getOrElse(taxon.taxonomyId),
                             "taxon should be in the same taxonomy as parent (sibling)")

      def validateParentToChildMove: ValidatedNel[Failure, Unit] = {
        Validation.validExpr(
            !isMoveRequired || taxon.id == 0 || !newPath.value.startsWith(taxon.childPath.value),
            CannotMoveParentTaxonUnderChild.description)
      }

      (validateSameTaxonomy |@| validateParentToChildMove).map {
        case _ ⇒ this
      }
    }

    val newPath: LTree =
      parent.map(_.childPath).orElse(sibling.map(_.path)).getOrElse(LTree(List()))
  }

  def getTaxonomy(taxonomyFormId: ObjectForm#Id)(implicit ec: EC,
                                                 oc: OC,
                                                 db: DB): DbResultT[TaxonomyResponse] =
    for {
      taxonomy ← * <~ ObjectUtils.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      taxons   ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(taxonomy.model)
    } yield TaxonomyResponse.build(taxonomy, taxons)

  def createTaxonomy(payload: CreateTaxonomyPayload)(implicit ec: EC,
                                                     oc: OC): DbResultT[TaxonomyResponse] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxonomy ← * <~ Taxonomies.create(
                    Taxonomy(hierarchical = payload.hierarchical,
                             contextId = oc.id,
                             formId = ins.form.id,
                             shadowId = ins.shadow.id,
                             commitId = ins.commit.id))
    } yield TaxonomyResponse.build(FullObject(taxonomy, ins.form, ins.shadow), Seq())
  }

  def updateTaxonomy(taxonomyFormId: ObjectForm#Id, payload: UpdateTaxonomyPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[TaxonomyResponse] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxonomy ← * <~ ObjectUtils.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      _        ← * <~ failIf(taxonomy.model.archivedAt.isDefined, TaxonomyIsArchived(taxonomyFormId))
      newTaxonomy ← * <~ ObjectUtils.commitUpdate(
                       taxonomy,
                       form.attributes,
                       taxonomy.shadow.attributes.merge(shadow.attributes),
                       Taxonomies.updateHead)
      taxons ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(newTaxonomy.model)
    } yield TaxonomyResponse.build(newTaxonomy, taxons)
  }

  def archiveByContextAndId(
      taxonomyFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonomyFormId)
      archivedAt = Some(Instant.now)
      archived ← * <~ Taxonomies.update(taxonomy, taxonomy.copy(archivedAt = archivedAt))
      allLinks ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(taxonomy)
      _ ← * <~ allLinks.map {
           case (_, link) ⇒ TaxonomyTaxonLinks.update(link, link.copy(archivedAt = archivedAt))
         }
      _ ← * <~ allLinks.map { case (taxon, _) ⇒ taxon.model }.distinct.map(taxon ⇒
               Taxons.update(taxon, taxon.copy(archivedAt = archivedAt)))
    } yield {}

  def getTaxon(
      taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[TaxonResponse] =
    ObjectUtils.getFullObject(Taxons.mustFindByFormId404(taxonFormId)).map(TaxonResponse.build)

  def createTaxon(taxonFormId: ObjectForm#Id, payload: CreateTaxonPayload)(
      implicit ec: EC,
      oc: OC): DbResultT[TaxonResponse] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      _        ← * <~ payload.validate
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonFormId)

      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxon ← * <~ Taxons.create(
                 Taxon(contextId = oc.id,
                       shadowId = ins.form.id,
                       formId = ins.shadow.id,
                       commitId = ins.commit.id))

      parentLink ← * <~ payload.parent.fold(DbResultT.none[TaxonomyTaxonLink])(
                      parentId ⇒
                        TaxonomyTaxonLinks
                          .active()
                          .mustFindByTaxonomyAndTaxonFormId(taxonomy, parentId)
                          .map(Some(_)))
      siblingLink ← * <~ payload.sibling.fold(DbResultT.none[TaxonomyTaxonLink])(
                       siblingId ⇒
                         TaxonomyTaxonLinks
                           .active()
                           .mustFindByTaxonomyAndTaxonFormId(taxonomy, siblingId)
                           .map(Some(_)))

      index ← * <~ TaxonomyTaxonLinks.nextIndex(taxonomy.id).result
      moveSpec ← * <~ MoveSpec(TaxonomyTaxonLink(index = index,
                                                 taxonomyId = taxonomy.id,
                                                 taxonId = taxon.id,
                                                 position = -1,
                                                 path = LTree("")),
                               parentLink,
                               siblingLink).validate.map(_.fillLinkWithNewPath)
      link ← * <~ TaxonomyTaxonLinks.create(moveSpec.taxon)
      _ ← * <~ moveSpec.sibling.fold(DbResultT.good(link))(sibling ⇒
               TaxonomyTaxonLinks.moveTaxonAfter(link, sibling))
    } yield TaxonResponse.build(FullObject(taxon, ins.form, ins.shadow))
  }

  def mustFindSingleTaxonomyForTaxon(taxon: Taxon)(implicit ec: EC,
                                                   db: DB): DbResultT[FullObject[Taxonomy]] =
    for {
      taxonomies ← * <~ TaxonomyTaxonLinks.queryLeftByRight(taxon)
      taxonomy ← * <~ (taxonomies.toList match {
                      case t :: Nil ⇒ DbResultT.good(t)
                      case _ ⇒
                        DbResultT.failure(InvalidTaxonomiesForTaxon(taxon, taxonomies.length))
                    })
    } yield taxonomy

  private def updateTaxonomyHierarchy(taxon: Taxon, parent: Option[Int], sibling: Option[Int])(
      implicit ec: EC,
      db: DB,
      oc: OC) =
    for {
      taxonomy ← * <~ mustFindSingleTaxonomyForTaxon(taxon)
      parentTaxon ← * <~ parent.fold(DbResultT.none[TaxonomyTaxonLink])(
                       parent ⇒
                         TaxonomyTaxonLinks
                           .active()
                           .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, parent)
                           .map(Some(_)))
      siblingTaxon ← * <~ sibling.fold(DbResultT.none[TaxonomyTaxonLink])(
                        sibling ⇒
                          TaxonomyTaxonLinks
                            .active()
                            .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, sibling)
                            .map(Some(_)))
      link ← * <~ TaxonomyTaxonLinks
              .active()
              .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, taxon.formId)
      moveSpec ← * <~ MoveSpec(link, parentTaxon, siblingTaxon).validate
      _        ← * <~ doOrMeh(moveSpec.isMoveRequired, moveTaxon(taxonomy.model, moveSpec))
    } yield taxonomy

  def updateTaxon(taxonId: Int, payload: UpdateTaxonPayload)(implicit ec: EC,
                                                             oc: OC,
                                                             db: DB): DbResultT[TaxonResponse] = {
    for {
      _     ← * <~ payload.validate
      taxon ← * <~ Taxons.mustFindByFormId404(taxonId)
      _     ← * <~ failIf(taxon.archivedAt.isDefined, TaxonIsArchived(taxonId))
      newTaxon ← * <~ (payload.attributes match {
                      case Some(attributes) ⇒ updateTaxonAttributes(taxon, attributes)
                      case _                ⇒ DbResultT.good(taxon)
                    })
      _        ← * <~ updateTaxonomyHierarchy(taxon, payload.parent, payload.sibling)
      response ← * <~ ObjectManager.getFullObject(DbResultT.good(newTaxon))
    } yield TaxonResponse.build(response)
  }

  private def moveTaxon(taxon: Taxonomy, moveSpec: MoveSpec)(implicit ec: EC, oc: OC, db: DB) =
    for {
      _ ← * <~ TaxonomyTaxonLinks.archive(moveSpec.taxon)
      newPath = moveSpec.newPath
      newLink ← * <~ TaxonomyTaxonLinks.create(
                   moveSpec.taxon.copy(id = 0, position = -1, path = newPath))
      _ ← * <~ TaxonomyTaxonLinks.updatePath(
             taxon.id,
             moveSpec.taxon.childPath,
             newPath.copy(value = newPath.value ::: List(moveSpec.taxon.index.toString)))
      _ ← * <~ moveSpec.sibling.fold(DbResultT.unit)(sibling ⇒
               TaxonomyTaxonLinks.moveTaxonAfter(newLink, sibling).meh)
    } yield {}

  private def updateTaxonAttributes(taxon: Taxon, newAttributes: Map[String, Json])(
      implicit ec: EC,
      db: DB,
      oc: OC): DbResultT[Taxon] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, newAttributes)
    val shadow = ObjectShadow.fromPayload(newAttributes)

    for {
      fullTaxon ← * <~ ObjectUtils.getFullObject(DbResultT.good(taxon))
      newTaxon ← * <~ ObjectUtils.commitUpdate(
                    fullTaxon,
                    form.attributes,
                    fullTaxon.shadow.attributes.merge(shadow.attributes),
                    Taxons.updateHead)
    } yield newTaxon.model
  }

  def archiveTaxonByContextAndId(
      taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      taxon    ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      _        ← * <~ Taxons.update(taxon, taxon.copy(archivedAt = Some(Instant.now)))
      links    ← * <~ TaxonomyTaxonLinks.filterByTaxonFormId(taxonFormId).result
      children ← * <~ links.map(link ⇒ TaxonomyTaxonLinks.hasChildren(link).result.dbresult)
      _ ← * <~ failIf(children.exists(identity),
                      TaxonomyFailures.CannotArchiveParentTaxon(taxonFormId))
      _ ← * <~ links.map(TaxonomyTaxonLinks.archive)
    } yield {}
}
