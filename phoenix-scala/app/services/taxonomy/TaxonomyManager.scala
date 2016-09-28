package services.taxonomy

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.Failure
import failures.TaxonomyFailures._
import models.objects._
import models.taxonomy._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import services.objects.ObjectManager
import utils.Validation
import utils.aliases.{DB, EC, Json, OC}
import utils.db.ExPostgresDriver.api._
import utils.db._

object TaxonomyManager {

  case class MoveSpec(taxon: TaxonomyTaxonLink,
                      parent: Option[TaxonomyTaxonLink],
                      sibling: Option[TaxonomyTaxonLink])
      extends Validation[MoveSpec] {

    def moveRequired: Boolean =
      taxon.path != newPath || (sibling.isDefined && sibling.get.position + 1 != taxon.position)

    override def validate: ValidatedNel[Failure, MoveSpec] = {

      def validateSiblingOrParentDefined: ValidatedNel[Failure, Unit] = {
        Validation.validExpr(sibling.isEmpty || sibling.isDefined && parent.isEmpty,
                             "'parent' should be empty if 'sibling' is defined")
      }
      def validateSameTaxonomy: ValidatedNel[Failure, Unit] = {
        Validation.validExpr(taxon.taxonomyId == parent
                               .orElse(sibling)
                               .map(_.taxonomyId)
                               .getOrElse(taxon.taxonomyId),
                             "taxon should be in the same taxonomy as parent (sibling)")
      }
      (validateSiblingOrParentDefined |@| validateSameTaxonomy).map { case _ ⇒ this }
    }

    def newPath: LTree =
      parent.map(_.childPath).orElse(sibling.map(_.path)).getOrElse(LTree(List()))
  }

  def getTaxonomy(taxonomyFormId: ObjectForm#Id)(implicit ec: EC,
                                                 oc: OC,
                                                 db: DB): DbResultT[TaxonomyResponse.Root] =
    for {
      taxonomy ← * <~ ObjectUtils.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      taxons   ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(taxonomy.model)
    } yield TaxonomyResponse.build(taxonomy, taxons)

  def createTaxonomy(payload: CreateTaxonomyPayload)(implicit ec: EC,
                                                     oc: OC): DbResultT[TaxonomyResponse.Root] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxonomy ← * <~ Taxonomies.create(
                    Taxonomy(id = 0,
                             hierarchical = payload.hierarchical,
                             oc.id,
                             ins.form.id,
                             ins.shadow.id,
                             ins.commit.id))
    } yield TaxonomyResponse.build(FullObject(taxonomy, ins.form, ins.shadow), Seq())
  }

  def updateTaxonomy(taxonomyFormId: ObjectForm#Id, payload: UpdateTaxonomyPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[TaxonomyResponse.Root] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxonomy ← * <~ ObjectUtils.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
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
      //TODO: do we need to archivate ObjectForm too
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
      taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[TaxonResponse.Root] =
    ObjectUtils.getFullObject(Taxons.mustFindByFormId404(taxonFormId)).map(TaxonResponse.build)

  def createTaxon(taxonFormId: ObjectForm#Id, payload: CreateTaxonPayload)(
      implicit ec: EC,
      oc: OC): DbResultT[TaxonResponse.Root] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonFormId)

      ins   ← * <~ ObjectUtils.insert(form, shadow)
      taxon ← * <~ Taxons.create(Taxon(id = 0, oc.id, ins.form.id, ins.shadow.id, ins.commit.id))

      parentLink ← * <~ (if (payload.parent.isDefined)
                           TaxonomyTaxonLinks
                             .mustFindByTaxonomyAndTaxonFormId(taxonomy, payload.parent.get)
                             .map(link ⇒ Some(link))
                         else DbResultT.none)
      siblingLink ← * <~ (if (payload.sibling.isDefined)
                            TaxonomyTaxonLinks
                              .mustFindByTaxonomyAndTaxonFormId(taxonomy, payload.sibling.get)
                              .map(link ⇒ Some(link))
                          else DbResultT.none)

      index ← * <~ TaxonomyTaxonLinks.nextIndex(taxonomy.id).result
      moveSpec ← * <~ MoveSpec(TaxonomyTaxonLink(0, index, taxonomy.id, taxon.id, -1, LTree("")),
                               parentLink,
                               siblingLink).validate
      moveSpec2 = moveSpec.copy(taxon = moveSpec.taxon.copy(path = moveSpec.newPath))
      link ← * <~ TaxonomyTaxonLinks.create(moveSpec2.taxon)
      _ ← * <~ (if (moveSpec2.sibling.isDefined)
                  TaxonomyTaxonLinks.moveTaxonAfter(link, moveSpec2.sibling.get)
                else
                  DbResultT.good(link))
    } yield TaxonResponse.build(FullObject(taxon, ins.form, ins.shadow))
  }

  def mustFindSingleTaxonomyForTaxon(taxon: Taxon)(implicit ec: EC,
                                                   db: DB): DbResultT[FullObject[Taxonomy]] =
    for {
      taxonomies ← * <~ TaxonomyTaxonLinks.queryLeftByRight(taxon)
      taxonomy ← * <~ (if (taxonomies.length == 1) DbResultT.good(taxonomies.head)
                       else DbResultT.failure(InvalidTaxonomiesForTaxon(taxon, taxonomies.length)))
    } yield taxonomy

  def updateTaxonomyHierarchy(taxon: Taxon, parent: Option[Int], sibling: Option[Int])(
      implicit ec: EC,
      db: DB,
      oc: OC) =
    for {
      taxonomy ← * <~ mustFindSingleTaxonomyForTaxon(taxon)
      parentTaxon ← * <~ (if (parent.isDefined)
                            TaxonomyTaxonLinks
                              .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, parent.get)
                              .map(link ⇒ Some(link))
                          else DbResultT.none)
      siblingTerm ← * <~ (if (sibling.isDefined)
                            TaxonomyTaxonLinks
                              .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, sibling.get)
                              .map(link ⇒ Some(link))
                          else DbResultT.none)
      link     ← * <~ TaxonomyTaxonLinks.mustFindByTaxonomyAndTaxonFormId(taxonomy.model, taxon.formId)
      moveSpec ← * <~ MoveSpec(link, parentTaxon, siblingTerm).validate
      _ ← * <~ (if (moveSpec.moveRequired) moveTaxon(taxonomy.model, moveSpec)
                else DbResultT.good(Unit))
    } yield taxonomy

  private def moveTaxon(taxon: Taxonomy, moveSpec: MoveSpec)(implicit ec: EC, oc: OC, db: DB) =
    for {
      _       ← * <~ TaxonomyTaxonLinks.archivate(moveSpec.taxon)
      newPath ← * <~ moveSpec.newPath
      newLink ← * <~ TaxonomyTaxonLinks.create(
                   moveSpec.taxon.copy(id = 0, position = -1, path = newPath))
      _ ← * <~ TaxonomyTaxonLinks.updatePath(taxon.id,
                                             moveSpec.taxon.childPath,
                                             newPath.copy(value = newPath.value :::
                                                     List(moveSpec.taxon.index.toString)))
      _ ← * <~ (if (moveSpec.sibling.isDefined)
                  TaxonomyTaxonLinks.moveTaxonAfter(newLink, moveSpec.sibling.get)
                else
                  DbResultT.good(newLink))
    } yield {}

  def updateTaxon(taxonId: Int, payload: UpdateTaxonPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[TaxonResponse.Root] = {
    for {
      taxon ← * <~ Taxons.mustFindByFormId404(taxonId)
      newTaxon ← * <~ (payload.attributes match {
                      case Some(attributes) ⇒ updateTaxonAttributes(taxon, attributes)
                      case _                ⇒ DbResultT.good(taxon)
                    })

      _ ← * <~ updateTaxonomyHierarchy(taxon, payload.parent, payload.sibling)
      r ← * <~ ObjectManager.getFullObject(DbResultT.good(newTaxon))
    } yield TaxonResponse.build(r)
  }

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
      links ← * <~ TaxonomyTaxonLinks.filterByTaxonFormId(taxonFormId).result
      _     ← * <~ links.map(TaxonomyTaxonLinks.archivate)
    } yield {}
}
