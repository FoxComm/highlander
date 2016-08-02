package services.image

import java.time.Instant

import failures.ImageFailures._
import failures.{ImageFailures, NotFoundFailure400, NotFoundFailure404, ProductFailures}
import models.StoreAdmin
import models.image._
import models.inventory.Sku
import models.objects.ObjectUtils.InsertResult
import models.objects._
import models.product._
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
      images ← * <~ AlbumImageLinks.queryRightByLeft(album.model)
    } yield AlbumResponse.build(album, images)

  def getAlbumsForProduct(
      productFormId: ObjectForm#Id)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product ← * <~ ProductManager.mustFindProductByContextAndFormId404(oc.id, productFormId)
      result  ← * <~ getAlbumsForProductInner(product)
    } yield result

  private def getAlbumsForProductInner(
      product: Product)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      albums ← * <~ ProductAlbumLinks.queryRightByLeft(product)
      images ← * <~ albums.map(album ⇒ AlbumImageLinks.queryRightByLeft(album.model))
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
      images ← * <~ albums.map(album ⇒ AlbumImageLinks.queryRightByLeft(album.model))
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
      links ← * <~ AlbumImageLinks.filterLeft(album).result
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
      link ← * <~ ProductAlbumLinks.createLast(product, fullAlbum.model)
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
      link ← * <~ SkuAlbumLinks.createLast(sku, fullAlbum.model)
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
      images ← * <~ AlbumImageLinks.queryRightByLeft(album.model)
    } yield AlbumResponse.build(album, images)

  def updateProductAlbumPosition(
      albumFormId: ObjectForm#Id,
      productFormId: ObjectForm#Id,
      position: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product ← * <~ Products
                 .filter(_.formId === productFormId)
                 .mustFindOneOr(ProductFailures.ProductFormNotFound(productFormId))
      album ← * <~ Albums
               .filter(_.formId === albumFormId)
               .mustFindOneOr(ImageFailures.AlbumNotFoundForContext(albumFormId, oc.id))
      updatedLink ← * <~ ProductAlbumLinks.updatePosition(product, album, position)
      albums      ← * <~ getAlbumsForProductInner(product)
    } yield albums

  def archiveByContextAndId(id: Int, contextName: String)(implicit ec: EC, db: DB) =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      albumObject ← * <~ mustFindFullAlbumByFormIdAndContext404(id, context)
      archiveResult ← * <~ Albums.update(albumObject.model,
                                         albumObject.model.copy(archivedAt = Some(Instant.now)))
      productLinks ← * <~ ProductAlbumLinks.filterRight(albumObject.model).result
      _ ← * <~ productLinks.map { link ⇒
           ProductAlbumLinks.deleteById(link.id,
                                        DbResultT.unit,
                                        id ⇒ NotFoundFailure400(ProductAlbumLinks, id))
         }
      skuLinks ← * <~ SkuAlbumLinks.filterRight(albumObject.model).result
      _ ← * <~ skuLinks.map { link ⇒
           SkuAlbumLinks.deleteById(link.id,
                                    DbResultT.unit,
                                    id ⇒ NotFoundFailure400(SkuAlbumLink, id))
         }
      images ← * <~ getAlbumImages(albumObject.model)
    } yield
      AlbumResponse.build(
          FullObject(model = archiveResult, form = albumObject.form, shadow = albumObject.shadow),
          images)

  def mustFindFullAlbumByFormIdAndContext404(id: ObjectForm#Id,
                                             context: ObjectContext)(implicit ec: EC, db: DB) =
    ObjectManager.getFullObject(mustFindAlbumByFormIdAndContext404(id, context))

  private def mustFindAlbumByFormIdAndContext404(id: Int, context: ObjectContext)(implicit ec: EC,
                                                                                  db: DB) =
    Albums
      .filterByContextAndFormId(context.id, id)
      .mustFindOneOr(AlbumNotFoundForContext(id, context.id))

  def getAlbumImages(album: Album)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      imageIds ← * <~ AlbumImageLinks.filterLeft(album).sortBy(_.position).map(_.rightId).result
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
