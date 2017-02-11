package responses

import java.time.Instant
import cats.implicits._
import models.inventory._
import models.objects._
import responses.AlbumResponses.AlbumResponse
import responses.ObjectResponses.ObjectContextResponse
import responses.ProductResponses.ProductResponse
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
                    context: ObjectContextResponse.Root,
                    attributes: Json,
                    product: ProductResponse.Partial,
                    albums: Seq[AlbumResponse.Root],
                    options: Seq[ProductOptionResponse.Partial],
                    skuId: Int,
                    skuCode: String,
                    archivedAt: Option[Instant])
        extends ResponseItem

    case class Partial(id: Int,
                       attributes: Json,
                       albums: Seq[AlbumResponse.Root],
                       options: Seq[ProductOptionResponse.Partial],
                       skuId: Int,
                       skuCode: String,
                       archivedAt: Option[Instant])
        extends ResponseItem

    def build(variant: IlluminatedVariant,
              product: ProductResponse.Partial,
              albums: Seq[AlbumResponse.Root],
              skuId: Int,
              skuCode: String,
              options: Seq[ProductOptionResponse.Partial]): Root =
      Root(id = variant.id,
           archivedAt = variant.archivedAt,
           attributes = variant.attributes,
           context = ObjectContextResponse.build(variant.context),
           skuId = skuId,
           skuCode = skuCode,
           product = product,
           albums = albums,
           options = options)

    def buildPartial(variant: IlluminatedVariant,
                     albums: Seq[AlbumResponse.Root],
                     skuId: Int,
                     skuCode: String,
                     options: Seq[ProductOptionResponse.Partial]): Partial =
      Partial(id = variant.id,
              archivedAt = variant.archivedAt,
              attributes = variant.attributes,
              skuId = skuId,
              skuCode = skuCode,
              albums = albums,
              options = options)
  }
}
