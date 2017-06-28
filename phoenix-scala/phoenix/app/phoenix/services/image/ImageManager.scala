package phoenix.services.image

import java.time.Instant

import cats.implicits._
import core.db._
import objectframework.ObjectUtils
import objectframework.ObjectUtils.InsertResult
import objectframework.models._
import objectframework.services.ObjectManager
import org.json4s.JsonAST.JString
import phoenix.failures.ImageFailures._
import phoenix.models.account._
import phoenix.models.image._
import phoenix.models.inventory.Sku
import phoenix.models.objects._
import phoenix.models.product._
import phoenix.payloads.ImagePayloads._
import phoenix.responses.AlbumResponses.AlbumResponse.{Root ⇒ AlbumRoot}
import phoenix.responses.AlbumResponses._
import phoenix.services.inventory.SkuManager
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object ImageManager {
  type FullAlbum           = FullObject[Album]
  type FullAlbumWithImages = (FullObject[Album], Seq[FullObject[Image]])

  def getAlbum(formId: ObjectForm#Id, contextName: String)(implicit ec: EC, db: DB): DbResultT[AlbumRoot] =
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
      productReference: ProductReference)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product ← * <~ Products.mustFindByReference(productReference)
      result  ← * <~ getAlbumsForProductInner(product)
    } yield result

  def getAlbumsForProductInner(product: Product)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      albums ← * <~ ProductAlbumLinks.queryRightByLeft(product)
      images ← * <~ albums.map(album ⇒ AlbumImageLinks.queryRightByLeft(album.model))
      result ← * <~ albums.zip(images).map {
                case (album, image) ⇒ AlbumResponse.build(album, image)
              }
    } yield result

  def getAlbumsForSku(code: String)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      albums ← * <~ getAlbumsForSkuInner(code, oc)
    } yield albums

  def getAlbumsForSkuInner(code: String, context: ObjectContext)(implicit ec: EC,
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

  def createAlbum(album: AlbumPayload,
                  contextName: String)(implicit ec: EC, db: DB, au: AU): DbResultT[AlbumRoot] =
    for {
      context        ← * <~ ObjectManager.mustFindByName404(contextName)
      createdObjects ← * <~ createAlbumInner(album, context)
      (album, images) = createdObjects
    } yield AlbumResponse.build(album, images)

  def createAlbumInner(
      payload: AlbumPayload,
      context: ObjectContext)(implicit ec: EC, db: DB, au: AU): DbResultT[FullAlbumWithImages] =
    for {
      album ← * <~ ObjectUtils.insertFullObject(payload.formAndShadow,
                                                ins ⇒ createAlbumHeadFromInsert(context, ins, payload.scope))
      images ← * <~ (payload.images match {
                case Some(imagesPayload) ⇒
                  createImagesForAlbum(album.model, imagesPayload, context)
                case None ⇒
                  Seq.empty.pure[DbResultT]
              })
    } yield (album, images)

  def createOrUpdateImagesForAlbum(album: Album, imagesPayload: Seq[ImagePayload], context: ObjectContext)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[Seq[FullObject[Image]]] =
    for {
      updatedImages ← * <~ imagesPayload.zipWithIndex.map {
                       case (payload, index) ⇒
                         createOrUpdateImageForAlbum(album, payload, index, context)
                     }
      imageIds = updatedImages.map(_.model.id).toSet
      links ← * <~ AlbumImageLinks.filterLeft(album).result
      linksToDelete = links.filter(link ⇒ !imageIds.contains(link.rightId))
      _ ← * <~ AlbumImageLinks.filter(_.id inSet linksToDelete.map(_.id)).delete
      _ ← * <~ Images.filterByIds(linksToDelete.map(_.rightId)).delete
    } yield updatedImages

  def createOrUpdateImages(
      imagesPayload: Seq[ImagePayload],
      context: ObjectContext)(implicit ec: EC, db: DB, au: AU): DbResultT[Seq[FullObject[Image]]] =
    for {
      images ← * <~ imagesPayload.map { payload ⇒
                payload.id match {
                  case None ⇒
                    for {
                      _ ← * <~ payload.validate
                      inserted ← * <~ ObjectUtils.insertFullObject(
                                  payload.formAndShadow,
                                  ins ⇒ createImageHeadFromInsert(context, ins, payload.scope))
                    } yield inserted
                  case Some(id) ⇒
                    for {
                      _     ← * <~ payload.validate
                      image ← * <~ ObjectManager.getFullObject(Images.mustFindById404(id))
                      (newForm, newShadow) = payload.formAndShadow.tupled
                      updated ← * <~ ObjectUtils.commitUpdate(image,
                                                              newForm.attributes,
                                                              newShadow.attributes,
                                                              updateImageHead)
                    } yield updated
                }
              }

    } yield images

  def createOrUpdateImageForAlbum(album: Album, payload: ImagePayload, position: Int, context: ObjectContext)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullObject[Image]] =
    payload.id match {
      case None ⇒
        for {
          _ ← * <~ payload.validate
          inserted ← * <~ ObjectUtils.insertFullObject(
                      payload.formAndShadow,
                      ins ⇒ createImageHeadFromInsert(context, ins, payload.scope))
          _ ← * <~ AlbumImageLinks.create(
               AlbumImageLink(leftId = album.id, position = position, rightId = inserted.model.id))
        } yield inserted
      case Some(id) ⇒
        for {
          _     ← * <~ payload.validate
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
      db: DB,
      au: AU): DbResultT[Seq[FullObject[Image]]] =
    for {
      images ← * <~ imagesPayload.map(
                img ⇒
                  ObjectUtils.insertFullObject(img.formAndShadow,
                                               ins ⇒ createImageHeadFromInsert(context, ins, img.scope)))
      links ← * <~ images.zipWithIndex.map {
               case (image, index) ⇒
                 AlbumImageLinks.create(
                   AlbumImageLink(leftId = album.id, position = index, rightId = image.model.id))
             }
    } yield images

  def createAlbumForProduct(
      admin: User,
      productId: ProductReference,
      payload: AlbumPayload)(implicit ec: EC, db: DB, ac: AC, au: AU, oc: OC): DbResultT[AlbumRoot] =
    for {
      product ← * <~ Products.mustFindByReference(productId)
      created ← * <~ createAlbumInner(payload, oc)
      (fullAlbum, images) = created
      link ← * <~ ProductAlbumLinks.createLast(product, fullAlbum.model)
    } yield AlbumResponse.build(fullAlbum, images)

  def createAlbumForSku(
      admin: User,
      code: String,
      payload: AlbumPayload)(implicit ec: EC, db: DB, ac: AC, oc: OC, au: AU): DbResultT[AlbumRoot] =
    for {
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(oc.id, code)
      created ← * <~ createAlbumInner(payload, oc)
      (fullAlbum, images) = created
      link ← * <~ SkuAlbumLinks.createLast(sku, fullAlbum.model)
    } yield AlbumResponse.build(fullAlbum, images)

  def updateAlbum(id: ObjectForm#Id,
                  payload: AlbumPayload,
                  contextName: String)(implicit ec: EC, db: DB, au: AU): DbResultT[AlbumRoot] =
    for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ updateAlbumInner(id, payload, context)
    } yield AlbumResponse.build(response)

  def updateAlbumInner(id: ObjectForm#Id, payload: AlbumPayload, context: ObjectContext)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[FullAlbumWithImages] =
    for {
      album ← * <~ mustFindFullAlbumByFormIdAndContext404(id, context)
      oldShadow                    = album.shadow
      (payloadForm, payloadShadow) = payload.formAndShadow.tupled
      mergedAtts                   = oldShadow.attributes.merge(payloadShadow.attributes)
      album ← * <~ ObjectUtils
               .commitUpdate[Album](album, payloadForm.attributes, mergedAtts, updateAlbumHead, force = true)
      _      ← * <~ createOrUpdateImagesForAlbum(album.model, payload.images.getOrElse(Seq.empty), context)
      images ← * <~ AlbumImageLinks.queryRightByLeft(album.model)
    } yield (album, images)

  def updateProductAlbumPosition(albumFormId: ObjectForm#Id,
                                 productRef: ProductReference,
                                 position: Int)(implicit ec: EC, db: DB, oc: OC): DbResultT[Seq[AlbumRoot]] =
    for {
      product     ← * <~ Products.mustFindByReference(productRef)
      album       ← * <~ ImageManager.mustFindAlbumByFormIdAndContext404(albumFormId, oc)
      updatedLink ← * <~ ProductAlbumLinks.updatePosition(product, album, position)
      albums      ← * <~ getAlbumsForProductInner(product)
    } yield albums

  def updateOrCreateAlbum(
      payload: AlbumPayload)(implicit ec: EC, db: DB, oc: OC, au: AU): DbResultT[FullAlbumWithImages] =
    payload.id match {
      case Some(id) ⇒ updateAlbumInner(id, payload, oc)
      case None     ⇒ createAlbumInner(payload, oc)
    }

  def archiveByContextAndId(id: Int, contextName: String)(implicit ec: EC, db: DB) =
    for {
      context     ← * <~ ObjectManager.mustFindByName404(contextName)
      albumObject ← * <~ mustFindFullAlbumByFormIdAndContext404(id, context)
      archiveResult ← * <~ Albums.update(albumObject.model,
                                         albumObject.model.copy(archivedAt = Some(Instant.now)))
      _      ← * <~ ProductAlbumLinks.filterRight(albumObject.model).delete
      _      ← * <~ SkuAlbumLinks.filterRight(albumObject.model).delete
      images ← * <~ getAlbumImages(albumObject.model)
    } yield
      AlbumResponse.build(
        FullObject(model = archiveResult, form = albumObject.form, shadow = albumObject.shadow),
        images)

  def mustFindFullAlbumByFormIdAndContext404(id: ObjectForm#Id, context: ObjectContext)(implicit ec: EC,
                                                                                        db: DB) =
    ObjectManager.getFullObject(mustFindAlbumByFormIdAndContext404(id, context))

  def mustFindAlbumByFormIdAndContext404(id: Int, context: ObjectContext)(implicit ec: EC, db: DB) =
    Albums
      .filterByContextAndFormId(context.id, id)
      .mustFindOneOr(AlbumNotFoundForContext(id, context.id))

  def getAlbumImages(album: Album)(implicit ec: EC, db: DB): DbResultT[Seq[FullObject[Image]]] =
    for {
      imageIds ← * <~ AlbumImageLinks.filterLeft(album).sortBy(_.position).map(_.rightId).result
      images   ← * <~ imageIds.map(imgId ⇒ ObjectManager.getFullObject(Images.mustFindById404(imgId)))
    } yield images

  def getFirstImageForAlbum(album: Album)(implicit ec: EC, db: DB): DbResultT[Option[String]] =
    for {
      imageLink ← * <~ AlbumImageLinks.filterLeft(album).sortBy(_.position).one.dbresult
      src ← * <~ imageLink.flatTraverse { link ⇒
             for {
               fullImage ← * <~ ObjectManager.getFullObject(Images.mustFindById404(link.rightId))
             } yield
               ObjectUtils.get("src", fullImage.form, fullImage.shadow) match {
                 case JString(src) ⇒ src.some
                 case _            ⇒ None
               }
           }
    } yield src

  private def createAlbumHeadFromInsert(oc: ObjectContext, insert: InsertResult, maybeScope: Option[String])(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[Album] =
    for {
      scope ← * <~ Scope.resolveOverride(maybeScope)
      album ← * <~ Albums.create(
               Album(scope = scope,
                     contextId = oc.id,
                     shadowId = insert.shadow.id,
                     formId = insert.form.id,
                     commitId = insert.commit.id))
    } yield album

  private def updateAlbumHead(fullObject: FullObject[Album],
                              commitId: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[Album]] =
    for {
      newModel ← * <~ Albums.update(
                  fullObject.model,
                  fullObject.model.copy(shadowId = fullObject.shadow.id, commitId = commitId))
    } yield fullObject.copy(model = newModel)

  private def createImageHeadFromInsert(oc: ObjectContext, ins: InsertResult, maybeScope: Option[String])(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[Image] =
    for {
      scope ← * <~ Scope.resolveOverride(maybeScope)
      image ← * <~ Images.create(
               Image(scope = scope,
                     contextId = oc.id,
                     shadowId = ins.shadow.id,
                     formId = ins.form.id,
                     commitId = ins.commit.id))
    } yield image

  private def updateImageHead(fullObject: FullObject[Image],
                              commitId: Int)(implicit ec: EC, db: DB): DbResultT[FullObject[Image]] =
    for {
      newModel ← * <~ Images.update(
                  fullObject.model,
                  fullObject.model.copy(shadowId = fullObject.shadow.id, commitId = commitId))
    } yield fullObject.copy(model = newModel)
}
