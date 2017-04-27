package responses

import cats.implicits._
import io.circe.syntax._
import java.time.Instant
import models.inventory._
import models.objects._
import responses.AlbumResponses.AlbumResponse
import responses.ObjectResponses.ObjectContextResponse
import utils.aliases._
import utils.json.codecs._

object SkuResponses {

  object SkuHeadResponse {

    case class Root(code: String) extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(sku: Sku): Root =
      Root(code = sku.code)
  }

  object SkuFormResponse {

    case class Root(id: Int, code: String, attributes: Json, createdAt: Instant)
        extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(sku: Sku, form: ObjectForm): Root =
      Root(id = form.id, code = sku.code, attributes = form.attributes, createdAt = form.createdAt)
  }

  object SkuShadowResponse {

    case class Root(code: String, attributes: Json, createdAt: Instant) extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(sku: Sku, shadow: ObjectShadow): Root =
      Root(code = sku.code, attributes = shadow.attributes, createdAt = shadow.createdAt)
  }

  object IlluminatedSkuResponse {

    case class Root(code: String,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root])
        extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(s: IlluminatedSku): Root =
      Root(code = s.code,
           attributes = s.attributes,
           context = ObjectContextResponse.build(s.context).some,
           albums = Seq.empty)

    def build(ctx: ObjectContext, sku: FullObject[Sku], albums: Seq[AlbumResponse.Root]): Root = {
      val illuminatedSku = IlluminatedSku.illuminate(ctx, sku)
      Root(code = illuminatedSku.code,
           attributes = illuminatedSku.attributes,
           context = ObjectContextResponse.build(ctx).some,
           albums = albums)
    }

    def buildLite(s: IlluminatedSku): Root =
      Root(code = s.code, attributes = s.attributes, context = None, albums = Seq.empty)

    def buildLite(ctx: ObjectContext,
                  sku: FullObject[Sku],
                  albums: Seq[AlbumResponse.Root]): Root = {
      val illuminatedSku = IlluminatedSku.illuminate(ctx, sku)
      Root(code = illuminatedSku.code,
           attributes = illuminatedSku.attributes,
           context = None,
           albums = albums)
    }
  }

  object FullSkuResponse {

    case class Root(code: String,
                    context: ObjectContextResponse.Root,
                    form: SkuFormResponse.Root,
                    shadow: SkuShadowResponse.Root)

    def build(form: SkuFormResponse.Root,
              shadow: SkuShadowResponse.Root,
              context: ObjectContext): Root =
      Root(code = shadow.code,
           form = form,
           shadow = shadow,
           context = ObjectContextResponse.build(context))
  }

  object SkuResponse {

    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root],
                    archivedAt: Option[Instant])
        extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(sku: IlluminatedSku, albums: Seq[AlbumResponse.Root]): Root =
      Root(id = sku.id,
           archivedAt = sku.archivedAt,
           attributes = sku.attributes,
           context = ObjectContextResponse.build(sku.context).some,
           albums = albums)

    def buildLite(sku: IlluminatedSku, albums: Seq[AlbumResponse.Root]): Root =
      Root(id = sku.id,
           archivedAt = sku.archivedAt,
           attributes = sku.attributes,
           context = None,
           albums = albums)
  }
}
