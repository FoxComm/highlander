package services.image

import java.io.File
import java.time.Instant

import scala.concurrent.Future
import scala.util.Try
import akka.http.scaladsl.model.Multipart
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, FileIO, Source}
import akka.util.ByteString

import cats.data.Xor.{left, right}
import cats.data.XorT
import cats.implicits._
import failures.GeneralFailure
import failures.ImageFailures._
import models.StoreAdmin
import models.image._
import models.objects._
import org.json4s.JsonAST.{JField, JNothing, JObject, JString, JValue}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.native.Serialization.{read, write}
import payloads.{AlbumPayload, ImagePayload}
import responses.ImageResponses._
import services.{AmazonS3, Result}
import services.inventory.SkuManager
import services.objects.ObjectManager
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._
import utils.IlluminateAlgorithm

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials

object ImageManager {
  type FullAlbum = FullObject[Album]

  def getAlbum(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[AlbumResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    album   ← * <~ getAlbumInner(id, context)
  } yield album).run()

  def getAlbumInner(id: Int, context: ObjectContext)
    (implicit ec: EC, db: DB): DbResultT[AlbumResponse.Root] = for {
    album   ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
    images  ← * <~ Image.buildFromAlbum(album)
  } yield AlbumResponse.build(album, images)


  def getAlbumsForProduct(productId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[Seq[AlbumResponse.Root]] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
    albums  ← * <~ getAlbumsForObject(product.shadowId, context, ObjectLink.ProductAlbum)
  } yield albums).run()

  def getAlbumsForSku(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[Seq[AlbumResponse.Root]] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
    albums  ← * <~ getAlbumsForObject(sku.shadowId, context, ObjectLink.SkuAlbum)
  } yield albums).run()

  def createAlbum(album: AlbumPayload, contextName: String)
    (implicit ec: EC, db: DB): Result[AlbumResponse.Root] = (for {
    context   ← * <~ ObjectManager.mustFindByName404(contextName)
    fullAlbum ← * <~ createAlbumInner(album, context)
    images    ← * <~ Image.buildFromAlbum(fullAlbum)
  } yield AlbumResponse.build(fullAlbum, images)).runTxn()

  def createAlbumInner(album: AlbumPayload, context: ObjectContext)
    (implicit ec: EC, db: DB): DbResultT[FullAlbum] = for {
    ins       ← * <~ ObjectUtils.insert(album.objectForm, album.objectShadow)
    album     ← * <~ Albums.create(Album(contextId = context.id,
      shadowId = ins.shadow.id, formId = ins.form.id, commitId = ins.commit.id))
  } yield FullObject(model = album, form = ins.form, shadow = ins.shadow)

  def createAlbumForProduct(admin: StoreAdmin, productId: Int, payload: AlbumPayload,
    contextName: String)(implicit ec: EC, db: DB, ac: AC): Result[AlbumResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    product ← * <~ ProductManager.mustFindProductByContextAndId404(context.id, productId)
    album   ← * <~ createAlbumInner(payload, context)
    images  ← * <~ Image.buildFromAlbum(album)
    link    ← * <~ ObjectLinks.create(ObjectLink(leftId = product.shadowId,
      rightId = album.shadow.id, linkType = ObjectLink.ProductAlbum))
  } yield AlbumResponse.build(album, images)).runTxn()

  def createAlbumForSku(admin: StoreAdmin, code: String, payload: AlbumPayload,
    contextName: String)(implicit ec: EC, db: DB, ac: AC): Result[AlbumResponse.Root] = (for {
    context ← * <~ ObjectManager.mustFindByName404(contextName)
    sku     ← * <~ SkuManager.mustFindSkuByContextAndCode(context.id, code)
    album   ← * <~ createAlbumInner(payload, context)
    images  ← * <~ Image.buildFromAlbum(album)
    link    ← * <~ ObjectLinks.create(ObjectLink(leftId = sku.shadowId,
      rightId = album.shadow.id, linkType = ObjectLink.SkuAlbum))
  } yield AlbumResponse.build(album, images)).runTxn()

  def updateAlbum(id: Int, album: AlbumPayload, contextName: String)
    (implicit ec: EC, db: DB): Result[AlbumResponse.Root] = {
    album.images match {
      case Some(_) ⇒
        val form = album.objectForm
        val shadow = album.objectShadow

        (for {
          context ← * <~ ObjectManager.mustFindByName404(contextName)
          album   ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
          update  ← * <~ ObjectUtils.update(album.form.id, album.shadow.id, form.attributes, shadow.attributes)
          upAlbum ← * <~ commitAlbumUpdate(album.model, update)
          images  ← * <~ Image.buildFromAlbum(upAlbum)
        } yield AlbumResponse.build(upAlbum, images)).runTxn()
      case None ⇒
        getAlbum(id, contextName)
    }
  }

  private def updateAlbumInner(id: Int, album: AlbumPayload, context: ObjectContext)
    (implicit ec: EC, db: DB): DbResultT[AlbumResponse.Root] = {
    album.images match {
      case Some(_) ⇒
        val form = album.objectForm
        val shadow = album.objectShadow

        for {
          album   ← * <~ mustFindFullAlbumByIdAndContext404(id, context)
          update  ← * <~ ObjectUtils.update(album.form.id, album.shadow.id, form.attributes, shadow.attributes)
          upAlbum ← * <~ commitAlbumUpdate(album.model, update)
          images  ← * <~ Image.buildFromAlbum(upAlbum)
        } yield AlbumResponse.build(upAlbum, images)
      case None ⇒
        getAlbumInner(id, context)
    }
  }

  def uploadImage(albumId: Int, contextName: String, bytes: Source[ByteString, Any])
    (implicit ec: EC, db: DB, am: ActorMaterializer): Result[AlbumResponse.Root] = {
    (for {
      context  ← * <~ ObjectManager.mustFindByName404(contextName)
      file     ← * <~ getFileFromRequest(bytes)
      album    ← * <~ mustFindFullAlbumByIdAndContext404(albumId, context)
      s3       ← * <~ AmazonS3.uploadFile("test.jpg", file)
      payload  = payloadForNewImage(album, s3)
      album    ← * <~ updateAlbumInner(albumId, payload, context)
    } yield album).runTxn()
  }

  private def getFileFromRequest(bytes: Source[ByteString, Any])
    (implicit ec: EC, am: ActorMaterializer) = {
    val file = File.createTempFile("debug", ".jpg")
    bytes.runWith(FileIO.toFile(file)).map { up ⇒
      if (up.wasSuccessful) right(file)
      else left(GeneralFailure("CC").single)
    }
  }

  private def payloadForNewImage(album: FullAlbum, imageUrl: String) = {
    val formAttrs = album.form.attributes
    val shadowAttrs = album.shadow.attributes

    val name = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
    val existingImages = IlluminateAlgorithm.get("images", formAttrs, shadowAttrs)
    val imageSeq = existingImages.extractOpt[Seq[ImagePayload]] match {
      case Some(images) ⇒ images :+ ImagePayload(src = imageUrl)
      case None ⇒         Seq(ImagePayload(src = imageUrl))
    }

    AlbumPayload(name = name, images = imageSeq.some)
  }

  private def getAlbumsForObject(shadowId: Int, context: ObjectContext,
    linkType: ObjectLink.LinkType)
    (implicit ec: EC, db: DB): DbResultT[Seq[AlbumResponse.Root]] = for {

    links   ← * <~ ObjectLinks.findByLeftAndType(shadowId, linkType).result
    albums  ← * <~ DbResultT.sequence(links.map { link ⇒
      for {
        shadow  ← * <~ ObjectShadows.mustFindById404(link.rightId)
        form    ← * <~ ObjectForms.mustFindById404(shadow.formId)
        album   ← * <~ mustFindAlbumByIdAndContext404(form.id, context)
        full    = FullObject(model = album, form = form, shadow = shadow)
        images  ← * <~ Image.buildFromAlbum(full)
      } yield AlbumResponse.build(full, images)
    })
  } yield albums

  private def commitAlbumUpdate(album: Album, up: ObjectUtils.UpdateResult)(implicit ec: EC) = {
    if (up.updated)
      for {
        commit ← * <~ ObjectCommits.create(ObjectCommit(formId = up.form.id, shadowId = up.shadow.id))
        update ← * <~ Albums.update(album, album.copy(shadowId = up.shadow.id, commitId = commit.id))
      } yield FullObject(model = update, form = up.form, shadow = up.shadow)
    else
      DbResultT.pure(FullObject(model = album, form = up.form, shadow = up.shadow))
  }

  private def mustFindFullAlbumByIdAndContext404(id: Int, context: ObjectContext)
    (implicit ec: EC, db: DB) = for {
    album   ← * <~ mustFindAlbumByIdAndContext404(id, context)
    form    ← * <~ ObjectForms.mustFindById404(album.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(album.shadowId)
  } yield FullObject(model = album, form = form, shadow = shadow)

  private def mustFindAlbumByIdAndContext404(id: Int, context: ObjectContext)
    (implicit ec: EC, db: DB) = for {
    album   ← * <~ Albums.filterByContextAndFormId(context.id, id).one.
      mustFindOr(AlbumNotFoundForContext(id, context.id))
  } yield album
}
