package models.product

import java.time.Instant

import failures._
import models.image._
import models.inventory._
import models.objects._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

/**
  * An IlluminatedProduct is what you get when you combine the product shadow and
  * the form.
  */
case class IlluminatedProduct(id: Int,
                              slug: String,
                              context: IlluminatedContext,
                              attributes: Json,
                              archivedAt: Option[Instant],
                              skus: Seq[IlluminatedSku] = Seq.empty,
                              variants: Seq[IlluminatedVariant] = Seq.empty,
                              albums: Seq[IlluminatedAlbum] = Seq.empty)

object IlluminatedProduct {
  def illuminate(context: ObjectContext,
                 product: Product,
                 form: ObjectForm,
                 shadow: ObjectShadow,
                 skus: Seq[IlluminatedSku] = Seq.empty,
                 variants: Seq[IlluminatedVariant] = Seq.empty,
                 albums: Seq[IlluminatedAlbum] = Seq.empty): IlluminatedProduct = {

    IlluminatedProduct(id = form.id,
                       slug = product.slug,
                       context = IlluminatedContext(context.name, context.attributes),
                       attributes =
                         IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes),
                       archivedAt = product.archivedAt,
                       skus = skus,
                       variants = variants,
                       albums = albums)
  }

  def findById(formId: Int)(implicit ec: EC, oc: OC): DbResultT[IlluminatedProduct] =
    for {
      coreProduct ← * <~ mustFindHeadProductById(formId)
      product     ← * <~ illuminateWithDependents(coreProduct)
    } yield product

  def create(product: IlluminatedProduct)(implicit ec: EC, oc: OC): DbResultT[IlluminatedProduct] = {
    DbResultT.pure(product)
  }

  private def illuminateWithDependents(product: FullObject[Product])(implicit ec: EC, oc: OC) =
    (for {
      skus     ← * <~ IlluminatedSku.findAllByProductId(product.model.id)
      variants ← * <~ IlluminatedVariant.findByProductId(product.model.id)
      albums   ← * <~ IlluminatedAlbum.findByProductId(product.model.id)
    } yield (product, skus, variants, albums)).map {
      case (product, skus, variants, albums) ⇒
        illuminate(oc, product.model, product.form, product.shadow, skus, variants, albums)
    }

  private def mustFindHeadProductById(formId: Int)(implicit ec: EC,
                                                   oc: OC): DbResultT[FullObject[Product]] =
    filterById(formId).one
      .mustFindOr(NotFoundFailure404(s"product $formId in context ${oc.id} not found"))
      .map { case (head, form, shadow) ⇒ FullObject(head, form, shadow) }

  private def filterById(formId: Int)(implicit oc: OC) =
    for {
      head   ← Products.filter(_.contextId === oc.id).filter(_.formId === formId)
      form   ← ObjectForms if head.formId === form.id
      shadow ← ObjectShadows if head.shadowId === shadow.id
    } yield (head, form, shadow)

}
