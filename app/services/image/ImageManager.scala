package services.image

import failures.ImageFailures._
import failures.NotFoundFailure404
import models.StoreAdmin
import models.image._
import models.inventory.Sku
import models.objects.ObjectUtils.InsertResult
import models.objects._
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

  def getAlbum(formId: ObjectForm#Id, contextName: String)(implicit ec: EC,
                                                           db: DB): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ getAlbumInner(formId, context)
    } yield album

  def getAlbumInner(id: ObjectForm#Id, context: ObjectContext)(implicit ec: EC,
                                                               db: DB): DbResultT[AlbumRoot] =
    for {
      album  ← * <~ mustFindFullAlbumByFormIdAndContext404(id, context)
      images ← * <~ getAlbumImages(album.model.id)
    } yield AlbumResponse.build(album, images)

  def getAlbumsForProduct(
      productFormId: ObjectForm#Id)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product ← * <~ ProductManager.mustFindProductByContextAndFormId404(oc.id, productFormId)
      albums  ← * <~ ProductAlbumLinks.queryRightByLeft(product)
      images  ← * <~ albums.map(album ⇒ getAlbumImages(album.model.id))
      result ← * <~ albums.zip(images).map {
                case (album, image) ⇒ AlbumResponse.build(album, image)
              }
    } yield result

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
      result ← * <~ getAlbumsBySku(sku)
    } yield result

  def getAlbumsBySku(sku: Sku)(implicit ec: EC, db: DB): DbResultT[Seq[AlbumResponse.Root]] =
    for {
      albums ← * <~ SkuAlbumLinks.queryRightByLeft(sku)
      images ← * <~ albums.map(album ⇒ getAlbumImages(album.model.id))
      result ← * <~ albums.zip(images).map {
                case (album, image) ⇒ AlbumResponse.build(album, image)
              }
    } yield result

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
                      createImagesForAlbum(album.model, imagesPayload, context)
                    case None ⇒
                      DbResultT.good(Seq.empty)
                  })
    } yield (album, images)

  def createOrUpdateImagesForAlbum(
      album: Album,
      imagesPayload: Seq[ImagePayload],
      context: ObjectContext)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      updatedImages ← * <~ imagesPayload.zipWithIndex.map {
                       case (payload, index) ⇒
                         createOrUpdateImageForAlbum(album, payload, index, context)
                     }
      imageIds = updatedImages.map(_.model.id).toSet
      links ← * <~ AlbumImageLinks.filterLeft(album.id).result
      linksToDelete = links.filter(link ⇒ !imageIds.contains(link.rightId))
      _ ← * <~ linksToDelete.map(
             link ⇒
               AlbumImageLinks
                 .deleteById(link.id, DbResultT.unit, (id) ⇒ NotFoundFailure404(link, id)))
      imagesToDelete ← * <~ Images.filterByIds(linksToDelete.map(_.rightId)).result
      _ ← * <~ imagesToDelete.map(img ⇒
               Images.deleteById(img.id, DbResultT.unit, (id) ⇒ NotFoundFailure404(img, id)))
    } yield updatedImages

  def createOrUpdateImageForAlbum(
      album: Album,
      payload: ImagePayload,
      position: Int,
      context: ObjectContext)(implicit ec: EC, db: DB): DbResultT[FullObject[Image]] =
    payload.id match {
      case None ⇒
        for {
          inserted ← * <~ ObjectUtils.insertFullObject(
                        payload.formAndShadow,
                        ins ⇒ createImageHeadFromInsert(context, ins))
          _ ← * <~ AlbumImageLinks.create(
                 AlbumImageLink(leftId = album.id,
                                position = position,
                                rightId = inserted.model.id))
        } yield inserted
      case Some(id) ⇒
        for {
          image ← * <~ ObjectManager.getFullObject(Images.mustFindById404(id))
          link  ← * <~ mustFindAlbumImageLink404(album.id, id)
          _     ← * <~ AlbumImageLinks.update(link, link.copy(position = position))
          (newForm, newShadow) = payload.formAndShadow.tupled
          updated ← * <~ ObjectUtils.commitUpdate(image,
                                                  newForm.attributes,
                                                  newShadow.attributes,
                                                  updateImageHead)
        } yield updated
    }

  private def mustFindAlbumImageLink404(albumId: Album#Id, imageId: Image#Id)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumImageLink] =
    AlbumImageLinks
      .filterLeftAndRight(albumId, imageId)
      .mustFindOneOr(ImageNotFoundInAlbum(imageId, albumId))

  def createImagesForAlbum(album: Album, imagesPayload: Seq[ImagePayload], context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      images ← * <~ imagesPayload.map(
                  img ⇒
                    ObjectUtils.insertFullObject(img.formAndShadow,
                                                 ins ⇒ createImageHeadFromInsert(context, ins)))
      links ← * <~ images.zipWithIndex.map {
               case (image, index) ⇒
                 AlbumImageLinks.create(
                     AlbumImageLink(leftId = album.id, position = index, rightId = image.model.id))
             }
    } yield images

  def createAlbumForProduct(
      admin: StoreAdmin,
      productId: Int,
      payload: CreateAlbumPayload,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ ProductManager.mustFindProductByContextAndFormId404(context.id, productId)
      created ← * <~ createAlbumInner(payload, context)
      (fullAlbum, images) = created
      link ← * <~ ProductAlbumLinks.create(
                ProductAlbumLink(leftId = product.id, rightId = fullAlbum.model.id))
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
      link ← * <~ SkuAlbumLinks.create(SkuAlbumLink(leftId = sku.id, rightId = fullAlbum.model.id))
    } yield AlbumResponse.build(fullAlbum, images)

  def updateAlbum(id: ObjectForm#Id, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] =
    for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ updateAlbumInner(id, payload, contextName)
    } yield response

  def updateAlbumInner(id: ObjectForm#Id, updatePayload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[AlbumRoot] =
    for {
      payload ← * <~ updatePayload.validate
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ mustFindFullAlbumByFormIdAndContext404(id, context)
      oldShadow                    = album.shadow
      (payloadForm, payloadShadow) = payload.formAndShadow.tupled
      mergedAtts                   = oldShadow.attributes.merge(payloadShadow.attributes)
      album ← * <~ ObjectUtils.commitUpdate[Album](album,
                                                   payloadForm.attributes,
                                                   mergedAtts,
                                                   updateAlbumHead,
                                                   force = true)
      _ ← * <~ createOrUpdateImagesForAlbum(album.model,
                                            payload.images.getOrElse(Seq.empty),
                                            context)
      images ← * <~ getAlbumImages(album.model.id)
    } yield AlbumResponse.build(album, images)

  def mustFindFullAlbumByFormIdAndContext404(id: Album#Id, context: ObjectContext)(implicit ec: EC,
                                                                                   db: DB) =
    for {
      album  ← * <~ mustFindAlbumByFormIdAndContext404(id, context)
      form   ← * <~ ObjectForms.mustFindById404(album.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(album.shadowId)
    } yield FullObject(model = album, form = form, shadow = shadow)

  private def mustFindAlbumByFormIdAndContext404(id: Int, context: ObjectContext)(implicit ec: EC,
                                                                                  db: DB) =
    Albums
      .filterByContextAndFormId(context.id, id)
      .mustFindOneOr(AlbumNotFoundForContext(id, context.id))

  def getAlbumImages(albumId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      imageIds ← * <~ AlbumImageLinks.filterLeft(albumId).sortBy(_.position).map(_.rightId).result
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
