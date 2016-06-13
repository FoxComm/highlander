package services.image

import java.nio.file.Files

import akka.http.scaladsl.model.{HttpRequest, Multipart}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString

import cats.data.Xor
import cats.implicits._
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
import services.Result
import slick.driver.PostgresDriver.api._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.apis.Apis
import utils.db.DbResultT._
import utils.db._

object ImageManager {
  type FullAlbum = FullObject[Album]

  def getAlbum(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[AlbumRoot] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      album   ← * <~ getAlbumInner(id, context)
    } yield album).run()

  def getAlbumInner(id: Int, context: ObjectContext)(
      implicit ec: EC, db: DB): DbResultT[AlbumRoot] =
    for {
      album  ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      images ← * <~ Image.buildFromAlbum(album)
    } yield AlbumResponse.build(album, images)

  def getAlbumsForProduct(productId: Int, contextName: String)(
      implicit ec: EC, db: DB): Result[Seq[AlbumRoot]] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
      albums  ← * <~ getAlbumsForObject(product.shadowId, context, ObjectLink.ProductAlbum)
    } yield albums).run()

  def getAlbumsForSku(code: String, contextName: String)(
      implicit ec: EC, db: DB): Result[Seq[AlbumRoot]] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      albums  ← * <~ getAlbumsForObject(sku.shadowId, context, ObjectLink.SkuAlbum)
    } yield albums).run()

  def createAlbum(album: CreateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB): Result[AlbumRoot] =
    (for {
      context   ← * <~ ObjectManager.mustFindByName404(contextName)
      fullAlbum ← * <~ createAlbumInner(album, context)
      images    ← * <~ Image.buildFromAlbum(fullAlbum)
    } yield AlbumResponse.build(fullAlbum, images)).runTxn()

  def createAlbumInner(album: CreateAlbumPayload, context: ObjectContext)(
      implicit ec: EC, db: DB): DbResultT[FullAlbum] =
    for {
      ins ← * <~ ObjectUtils.insert(album.objectForm, album.objectShadow)
      album ← * <~ Albums.create(
                 Album(contextId = context.id,
                       shadowId = ins.shadow.id,
                       formId = ins.form.id,
                       commitId = ins.commit.id))
    } yield FullObject(model = album, form = ins.form, shadow = ins.shadow)

  def createAlbumForProduct(
      admin: StoreAdmin, productId: Int, payload: CreateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[AlbumRoot] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
      album   ← * <~ createAlbumInner(payload, context)
      images  ← * <~ Image.buildFromAlbum(album)
      link ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
                                                rightId = album.shadow.id,
                                                linkType = ObjectLink.ProductAlbum))
    } yield AlbumResponse.build(album, images)).runTxn()

  def createAlbumForSku(
      admin: StoreAdmin, code: String, payload: CreateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB, ac: AC): Result[AlbumRoot] =
    (for {
      context ← * <~ ObjectManager.mustFindByName404(contextName)
      sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
      album   ← * <~ createAlbumInner(payload, context)
      images  ← * <~ Image.buildFromAlbum(album)
      link ← * <~ ObjectLinks.create(ObjectLink(leftId = sku.shadowId,
                                                rightId = album.shadow.id,
                                                linkType = ObjectLink.SkuAlbum))
    } yield AlbumResponse.build(album, images)).runTxn()

  def updateAlbum(id: Int, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB): Result[AlbumRoot] =
    (for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      response ← * <~ updateAlbumInner(id, payload, contextName)
    } yield response).runTxn()

  def updateAlbumInner(id: Int, payload: UpdateAlbumPayload, contextName: String)(
      implicit ec: EC, db: DB): DbResultT[AlbumRoot] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      album ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      oldShadow  = album.shadow
      mergedAtts = oldShadow.attributes.merge(payload.objectShadow.attributes)
      updated ← * <~ ObjectUtils.update(album.model.formId,
                                        album.model.shadowId,
                                        payload.objectForm.attributes,
                                        mergedAtts,
                                        force = true)
      commit ← * <~ ObjectUtils.commit(updated)
      album  ← * <~ updateHead(album.model, updated.shadow, commit)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(
             Products, context.id, oldShadow.id, album.shadowId, ObjectLink.ProductAlbum)
      _ ← * <~ ObjectUtils.updateAssociatedLefts(
             Skus, context.id, oldShadow.id, album.shadowId, ObjectLink.SkuAlbum)
      album  ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
      images ← * <~ Image.buildFromAlbum(album)
    } yield AlbumResponse.build(album, images)

  def uploadImage(albumId: Int, contextName: String, request: HttpRequest)(
      implicit ec: EC, db: DB, am: Mat, apis: Apis): Result[AlbumRoot] = {
    Unmarshal(request.entity).to[Multipart.FormData].flatMap { formData ⇒
      val error: Result[AlbumRoot] = Result.failure(ImageNotFoundInPayload)
      formData.parts
        .filter(_.name == "upload-file")
        .runFold(error) { (_, part) ⇒
          (for {
            context  ← * <~ ObjectManager.mustFindByName404(contextName)
            filePath ← * <~ getFileFromRequest(part.entity.dataBytes)
            album    ← * <~ mustFindFullAlbumByIdAndContext404(albumId, context)
            filename ← * <~ getFileNameFromBodyPart(part)
            fullPath ← * <~ s"albums/${context.id}/$albumId/$filename"
            s3       ← * <~ apis.amazon.uploadFile(fullPath, filePath.toFile)
            payload = payloadForNewImage(album, s3, filename)
            album ← * <~ updateAlbumInner(albumId, payload, contextName)
          } yield album).runTxn()
        }
        .flatMap(a ⇒ a)
    }
  }

  private def getFileFromRequest(bytes: Source[ByteString, Any])(implicit ec: EC, am: Mat) = {
    val file = Files.createTempFile("tmp", ".jpg")
    bytes.runWith(FileIO.toPath(file)).map { ioResult ⇒
      if (ioResult.wasSuccessful) Xor.right(file)
      else Xor.left(ErrorReceivingImage.single)
    }
  }

  private def getFileNameFromBodyPart(part: Multipart.FormData.BodyPart)(implicit ec: EC) = {
    part.filename match {
      case Some(fileName) ⇒ Result.good(fileName)
      case None           ⇒ Result.failure(ImageFilenameNotFoundInPayload)
    }
  }

  private def payloadForNewImage(album: FullAlbum, imageUrl: String, filename: String) = {
    val formAttrs   = album.form.attributes
    val shadowAttrs = album.shadow.attributes

    val name           = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
    val existingImages = IlluminateAlgorithm.get("images", formAttrs, shadowAttrs)
    val payload        = ImagePayload(src = imageUrl, title = filename.some, alt = filename.some)
    val imageSeq = existingImages.extractOpt[Seq[ImagePayload]].foldLeft(Seq(payload)) {
      (payload, existing) ⇒
        existing ++ payload
    }

    UpdateAlbumPayload(name = name.some, images = imageSeq.some)
  }

  private def getAlbumsForObject(
      shadowId: Int, context: ObjectContext, linkType: ObjectLink.LinkType)(
      implicit ec: EC, db: DB): DbResultT[Seq[AlbumRoot]] =
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

  private def mustFindFullAlbumByIdAndContext404(id: Int, context: ObjectContext)(
      implicit ec: EC, db: DB) =
    for {
      album  ← * <~ mustFindAlbumByIdAndContext404(id, context)
      form   ← * <~ ObjectForms.mustFindById404(album.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(album.shadowId)
    } yield FullObject(model = album, form = form, shadow = shadow)

  private def mustFindAlbumByIdAndContext404(
      id: Int, context: ObjectContext)(implicit ec: EC, db: DB) =
    Albums
      .filterByContextAndFormId(context.id, id)
      .mustFindOneOr(AlbumNotFoundForContext(id, context.id))

  private def updateHead(album: Album, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Album] = maybeCommit match {
    case Some(commit) ⇒
      Albums.update(album, album.copy(shadowId = shadow.id, commitId = commit.id))
    case None ⇒
      DbResult.good(album)
  }
}
