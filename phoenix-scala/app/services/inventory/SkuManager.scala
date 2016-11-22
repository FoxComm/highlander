package services.inventory

import java.time.Instant
import com.github.tminglei.slickpg.LTree

import cats.data._
import failures.ProductFailures._
import failures.{Failures, GeneralFailure, NotFoundFailure400}
import models.account._
import models.inventory._
import models.objects._
import payloads.SkuPayloads._
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import services.LogActivity
import services.image.ImageManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object SkuManager {
  implicit val formats = JsonFormatters.DefaultFormats

  def createSku(admin: User, payload: SkuPayload)(implicit ec: EC,
                                                  db: DB,
                                                  ac: AC,
                                                  oc: OC,
                                                  au: AU): DbResultT[SkuResponse.Root] =
    for {
      sku    ← * <~ createSkuInner(oc, payload)
      albums ← * <~ ImageManager.getAlbumsForSkuInner(sku.model.code, oc)
      response = SkuResponse.build(IlluminatedSku.illuminate(oc, sku), albums)
      _ ← * <~ LogActivity.fullSkuCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  def getSku(code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, code)
      form   ← * <~ ObjectForms.mustFindById404(sku.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)
      albums ← * <~ ImageManager.getAlbumsForSkuInner(sku.code, oc)
    } yield SkuResponse.build(IlluminatedSku.illuminate(oc, FullObject(sku, form, shadow)), albums)

  def updateSku(
      admin: User,
      code: String,
      payload: SkuPayload)(implicit ec: EC, db: DB, ac: AC, oc: OC): DbResultT[SkuResponse.Root] =
    for {
      sku        ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, code)
      updatedSku ← * <~ updateSkuInner(sku, payload)
      albums     ← * <~ ImageManager.getAlbumsForSkuInner(updatedSku.model.code, oc)
      response = SkuResponse.build(IlluminatedSku.illuminate(oc, updatedSku), albums)
      _ ← * <~ LogActivity.fullSkuUpdated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response

  def archiveByCode(code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    for {
      fullSku ← * <~ ObjectManager.getFullObject(
                   SkuManager.mustFindSkuByContextAndCode(oc.id, code))
      _ ← * <~ fullSku.model.mustNotBePresentInCarts
      archivedSku ← * <~ Skus.update(fullSku.model,
                                     fullSku.model.copy(archivedAt = Some(Instant.now)))
      albumLinks ← * <~ SkuAlbumLinks.filter(_.leftId === archivedSku.id).result
      _ ← * <~ albumLinks.map { link ⇒
           SkuAlbumLinks.deleteById(link.id,
                                    DbResultT.unit,
                                    id ⇒ NotFoundFailure400(SkuAlbumLinks, id))
         }
      albums       ← * <~ ImageManager.getAlbumsForSkuInner(archivedSku.code, oc)
      productLinks ← * <~ ProductSkuLinks.filter(_.rightId === archivedSku.id).result
      _ ← * <~ productLinks.map { link ⇒
           ProductSkuLinks.deleteById(link.id,
                                      DbResultT.unit,
                                      id ⇒ NotFoundFailure400(ProductSkuLinks, id))
         }
    } yield
      SkuResponse.build(
          IlluminatedSku.illuminate(
              oc,
              FullObject(model = archivedSku, form = fullSku.form, shadow = fullSku.shadow)),
          albums)

  def createSkuInner(
      context: ObjectContext,
      payload: SkuPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[FullObject[Sku]] = {

    val form   = ObjectForm.fromPayload(Sku.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      code ← * <~ mustGetSkuCode(payload)
      ins  ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      sku ← * <~ Skus.create(
               Sku(scope = LTree(au.token.scope),
                   contextId = context.id,
                   code = code,
                   formId = ins.form.id,
                   shadowId = ins.shadow.id,
                   commitId = ins.commit.id))
    } yield FullObject(sku, ins.form, ins.shadow)
  }

  def updateSkuInner(sku: Sku, payload: SkuPayload)(implicit ec: EC,
                                                    db: DB): DbResultT[FullObject[Sku]] = {

    val newFormAttrs   = ObjectForm.fromPayload(Sku.kind, payload.attributes).attributes
    val newShadowAttrs = ObjectShadow.fromPayload(payload.attributes).attributes
    val code           = getSkuCode(payload.attributes).getOrElse(sku.code)

    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(sku.formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(sku.shadowId)

      mergedAttrs = oldShadow.attributes.merge(newShadowAttrs)
      updated ← * <~ ObjectUtils
                 .update(oldForm.id, oldShadow.id, newFormAttrs, mergedAttrs, force = true)
      commit      ← * <~ ObjectUtils.commit(updated)
      updatedHead ← * <~ updateHead(sku, code, updated.shadow, commit)
    } yield FullObject(updatedHead, updated.form, updated.shadow)
  }

  def findOrCreateSku(skuPayload: SkuPayload)(implicit ec: EC, db: DB, oc: OC, au: AU) =
    for {
      code ← * <~ mustGetSkuCode(skuPayload)
      sku ← * <~ Skus.filterByContextAndCode(oc.id, code).one.dbresult.flatMap {
             case Some(sku) ⇒ SkuManager.updateSkuInner(sku, skuPayload)
             case None      ⇒ SkuManager.createSkuInner(oc, skuPayload)
           }
    } yield sku

  private def updateHead(sku: Sku,
                         code: String,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Sku] =
    maybeCommit match {
      case Some(commit) ⇒
        Skus.update(sku, sku.copy(code = code, shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(sku)
    }

  def mustGetSkuCode(payload: SkuPayload): Failures Xor String =
    getSkuCode(payload.attributes) match {
      case Some(code) ⇒ Xor.right(code)
      case None       ⇒ Xor.left(GeneralFailure("SKU code not found in payload").single)
    }

  def getSkuCode(attributes: Map[String, Json]): Option[String] =
    attributes.get("code").flatMap(json ⇒ (json \ "v").extractOpt[String])

  def mustFindSkuByContextAndCode(contextId: Int, code: String)(implicit ec: EC): DbResultT[Sku] =
    for {
      sku ← * <~ Skus
             .filterByContextAndCode(contextId, code)
             .mustFindOneOr(SkuNotFoundForContext(code, contextId))
    } yield sku

  def mustFindFullSkuById(id: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[Sku]] =
    ObjectManager.getFullObject(Skus.filter(_.id === id).mustFindOneOr(SkuNotFound(id)))

  def mustFindFullSkuByIdAndShadowId(skuId: Int, shadowId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[Sku]] =
    for {
      shadow ← * <~ ObjectShadows.mustFindById404(shadowId)
      form   ← * <~ ObjectForms.mustFindById404(shadow.formId)
      sku    ← * <~ Skus.mustFindById404(skuId)
    } yield FullObject(sku, form, shadow)

  def illuminateSku(
      fullSku: FullObject[Sku])(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    ImageManager
      .getAlbumsBySku(fullSku.model)
      .map(albums ⇒ SkuResponse.buildLite(IlluminatedSku.illuminate(oc, fullSku), albums))
}
