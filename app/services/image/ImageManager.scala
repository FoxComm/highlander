package services.image

import failures.ImageFailures._
import failures.ObjectFailures.ObjectContextNotFound
import models.StoreAdmin
import models.image._
import models.inventory.Skus
import models.objects._
import models.product.Products
import payloads.ImagePayloads._
import responses.ImageResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.ImageResponses._
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ImageManager {
  type FullAlbum = FullObject[Album]

  def getAlbum(id: Int, contextName: String)(implicit ec: EC, db: DB): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ getAlbumInner(id, context)
    } yield album

  def getAlbumInner(id: Int, context: ObjectContext)(implicit ec: EC,
                                                     db: DB): DbResultT[AlbumRoot] =
    for {
      album  ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      images ← * <~ Image.buildFromAlbum(album)
    } yield AlbumResponse.build(album, images)

  def getAlbumsForProduct(
      productId: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product ← * <~ ProductManager.mustFindProductByContextAndId404(oc.id, productId)
      albums  ← * <~ getAlbumsForObject(product.shadowId, oc, ObjectLink.ProductAlbum)
    } yield albums

  def getAlbumsForSku(code: String, contextName: String)(implicit ec: EC,
                                                         db: DB): DbResultT[Seq[AlbumRoot]] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      albums  ← * <~ getAlbumsForSkuInner(code, context)
    } yield albums

  def getAlbumsForSkuInner(code: String, context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[AlbumResponse.Root]] =
    for {
      sku    ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      albums ← * <~ getAlbumsForObject(sku.shadowId, context, ObjectLink.SkuAlbum)
    } yield albums

  def createAlbum(album: CreateAlbumPayload, contextName: String)(implicit ec: EC,
                                                                  db: DB): DbResultT[AlbumRoot] =
    for {
      context   ← * <~ ObjectManager.mustFindByName404(contextName)
      fullAlbum ← * <~ createAlbumInner(album, context)
      images    ← * <~ Image.buildFromAlbum(fullAlbum)
    } yield AlbumResponse.build(fullAlbum, images)

  def createAlbumInner(payload: CreateAlbumPayload,
                       context: ObjectContext)(implicit ec: EC, db: DB): DbResultT[FullAlbum] = {

    val album = payload.fillImageIds()
    for {
      _   ← * <~ album.validate
      ins ← * <~ ObjectUtils.insert(album.objectForm, album.objectShadow)
      album ← * <~ Albums.create(
                 Album(contextId = context.id,
                       shadowId = ins.shadow.id,
                       formId = ins.form.id,
                       commitId = ins.commit.id))
    } yield FullObject(model = album, form = ins.form, shadow = ins.shadow)
  }

  def createAlbumForProduct(
      admin: StoreAdmin,
      productId: Int,
      payload: CreateAlbumPayload,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
      album   ← * <~ createAlbumInner(payload, context)
      images  ← * <~ Image.buildFromAlbum(album)
      link ← * <~ ObjectLinks.create(
                ObjectLink(leftId = product.shadowId,
                           rightId = album.shadow.id,
                           linkType = ObjectLink.ProductAlbum))
    } yield AlbumResponse.build(album, images)

  def createAlbumForSku(
      admin: StoreAdmin,
      code: String,
      payload: CreateAlbumPayload,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      album   ← * <~ createAlbumInner(payload, context)
      images  ← * <~ Image.buildFromAlbum(album)
      link ← * <~ ObjectLinks.create(
                ObjectLink(leftId = sku.shadowId,
                           rightId = album.shadow.id,
                           linkType = ObjectLink.SkuAlbum))
    } yield AlbumResponse.build(album, images)

  def updateAlbum(id: Int, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] =
    for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ updateAlbumInner(id, payload, contextName)
    } yield response

  def updateAlbumInner(id: Int, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] = {
    val fixedPayload = payload.fillImageIds()
    for {
      _ ← * <~ payload.validate
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      album ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      oldShadow  = album.shadow
      mergedAtts = oldShadow.attributes.merge(fixedPayload.objectShadow.attributes)
      updated ← * <~ ObjectUtils.update(album.model.formId,
                                        album.model.shadowId,
                                        fixedPayload.objectForm.attributes,
                                        mergedAtts,
                                        force = true)
      commit ← * <~ ObjectUtils.commit(updated)
      album  ← * <~ updateHead(album.model, updated.shadow, commit)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Products,
                                                 context.id,
                                                 oldShadow.id,
                                                 album.shadowId,
                                                 ObjectLink.ProductAlbum)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Skus,
                                                 context.id,
                                                 oldShadow.id,
                                                 album.shadowId,
                                                 ObjectLink.SkuAlbum)
      album  ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      images ← * <~ Image.buildFromAlbum(album)
    } yield AlbumResponse.build(album, images)
  }

  private def getAlbumsForObject(
      shadowId: Int,
      context: ObjectContext,
      linkType: ObjectLink.LinkType)(implicit ec: EC, db: DB): DbResultT[Seq[AlbumRoot]] =
    for {
      links ← * <~ ObjectLinks.findByLeftAndType(shadowId, linkType).result
      albums ← * <~ links.map { link ⇒
                for {
                  shadow ← * <~ ObjectShadows.mustFindById404(link.rightId)
                  form   ← * <~ ObjectForms.mustFindById404(shadow.formId)
                  album  ← * <~ mustFindAlbumByIdAndContext404(form.id, context)
                  full = FullObject(model = album, form = form, shadow = shadow)
                  images ← * <~ Image.buildFromAlbum(full)
                } yield AlbumResponse.build(full, images)
              }
    } yield albums

  def mustFindFullAlbumByIdAndContext404(id: Int, context: ObjectContext)(implicit ec: EC,
                                                                          db: DB) =
    for {
      album  ← * <~ mustFindAlbumByIdAndContext404(id, context)
      form   ← * <~ ObjectForms.mustFindById404(album.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(album.shadowId)
    } yield FullObject(model = album, form = form, shadow = shadow)

  private def mustFindAlbumByIdAndContext404(id: Int, context: ObjectContext)(implicit ec: EC,
                                                                              db: DB) =
    Albums
      .filterByContextAndFormId(context.id, id)
      .mustFindOneOr(AlbumNotFoundForContext(id, context.id))

  private def updateHead(album: Album, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResultT[Album] = maybeCommit match {
    case Some(commit) ⇒
      Albums.update(album, album.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResultT.good(album)
  }
}
