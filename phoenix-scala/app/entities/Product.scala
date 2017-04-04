package entities

import models.inventory.Sku
import models.objects.ObjectContext
import models.objects.{ObjectCommit, ObjectCommits}
import models.objects.{ObjectForm, ObjectForms}
import models.objects.{ObjectShadow, ObjectShadows}
import models.product.{Products ⇒ ProductHeads}
import models.product.Variant
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
                   variants: Seq[Variant])

class Products {
  type QueryCommit = Query[ObjectCommits, ObjectCommit, Seq]
  // def filter(ref: ProductReference, contextId: ObjectContext#Id) =

  private def filterCommitByHead(ref: ProductReference, contextId: ObjectContext#Id) = {
    val commitQ = ref match {
      case ProductId(id) ⇒
        ProductHeads.filter(_.id === id)
      case ProductSlug(slug) ⇒
        ProductHeads.filter(_.slug.toLowerCase === slug.toLowerCase)
    }

    commitQ.map(_.commitId)
  }

  private def filterCommitId(commitId: Int) = {
    def commitQ() = ObjectCommits.filter(_.id === commitId)
    filterCore(commitQ)
  }

  private def filterCore(filterCommit: () ⇒ QueryCommit) =
    for {
      commit ← filterCommit()
      form   ← ObjectForms if commit.formId === form.id
      shadow ← ObjectShadows if commit.shadowId === shadow.id
    } yield (form, shadow, commit)
}
