package services.image

import failures.ImageFailures._
import models.StoreAdmin
import models.image._
import models.inventory.Skus
import models.objects.ObjectUtils.InsertResult
import models.objects._
import models.product.Products
import payloads.ImagePayloads._
import responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import responses.AlbumResponses._
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ImageManager {
  type FullAlbum           = FullObject[Album]
  type FullAlbumWithImages = (FullObject[Album], Seq[FullObject[Image]])

  def getAlbum(id: Int, contextName: String)(implicit ec: EC, db: DB): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ getAlbumInner(id, context)
    } yield album

  def getAlbumInner(id: Int, context: ObjectContext)(implicit ec: EC,
                                                     db: DB): DbResultT[AlbumRoot] =
    for {
      album  ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      images ← * <~ getAlbumImages(album.model.id)
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
      context        ← * <~ ObjectManager.mustFindByName404(contextName)
      createdObjects ← * <~ createAlbumInner(album, context)
      (album, images) = createdObjects
    } yield AlbumResponse.build(album, images)

  def createAlbumInner(createPayload: CreateAlbumPayload, context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[FullAlbumWithImages] =
    for {
      payload ← * <~ createPayload.validate

      album ← * <~ ObjectUtils.insertFullObject(payload.formAndShadow,
                                                ins ⇒ createAlbumHeadFromInsert(context, ins))
      images ← * <~ (payload.images match {
                    case Some(imagesPayload) ⇒
                      imagesPayload.map(
                          img ⇒
                            ObjectUtils.insertFullObject(
                                img.formAndShadow,
                                ins ⇒ createImageHeadFromInsert(context, ins)))
                    case None ⇒
                      Seq.empty
                  })
    } yield (album, images)

  def createImagesForAlbum(album: Album, imagesPayload: Seq[ImagePayload], context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      images ← * <~ imagesPayload.map(
                  img ⇒
                    ObjectUtils.insertFullObject(img.formAndShadow,
                                                 ins ⇒ createImageHeadFromInsert(context, ins)))
      _ ← * <~ images.map(image ⇒
               AlbumImageLinks.create(AlbumImageLink(leftId = album.id, rightId = image.model.id)))
    } yield images

  def updateImagesForAlbum(album: Album, imagesPayload: Seq[ImagePayload], context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[Image]]] = {
    val (createPayloads, updatePayloads) = imagesPayload.span(_.id.isEmpty)
    for {
      createdImages ← * <~ createImagesForAlbum(album, createPayloads, context)
      imageObjects ← * <~ updatePayloads.map(img ⇒
                          ObjectManager.getFullObject(Images.mustFindById404(img.id.get)))
      updatedImages ← * <~ imageObjects.zip(updatePayloads).map {
                       case (original, newImage) ⇒
                         val (newForm, newShadow) = newImage.formAndShadow.tupled
                         ObjectUtils.commitUpdate(original,
                                                  newForm.attributes,
                                                  newShadow.attributes,
                                                  updateImageHead)
                     }
    } yield updatedImages
  }

  def createAlbumForProduct(
      admin: StoreAdmin,
      productId: Int,
      payload: CreateAlbumPayload,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
      created ← * <~ createAlbumInner(payload, context)
      (fullAlbum, images) = created
      link ← * <~ ObjectLinks.create(
                ObjectLink(leftId = product.shadowId,
                           rightId = fullAlbum.shadow.id,
                           linkType = ObjectLink.ProductAlbum))
    } yield AlbumResponse.build(fullAlbum, images)

  def createAlbumForSku(
      admin: StoreAdmin,
      code: String,
      payload: CreateAlbumPayload,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      created ← * <~ createAlbumInner(payload, context)
      (fullAlbum, images) = created
      link ← * <~ ObjectLinks.create(
                ObjectLink(leftId = sku.shadowId,
                           rightId = fullAlbum.shadow.id,
                           linkType = ObjectLink.SkuAlbum))
    } yield AlbumResponse.build(fullAlbum, images)

  def updateAlbum(id: Int, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] =
    for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ updateAlbumInner(id, payload, contextName)
    } yield response

  def updateAlbumInner(id: Int, updatePayload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] = {
    for {
      payload ← * <~ updatePayload.validate
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      oldShadow                    = album.shadow
      (payloadForm, payloadShadow) = payload.formAndShadow.tupled
      mergedAtts                   = oldShadow.attributes.merge(payloadShadow.attributes)
      album ← * <~ ObjectUtils.commitUpdate[Album](album,
                                                   payloadForm.attributes,
                                                   mergedAtts,
                                                   updateAlbumHead,
                                                   force = true)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Products,
                                                 context.id,
                                                 oldShadow.id,
                                                 album.model.shadowId,
                                                 ObjectLink.ProductAlbum)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(Skus,
                                                 context.id,
                                                 oldShadow.id,
                                                 album.model.shadowId,
                                                 ObjectLink.SkuAlbum)

      _      ← * <~ updateImagesForAlbum(album.model, payload.images.getOrElse(Seq.empty), context)
      images ← * <~ getAlbumImages(album.model.id)
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
                  images ← * <~ getAlbumImages(album.id)
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

  def getAlbumImages(albumId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      imageIds ← * <~ AlbumImageLinks.filterLeft(albumId).map(_.rightId).result
      images ← * <~ imageIds.map(imgId ⇒
                    ObjectManager.getFullObject(Images.mustFindById404(imgId)))
    } yield images

  private def createAlbumHeadFromInsert(oc: ObjectContext, insert: InsertResult)(
      implicit ec: EC,
      db: DB): DbResultT[Album] =
    Albums.create(
        Album(contextId = oc.id,
              shadowId = insert.shadow.id,
              formId = insert.form.id,
              commitId = insert.commit.id))

  private def updateAlbumHead(fullObject: FullObject[Album], commitId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[Album]] =
    for {
      newModel ← * <~ Albums.update(
                    fullObject.model,
                    fullObject.model.copy(shadowId = fullObject.shadow.id, commitId = commitId))
    } yield fullObject.copy(model = newModel)

  private def createImageHeadFromInsert(oc: ObjectContext, ins: InsertResult)(
      implicit ec: EC,
      db: DB): DbResultT[Image] = {
    Images.create(
        Image(contextId = oc.id,
              shadowId = ins.shadow.id,
              formId = ins.form.id,
              commitId = ins.commit.id))
  }

  private def updateImageHead(fullObject: FullObject[Image], commitId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[FullObject[Image]] =
    for {
      newModel ← * <~ Images.update(
                    fullObject.model,
                    fullObject.model.copy(shadowId = fullObject.shadow.id, commitId = commitId))
    } yield fullObject.copy(model = newModel)
}
