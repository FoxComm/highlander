package services.taxonomy

import java.time.Instant

import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures.TaxonomyFailures._
import failures.{Failure, TaxonomyFailures}
import models.objects.ObjectHeadLinks.ObjectHeadLinkQueries
import models.objects._
import models.product.Products
import models.taxonomy.TaxonomyTaxonLinks.scope._
import models.taxonomy.{TaxonLocation ⇒ _, _}
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
                      newPosition: Option[Int])
      extends Validation[MoveSpec] {

    def fillLinkWithNewPath: MoveSpec = copy(taxon = taxon.copy(path = newPath))

    def isMoveRequired: Boolean =
      taxon.path != newPath || newPosition.fold(true)(_ != taxon.position)

    override def validate: ValidatedNel[Failure, MoveSpec] = {

      def validateSameTaxonomy: ValidatedNel[Failure, Unit] =
        Validation.validExpr(
            taxon.taxonomyId == parent.map(_.taxonomyId).getOrElse(taxon.taxonomyId),
            TaxonomyFailures.TaxonomyShouldMatchForParentAndTarget.description)

      def validateParentToChildMove: ValidatedNel[Failure, Unit] = {
        Validation.validExpr(
            !isMoveRequired || taxon.id == 0 || !newPath.value.startsWith(taxon.childPath.value),
            CannotMoveParentTaxonUnderChild.description)
      }

      (validateSameTaxonomy |@| validateParentToChildMove).map {
        case _ ⇒ this
      }
    }

    val newPath: LTree = parent.map(_.childPath).getOrElse(LTree(List()))
  }

  def getTaxonomy(taxonomyFormId: ObjectForm#Id)(implicit ec: EC,
                                                 oc: OC,
                                                 db: DB): DbResultT[TaxonomyResponse] =
    for {
      taxonomy ← * <~ ObjectUtils.getFullObject(Taxonomies.mustFindByFormId404(taxonomyFormId))
      taxons   ← * <~ TaxonomyTaxonLinks.queryRightByLeftWithLinks(taxonomy.model)
    } yield TaxonomyResponse.build(taxonomy, taxons)

  def createTaxonomy(payload: CreateTaxonomyPayload)(implicit ec: EC,
                                                     oc: OC,
                                                     au: AU): DbResultT[TaxonomyResponse] = {
    val form   = ObjectForm.fromPayload(Taxonomy.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxonomy ← * <~ Taxonomies.create(
                    Taxonomy(hierarchical = payload.hierarchical,
                             scope = LTree(au.token.scope),
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
      archived ← * <~ Taxonomies.update(taxonomy, taxonomy.copy(archivedAt = Some(Instant.now)))
    } yield {}

  def getTaxon(taxonFormId: ObjectForm#Id)(implicit ec: EC,
                                           oc: OC,
                                           db: DB): DbResultT[SingleTaxonResponse] =
    for {
      taxonFull ← * <~ ObjectUtils.getFullObject(Taxons.mustFindByFormId404(taxonFormId))
      response  ← * <~ buildSingleTaxonResponse(taxonFull)
    } yield response

  def createTaxon(taxonFormId: ObjectForm#Id, payload: CreateTaxonPayload)(
      implicit ec: EC,
      oc: OC,
      au: AU): DbResultT[SingleTaxonResponse] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      _        ← * <~ payload.validate
      taxonomy ← * <~ Taxonomies.mustFindByFormId404(taxonFormId)

      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxon ← * <~ Taxons.create(
                 Taxon(contextId = oc.id,
                       scope = LTree(au.token.scope),
                       formId = ins.form.id,
                       shadowId = ins.shadow.id,
                       commitId = ins.commit.id))

      parentLink ← * <~ payload.location.fold(DbResultT.none[TaxonomyTaxonLink])(location ⇒
                        validateLocation(taxonomy, taxon, location))
      index ← * <~ TaxonomyTaxonLinks.nextIndex(taxonomy.id).result

      moveSpec ← * <~ MoveSpec(
                    TaxonomyTaxonLink(index = index,
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

  private def validateLocation(taxonomy: Taxonomy, taxon: Taxon, location: TaxonLocation)(
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
           doOrMeh(position != 0,
                   TaxonomyTaxonLinks
                     .active()
                     .findByPathAndPosition(parentLink.map(_.childPath).getOrElse(LTree("")),
                                            position - 1)
                     .mustFindOneOr(TaxonomyFailures.NoTaxonAtPosition(location.parent, position))
                     .meh)
         }
    } yield parentLink

  def updateTaxon(taxonId: Int, payload: UpdateTaxonPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[SingleTaxonResponse] = {
    for {
      _     ← * <~ payload.validate
      taxon ← * <~ Taxons.mustFindByFormId404(taxonId)
      _     ← * <~ failIf(taxon.archivedAt.isDefined, TaxonIsArchived(taxonId))
      newTaxon ← * <~ (payload.attributes match {
                      case Some(attributes) ⇒ updateTaxonAttributes(taxon, attributes)
                      case _                ⇒ DbResultT.good(taxon)
                    })
      _ ← * <~ payload.location.fold(DbResultT.unit)(location ⇒
               updateTaxonomyHierarchy(taxon, location).meh)
      taxonFull ← * <~ ObjectManager.getFullObject(DbResultT.good(newTaxon))
      response  ← * <~ buildSingleTaxonResponse(taxonFull)
    } yield response
  }

  private def buildSingleTaxonResponse(taxonFull: FullObject[Taxon])(
      implicit ec: EC): DbResultT[SingleTaxonResponse] =
    for {
      taxonomyTaxonLink ← * <~ TaxonomyTaxonLinks
                           .filterRight(taxonFull.model)
                           .mustFindOneOr(InvalidTaxonomiesForTaxon(taxonFull.model, 0))
      maybeParent ← * <~ TaxonomyTaxonLinks.parentOf(taxonomyTaxonLink)
    } yield
      SingleTaxonResponse.build(taxonomyTaxonLink.leftId, taxonFull, maybeParent.map(_.rightId))

  private def updateTaxonomyHierarchy(taxon: Taxon,
                                      location: TaxonLocation)(implicit ec: EC, db: DB, oc: OC) =
    for {
      taxonomy   ← * <~ mustFindSingleTaxonomyForTaxon(taxon)
      parentLink ← * <~ validateLocation(taxonomy.model, taxon, location)
      link ← * <~ TaxonomyTaxonLinks
              .active()
              .mustFindByTaxonomyAndTaxonFormId(taxonomy.model, taxon.formId)
      moveSpec ← * <~ MoveSpec(link, parentLink, location.position).validate
      _        ← * <~ doOrMeh(moveSpec.isMoveRequired, moveTaxon(taxonomy.model, moveSpec))
    } yield taxonomy

  private def mustFindSingleTaxonomyForTaxon(
      taxon: Taxon)(implicit ec: EC, db: DB): DbResultT[FullObject[Taxonomy]] =
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
      linkToCreate ← * <~ TaxonomyTaxonLinks.preparePosition(beforePositioning,
                                                             moveSpec.newPosition)

      newLink ← * <~ TaxonomyTaxonLinks.create(linkToCreate)

      _ ← * <~ TaxonomyTaxonLinks.updatePaths(
             taxon.id,
             moveSpec.taxon.childPath,
             newPath.copy(value = newPath.value ::: List(moveSpec.taxon.index.toString)))

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

  def assignProduct(taxonFormId: ObjectForm#Id, productFormId: ObjectForm#Id)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[Seq[SingleTaxonResponse]] =
    for {
      taxon    ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      product  ← * <~ Products.mustFindByFormId404(productFormId)
      _        ← * <~ ProductTaxonLinks.createIfNotExist(product, taxon)
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def unassignProduct(taxonFormId: ObjectForm#Id, productFormId: ObjectForm#Id)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[Seq[SingleTaxonResponse]] =
    for {
      taxon   ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      product ← * <~ Products.mustFindByFormId404(productFormId)
      r ← * <~ ProductTaxonLinks
           .filterLeft(product)
           .filter(_.rightId === taxon.id)
           .deleteAll(DbResultT.none,
                      DbResultT.failure(
                          TaxonomyFailures.CannotUnassignProduct(taxon.formId, product.formId)))
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def getAssignedTaxons(productFormId: ObjectForm#Id)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[Seq[SingleTaxonResponse]] =
    for {
      product  ← * <~ Products.mustFindByFormId404(productFormId)
      assigned ← * <~ getAssignedTaxons(product)
    } yield assigned

  def getAssignedTaxons(product: models.product.Product)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[Seq[SingleTaxonResponse]] =
    for {
      taxons   ← * <~ ProductTaxonLinks.queryRightByLeft(product)
      response ← * <~ taxons.map(buildSingleTaxonResponse)
    } yield response

}
