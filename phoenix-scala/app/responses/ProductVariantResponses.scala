package responses

import java.time.Instant
import cats.implicits._
import models.inventory._
import models.objects._
import responses.AlbumResponses.AlbumResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductOptionResponses.ProductOptionResponse
import utils.aliases._

object ProductVariantResponses {

  object SkuHeadResponse {

    case class Root(code: String) extends ResponseItem

    def build(variant: ProductVariant): Root =
      Root(code = variant.code)
  }

  object ProductVariantFormResponse {

    case class Root(id: Int, code: String, attributes: Json, createdAt: Instant)
        extends ResponseItem

    def build(variant: ProductVariant, form: ObjectForm): Root =
      Root(id = form.id,
           code = variant.code,
           attributes = form.attributes,
           createdAt = form.createdAt)
  }

  object ProductVariantShadowResponse {

    case class Root(code: String, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(variant: ProductVariant, shadow: ObjectShadow): Root =
      Root(code = variant.code, attributes = shadow.attributes, createdAt = shadow.createdAt)
  }

  object IlluminatedProductVariantResponse {

    case class Root(code: String,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root])
        extends ResponseItem

    def build(s: IlluminatedVariant): Root =
      Root(code = s.code,
           attributes = s.attributes,
           context = ObjectContextResponse.build(s.context).some,
           albums = Seq.empty)

    def build(ctx: ObjectContext,
              variant: FullObject[ProductVariant],
              albums: Seq[AlbumResponse.Root]): Root = {
      val illuminatedVariant = IlluminatedVariant.illuminate(ctx, variant)
      Root(code = illuminatedVariant.code,
           attributes = illuminatedVariant.attributes,
           context = ObjectContextResponse.build(ctx).some,
           albums = albums)
    }

    def buildLite(s: IlluminatedVariant): Root =
      Root(code = s.code, attributes = s.attributes, context = None, albums = Seq.empty)

    def buildLite(ctx: ObjectContext,
                  variant: FullObject[ProductVariant],
                  albums: Seq[AlbumResponse.Root]): Root = {
      val illuminatedVariant = IlluminatedVariant.illuminate(ctx, variant)
      Root(code = illuminatedVariant.code,
           attributes = illuminatedVariant.attributes,
           context = None,
           albums = albums)
    }
  }

  object FullProductVariantResponse {

    case class Root(code: String,
                    context: ObjectContextResponse.Root,
                    form: ProductVariantFormResponse.Root,
                    shadow: ProductVariantShadowResponse.Root)

    def build(form: ProductVariantFormResponse.Root,
              shadow: ProductVariantShadowResponse.Root,
              context: ObjectContext): Root =
      Root(code = shadow.code,
           form = form,
           shadow = shadow,
           context = ObjectContextResponse.build(context))
  }

  object ProductVariantResponse {

    case class Root(id: Int,
                    context: Option[ObjectContextResponse.Root],
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root],
                    options: Seq[ProductOptionResponse.Root],
                    middlewarehouseSkuId: Int,
                    archivedAt: Option[Instant])
        extends ResponseItem

    def build(variant: IlluminatedVariant,
              albums: Seq[AlbumResponse.Root],
              middlewarehouseSkuId: Int,
              options: Seq[ProductOptionResponse.Root]): Root =
      Root(id = variant.id,
           archivedAt = variant.archivedAt,
           attributes = variant.attributes,
           context = ObjectContextResponse.build(variant.context).some,
           middlewarehouseSkuId = middlewarehouseSkuId,
           albums = albums,
           options = options)

    def buildLite(variant: IlluminatedVariant,
                  albums: Seq[AlbumResponse.Root],
                  middlewarehouseSkuId: Int,
                  options: Seq[ProductOptionResponse.Root]): Root =
      Root(id = variant.id,
           archivedAt = variant.archivedAt,
           attributes = variant.attributes,
           context = None,
           middlewarehouseSkuId = middlewarehouseSkuId,
           albums = albums,
           options = options)
  }
}
