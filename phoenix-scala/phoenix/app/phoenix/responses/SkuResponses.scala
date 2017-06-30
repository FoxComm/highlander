package phoenix.responses

import java.time.Instant

import cats.implicits._
import core.failures.{Failure, NotFoundFailure404}
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.inventory._
import phoenix.models.traits.IlluminatedModel
import phoenix.responses.AlbumResponses.AlbumResponse
import phoenix.utils.aliases._

object SkuResponses {

  case class SkuHeadResponse(code: String) extends ResponseItem

  object SkuHeadResponse {

    def build(sku: Sku): SkuHeadResponse =
      SkuHeadResponse(code = sku.code)
  }

  case class SkuFormResponse(id: Int, code: String, attributes: Json, createdAt: Instant) extends ResponseItem

  object SkuFormResponse {

    def build(sku: Sku, form: ObjectForm): SkuFormResponse =
      SkuFormResponse(id = form.id, code = sku.code, attributes = form.attributes, createdAt = form.createdAt)
  }

  case class SkuShadowResponse(code: String, attributes: Json, createdAt: Instant) extends ResponseItem

  object SkuShadowResponse {

    def build(sku: Sku, shadow: ObjectShadow): SkuShadowResponse =
      SkuShadowResponse(code = sku.code, attributes = shadow.attributes, createdAt = shadow.createdAt)
  }

  case class IlluminatedSkuResponse(code: String,
                                    context: Option[ObjectContextResponse],
                                    attributes: Json,
                                    albums: Seq[AlbumResponse])
      extends ResponseItem

  object IlluminatedSkuResponse {

    def build(s: IlluminatedSku): IlluminatedSkuResponse =
      IlluminatedSkuResponse(code = s.code,
                             attributes = s.attributes,
                             context = ObjectContextResponse.build(s.context).some,
                             albums = Seq.empty)

    def build(ctx: ObjectContext, sku: FullObject[Sku], albums: Seq[AlbumResponse]): IlluminatedSkuResponse = {
      val illuminatedSku = IlluminatedSku.illuminate(ctx, sku)
      IlluminatedSkuResponse(code = illuminatedSku.code,
                             attributes = illuminatedSku.attributes,
                             context = ObjectContextResponse.build(ctx).some,
                             albums = albums)
    }

    def buildLite(s: IlluminatedSku): IlluminatedSkuResponse =
      IlluminatedSkuResponse(code = s.code, attributes = s.attributes, context = None, albums = Seq.empty)

    def buildLite(ctx: ObjectContext,
                  sku: FullObject[Sku],
                  albums: Seq[AlbumResponse]): IlluminatedSkuResponse = {
      val illuminatedSku = IlluminatedSku.illuminate(ctx, sku)
      IlluminatedSkuResponse(code = illuminatedSku.code,
                             attributes = illuminatedSku.attributes,
                             context = None,
                             albums = albums)
    }
  }

  case class FullSkuResponse(code: String,
                             context: ObjectContextResponse,
                             form: SkuFormResponse,
                             shadow: SkuShadowResponse)
  object FullSkuResponse {

    def build(form: SkuFormResponse, shadow: SkuShadowResponse, context: ObjectContext): FullSkuResponse =
      FullSkuResponse(code = shadow.code,
                      form = form,
                      shadow = shadow,
                      context = ObjectContextResponse.build(context))
  }

  case class SkuResponse(id: Int,
                         context: Option[ObjectContextResponse],
                         attributes: Json,
                         albums: Seq[AlbumResponse],
                         archivedAt: Option[Instant])
      extends ResponseItem
      with IlluminatedModel[SkuResponse] {
    override def inactiveError: Failure =
      NotFoundFailure404(Sku, (attributes \ "code" \ "v").extract[String])
  }

  object SkuResponse {

    def build(sku: IlluminatedSku, albums: Seq[AlbumResponse]): SkuResponse =
      SkuResponse(id = sku.id,
                  archivedAt = sku.archivedAt,
                  attributes = sku.attributes,
                  context = ObjectContextResponse.build(sku.context).some,
                  albums = albums)

    def buildLite(sku: IlluminatedSku, albums: Seq[AlbumResponse]): SkuResponse =
      SkuResponse(id = sku.id,
                  archivedAt = sku.archivedAt,
                  attributes = sku.attributes,
                  context = None,
                  albums = albums)
  }
}
