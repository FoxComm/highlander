package entities

import models.image.{Album, Albums}
import models.inventory.{Sku, Skus}
import models.objects.ObjectContext
import models.objects.{ObjectCommit, ObjectCommits}
import models.objects.{ObjectForm, ObjectForms}
import models.objects.{ObjectShadow, ObjectShadows}
import models.objects.{ProductAlbumLinks, ProductSkuLinks, ProductVariantLinks}
import models.product.{Product ⇒ ProductHead, Products ⇒ ProductHeads}
import models.product.{Variant, Variants}
import slick.lifted.Rep
import utils.aliases.Json
import utils.db.ExPostgresDriver.api._

object ProductReference {
  def apply(id: Int): ProductReference      = ProductId(id)
  def apply(slug: String): ProductReference = ProductSlug(slug)
}

trait ProductReference
case class ProductId(id: ObjectForm#Id) extends ProductReference
case class ProductSlug(slug: String)    extends ProductReference

case class Product(id: Int,
                   commitId: Int,
                   slug: String,
                   title: String,
                   attributes: Json,
                   skus: Seq[Sku],
                   variants: Seq[Variant],
                   albums: Seq[Album])

class Products {
  type QueryCommitFn = () ⇒ Query[ObjectCommits, ObjectCommit, Seq]
  type QueryCore = Query[(ObjectForms, ObjectShadows, ObjectCommits),
                         (ObjectForm, ObjectShadow, ObjectCommit),
                         Seq]

  def filter(ref: ProductReference, contextId: ObjectContext#Id): QueryCore =
    filterCore(fnFilterCommitByHead(ref, contextId))

  def filterByCommit(commitId: Int): QueryCore =
    filterCore(fnFilterCommitById(commitId))

  private def filterFull(core: QueryCore, contextId: ObjectContext#Id) = {
    for {
      form     ← core.map(_._1)
      head     ← ProductHeads.filter(h ⇒ h.formId === form.id && h.contextId === contextId)
      skus     ← filterSkus(head.id)
      variants ← filterVariants(head.id)
    } yield form
  }

  private def filterSkus(productHeadId: Rep[Int]) =
    for {
      link ← ProductSkuLinks.filter(_.leftId === productHeadId)
      sku  ← Skus.filter(_.id === link.id)
    } yield sku

  private def filterVariants(productHeadId: Rep[Int]) =
    for {
      link    ← ProductVariantLinks.filter(_.leftId === productHeadId)
      variant ← Variants.filter(_.id === link.id)
    } yield variant

  private def filterAlbums(productHeadId: Rep[Int]) =
    for {
      link  ← ProductAlbumLinks.filter(_.leftId === productHeadId)
      album ← Albums.filter(_.id === link.id)
    } yield album

  private def fnFilterCommitByHead(ref: ProductReference,
                                   contextId: ObjectContext#Id): QueryCommitFn = {
    val headQ = ref match {
      case ProductId(id) ⇒
        ProductHeads.filter(_.id === id)
      case ProductSlug(slug) ⇒
        ProductHeads.filter(_.slug.toLowerCase === slug.toLowerCase)
    }

    () ⇒
      for {
        head   ← headQ
        commit ← ObjectCommits if head.commitId === commit.id
      } yield commit
  }

  private def fnFilterCommitById(commitId: Int): QueryCommitFn =
    () ⇒ ObjectCommits.filter(_.id === commitId)

  private def filterCore(filterCommit: QueryCommitFn): QueryCore =
    for {
      commit ← filterCommit()
      form   ← ObjectForms if commit.formId === form.id
      shadow ← ObjectShadows if commit.shadowId === shadow.id
    } yield (form, shadow, commit)
}
