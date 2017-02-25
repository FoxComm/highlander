package services.inventory

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import cats.data._
import failures.ProductFailures._
import failures.{Failures, GeneralFailure, NotFoundFailure400}
import models.account._
import models.inventory._
import models.objects._
import payloads.ImagePayloads.AlbumPayload
import payloads.SkuPayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import services.LogActivity
import services.image.ImageManager
import services.image.ImageManager.FullAlbumWithImages
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
                                                  au: AU): DbResultT[SkuResponse.Root] = {
    val albumPayloads = payload.albums.getOrElse(Seq.empty)

    for {
      sku    ← * <~ createSkuInner(oc, payload)
      albums ← * <~ findOrCreateAlbumsForSku(sku.model, albumPayloads)
      albumResponse = albums.map { case (album, images) ⇒ AlbumResponse.build(album, images) }
      response      = SkuResponse.build(IlluminatedSku.illuminate(oc, sku), albumResponse)
      _ ← * <~ LogActivity.fullSkuCreated(Some(admin), response, ObjectContextResponse.build(oc))
    } yield response
  }

  def getSku(code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, code)
      form   ← * <~ ObjectForms.findById(sku.formId)
      shadow ← * <~ ObjectShadows.findById(sku.shadowId)
      albums ← * <~ ImageManager.getAlbumsForSkuInner(sku.code, oc)
    } yield SkuResponse.build(IlluminatedSku.illuminate(oc, FullObject(sku, form, shadow)), albums)

  def updateSku(admin: User, code: String, payload: SkuPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      oc: OC,
      au: AU): DbResultT[SkuResponse.Root] =
    for {
      sku        ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, code)
      updatedSku ← * <~ updateSkuInner(sku, payload)
      albums     ← * <~ updateAssociatedAlbums(updatedSku.model, payload.albums)
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
      scope ← * <~ Scope.resolveOverride(payload.scope)
      code  ← * <~ mustGetSkuCode(payload)
      ins   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      sku ← * <~ Skus.create(
               Sku(scope = scope,
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
      oldForm   ← * <~ ObjectForms.findById(sku.formId)
      oldShadow ← * <~ ObjectShadows.findById(sku.shadowId)

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

  def findOrCreateAlbumsForSku(sku: Sku, payload: Seq[AlbumPayload])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[FullAlbumWithImages]] =
    for {
      albums ← * <~ payload.map(ImageManager.updateOrCreateAlbum)
      _      ← * <~ SkuAlbumLinks.syncLinks(sku, albums.map { case (fullAlbum, _) ⇒ fullAlbum.model })
    } yield albums

  private def updateAssociatedAlbums(sku: Sku, albumsPayload: Option[Seq[AlbumPayload]])(
      implicit ec: EC,
      db: DB,
      oc: OC,
      au: AU): DbResultT[Seq[AlbumRoot]] =
    albumsPayload match {
      case Some(payloads) ⇒
        findOrCreateAlbumsForSku(sku, payloads).map(_.map(AlbumResponse.build))
      case None ⇒
        ImageManager.getAlbumsForSkuInner(sku.code, oc)
    }

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
      shadow ← * <~ ObjectShadows.findById(shadowId)
      form   ← * <~ ObjectForms.findById(shadow.formId)
      sku    ← * <~ Skus.mustFindById404(skuId)
    } yield FullObject(sku, form, shadow)

  def illuminateSku(
      fullSku: FullObject[Sku])(implicit ec: EC, db: DB, oc: OC): DbResultT[SkuResponse.Root] =
    ImageManager
      .getAlbumsBySku(fullSku.model)
      .map(albums ⇒ SkuResponse.buildLite(IlluminatedSku.illuminate(oc, fullSku), albums))
}
