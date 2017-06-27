package phoenix.services.taxonomy

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.utils.Validation
import core.failures.Failure
import objectframework.ObjectUtils
import objectframework.models._
import objectframework.services.ObjectManager
import org.json4s.Formats
import phoenix.failures.TaxonomyFailures
import phoenix.failures.TaxonomyFailures._
import phoenix.models.account._
import phoenix.models.objects.ProductTaxonLinks
import phoenix.models.product.{Product, ProductReference, Products}
import phoenix.models.taxonomy.TaxonomyTaxonLinks.scope._
import phoenix.models.taxonomy._
import phoenix.payloads.TaxonPayloads._
import phoenix.payloads.TaxonomyPayloads._
import phoenix.responses.TaxonResponses._
import phoenix.responses.TaxonomyResponses._
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import core.db.ExPostgresDriver.api._
import core.db._

object TaxonomyManager {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  case class MoveSpec(taxon: TaxonomyTaxonLink, parent: Option[TaxonomyTaxonLink], newPosition: Option[Int])
      extends Validation[MoveSpec] {

    def fillLinkWithNewPath: MoveSpec = copy(taxon = taxon.copy(path = newPath))

    def isMoveRequired: Boolean =
      taxon.path != newPath || newPosition.fold(true)(_ != taxon.position)

    override def validate: ValidatedNel[Failure, MoveSpec] = {

      def validateSameTaxonomy: ValidatedNel[Failure, Unit] =
        Validation.validExpr(taxon.taxonomyId == parent.map(_.taxonomyId).getOrElse(taxon.taxonomyId),
                             TaxonomyFailures.TaxonomyShouldMatchForParentAndTarget.description)

      def validateParentToChildMove: ValidatedNel[Failure, Unit] =
        Validation.validExpr(
          !isMoveRequired || taxon.id == 0 || !newPath.value.startsWith(taxon.childPath.value),
          CannotMoveParentTaxonUnderChild.description)

      (validateSameTaxonomy |@| validateParentToChildMove).map {
        case _ ⇒ this
      }
    }

    val newPath: LTree = parent.map(_.childPath).getOrElse(LTree(List()))
  }

  def getTaxonomy(
      taxonomyFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[FullTaxonomyResponse] =
    for {
      taxonomy ← * <~ ObjectManager.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      taxons   ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(taxonomy.model)
    } yield FullTaxonomyResponse.build(taxonomy, taxons)

  def createTaxonomy(
      payload: CreateTaxonomyPayload)(implicit ec: EC, oc: OC, au: AU): DbResultT[FullTaxonomyResponse] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(form, shadow)
      taxonomy ← * <~ Taxonomies.create(
                  Taxonomy(hierarchical = payload.hierarchical,
                           scope = scope,
                           contextId = oc.id,
                           formId = ins.form.id,
                           shadowId = ins.shadow.id,
                           commitId = ins.commit.id))
    } yield FullTaxonomyResponse.build(FullObject(taxonomy, ins.form, ins.shadow), Seq())
  }

  def updateTaxonomy(
      taxonomyFormId: ObjectForm#Id,
      payload: UpdateTaxonomyPayload)(implicit ec: EC, oc: OC, db: DB): DbResultT[FullTaxonomyResponse] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxonomy ← * <~ ObjectManager.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      _        ← * <~ failIf(taxonomy.model.archivedAt.isDefined, TaxonomyIsArchived(taxonomyFormId))
      newTaxonomy ← * <~ ObjectUtils.commitUpdate(taxonomy,
                                                  form.attributes,
                                                  taxonomy.shadow.attributes.merge(shadow.attributes),
                                                  Taxonomies.updateHead)
      taxons ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(newTaxonomy.model)
    } yield FullTaxonomyResponse.build(newTaxonomy, taxons)
  }

  def archiveByContextAndId(taxonomyFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonomyFormId)
      archived ← * <~ Taxonomies.update(taxonomy, taxonomy.copy(archivedAt = Some(Instant.now)))
    } yield {}

  def getTaxon(taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[FullTaxonResponse] =
    for {
      taxon    ← * <~ ObjectManager.getFullObject(Taxons.mustFindByFormId404(taxonFormId))
      response ← * <~ buildSingleTaxonResponse(taxon)
    } yield response

  def createTaxon(
      taxonomyFormId: ObjectForm#Id,
      payload: CreateTaxonPayload)(implicit ec: EC, oc: OC, au: AU): DbResultT[FullTaxonResponse] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonomyFormId)

      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxon ← * <~ Taxons.create(
               Taxon(contextId = oc.id,
                     scope = scope,
                     formId = ins.form.id,
                     shadowId = ins.shadow.id,
                     commitId = ins.commit.id))

      parentLink ← * <~ payload.location.fold(DbResultT.none[TaxonomyTaxonLink])(location ⇒
                    validateLocation(taxonomy, taxon, location))
      index ← * <~ TaxonomyTaxonLinks.nextIndex(taxonomy.id).result

      moveSpec ← * <~ MoveSpec(TaxonomyTaxonLink(index = index,
                                                 taxonomyId = taxonomy.id,
                                                 taxonId = taxon.id,
                                                 position = 0,
                                                 path = LTree("")),
                               parentLink,
                               payload.location.flatMap(_.position)).validate.map(_.fillLinkWithNewPath)
      taxonWithPosition ← * <~ TaxonomyTaxonLinks
                           .preparePosition(moveSpec.taxon, payload.location.flatMap(_.position))
      link     ← * <~ TaxonomyTaxonLinks.create(taxonWithPosition)
      response ← * <~ buildSingleTaxonResponse(FullObject(taxon, ins.form, ins.shadow))
    } yield response
  }

  private def validateLocation(taxonomy: Taxonomy, taxon: Taxon, location: TaxonLocationPayload)(
      implicit ec: EC,
      oc: OC): DbResultT[Option[TaxonomyTaxonLink]] =
    for {
      parentLink ← * <~ location.parent.fold(DbResultT.none[TaxonomyTaxonLink])(
                    pid ⇒
                      TaxonomyTaxonLinks
                        .active()
                        .mustFindByTaxonomyAndTaxonFormId(taxonomy, pid)
                        .map(Some(_)))

      _ ← * <~ location.position.fold(DbResultT.unit) { position ⇒
           when(
             position != 0,
             TaxonomyTaxonLinks
               .active()
               .findByPathAndPosition(parentLink.map(_.childPath).getOrElse(LTree("")), position - 1)
               .mustFindOneOr(TaxonomyFailures.NoTaxonAtPosition(location.parent, position))
               .meh
           )
         }
    } yield parentLink

  def updateTaxon(taxonId: Int, payload: UpdateTaxonPayload)(implicit ec: EC,
                                                             oc: OC,
                                                             db: DB): DbResultT[FullTaxonResponse] =
    for {
      taxon     ← * <~ Taxons.mustFindByFormId404(taxonId)
      _         ← * <~ failIf(taxon.archivedAt.isDefined, TaxonIsArchived(taxonId))
      newTaxon  ← * <~ updateTaxonAttributes(taxon, payload)
      _         ← * <~ payload.location.fold(DbResultT.unit)(location ⇒ updateTaxonomyHierarchy(taxon, location).meh)
      taxonFull ← * <~ ObjectManager.getFullObject(DbResultT.good(newTaxon))
      response  ← * <~ buildSingleTaxonResponse(taxonFull)
    } yield response

  private def buildSingleTaxonResponse(taxonFull: FullObject[Taxon])(
      implicit ec: EC): DbResultT[FullTaxonResponse] =
    for {
      taxonomyTaxonLink ← * <~ TaxonomyTaxonLinks
                           .filterRight(taxonFull.model)
                           .mustFindOneOr(InvalidTaxonomiesForTaxon(taxonFull.model, 0))
      taxonomy    ← * <~ Taxonomies.findOneById(taxonomyTaxonLink.leftId).safeGet
      maybeParent ← * <~ TaxonomyTaxonLinks.parentOf(taxonomyTaxonLink)
      parentTaxon ← * <~ maybeParent.flatTraverse(link ⇒ Taxons.findOneById(link.rightId).dbresult)
    } yield FullTaxonResponse.build(taxonFull, taxonomy.formId, parentTaxon.map(_.formId))

  private def updateTaxonomyHierarchy(taxon: Taxon,
                                      location: TaxonLocationPayload)(implicit ec: EC, db: DB, oc: OC) =
    for {
      taxonomy   ← * <~ mustFindSingleTaxonomyForTaxon(taxon)
      parentLink ← * <~ validateLocation(taxonomy.model, taxon, location)
      link ← * <~ TaxonomyTaxonLinks
              .active()
              .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, taxon.formId)
      moveSpec ← * <~ MoveSpec(link, parentLink, location.position).validate
      _        ← * <~ when(moveSpec.isMoveRequired, moveTaxon(taxonomy.model, moveSpec))
    } yield taxonomy

  private def mustFindSingleTaxonomyForTaxon(taxon: Taxon)(implicit ec: EC,
                                                           db: DB): DbResultT[FullObject[Taxonomy]] =
    for {
      taxonomies ← * <~ TaxonomyTaxonLinks.queryLeftByRight(taxon)
      taxonomy ← * <~ (taxonomies.toList match {
                  case t :: Nil ⇒ DbResultT.good(t)
                  case _ ⇒
                    DbResultT.failure(InvalidTaxonomiesForTaxon(taxon, taxonomies.length))
                })
    } yield taxonomy

  private def moveTaxon(taxon: Taxonomy, moveSpec: MoveSpec)(implicit ec: EC, oc: OC, db: DB) =
    for {
      _ ← * <~ TaxonomyTaxonLinks.archive(moveSpec.taxon)
      newPath = moveSpec.newPath
      beforePositioning ← * <~ moveSpec.taxon.copy(id = 0, position = 0, path = newPath)
      linkToCreate      ← * <~ TaxonomyTaxonLinks.preparePosition(beforePositioning, moveSpec.newPosition)

      newLink ← * <~ TaxonomyTaxonLinks.create(linkToCreate)

      _ ← * <~ TaxonomyTaxonLinks.updatePaths(
           taxon.id,
           moveSpec.taxon.childPath,
           newPath.copy(value = newPath.value ::: List(moveSpec.taxon.index.toString)))

    } yield {}

  private def updateTaxonAttributes(taxon: Taxon, payload: UpdateTaxonPayload)(implicit ec: EC,
                                                                               db: DB,
                                                                               oc: OC): DbResultT[Taxon] = {

    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      fullTaxon ← * <~ ObjectManager.getFullObject(DbResultT.good(taxon))
      newTaxon ← * <~ ObjectUtils.commitUpdate(fullTaxon,
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
      _        ← * <~ failIf(children.exists(identity), TaxonomyFailures.CannotArchiveParentTaxon(taxonFormId))
      _        ← * <~ links.map(TaxonomyTaxonLinks.archive)
    } yield {}

  def assignProduct(
      taxonFormId: ObjectForm#Id,
      productFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Seq[AssignedTaxonsResponse]] =
    for {
      taxon    ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      product  ← * <~ Products.mustFindByFormId404(productFormId)
      _        ← * <~ ProductTaxonLinks.createIfNotExist(product, taxon)
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def unassignProduct(
      taxonFormId: ObjectForm#Id,
      productFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Seq[AssignedTaxonsResponse]] =
    for {
      taxon   ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      product ← * <~ Products.mustFindByFormId404(productFormId)
      r ← * <~ ProductTaxonLinks
           .filterLeft(product)
           .filter(_.rightId === taxon.id)
           .deleteAll(DbResultT.none,
                      DbResultT.failure(TaxonomyFailures.CannotUnassignProduct(taxon.formId, product.formId)))
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def getAssignedTaxons(
      productRef: ProductReference)(implicit ec: EC, oc: OC, db: DB): DbResultT[Seq[AssignedTaxonsResponse]] =
    for {
      product  ← * <~ Products.mustFindByReference(productRef)
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def getAssignedTaxons(
      product: Product)(implicit ec: EC, oc: OC, db: DB): DbResultT[Seq[AssignedTaxonsResponse]] = {

    val assignedTaxons = ProductTaxonLinks.filterLeft(product).flatMap(_.right)

    //get taxonomies and taxons assigned to the product grouped into (taxonomy, taxon) pairs
    val assignedTaxonomiesQuery = TaxonomyTaxonLinks
      .join(assignedTaxons)
      .on { case (link, taxons) ⇒ link.rightId === taxons.id }
      .join(Taxonomies)
      .on { case ((link, _), taxonomy) ⇒ link.leftId === taxonomy.id }
      .map { case ((_, taxon), taxonomy) ⇒ (taxonomy, taxon) }

    for {
      assignedTaxonomies ← * <~ assignedTaxonomiesQuery.result
      (taxonomies, taxons) = assignedTaxonomies
        .groupBy { case (taxonomy, _) ⇒ taxonomy }
        .mapValues(_.map { case (_, taxon) ⇒ taxon })
        .unzip

      fullTaxonomies ← * <~ ObjectManager.getFullObjects(taxonomies.toSeq)
      fullTaxons     ← * <~ taxons.map(ObjectManager.getFullObjects).toList

    } yield
      fullTaxonomies.zip(fullTaxons).map {
        case (taxonomy, items) ⇒ AssignedTaxonsResponse.build(taxonomy, items)
      }
  }
}
