package entities

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import failures._
import models.objects._
import models.taxonomy._
import payloads.CategoryPayloads._
import payloads.ObjectPayloads.{AttributesBuilder, StringField}
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases._
import utils.db._
import utils.db.ExPostgresDriver.api._

case class Category(id: Int,
                    scope: LTree,
                    contextId: Int,
                    parentId: Option[Int],
                    name: String,
                    position: Int,
                    headId: Option[Int])

case class RawCategory(head: Taxon,
                       form: ObjectForm,
                       shadow: ObjectShadow,
                       link: TaxonomyTaxonLink) {
  type RawTuple = (Taxon, ObjectForm, ObjectShadow, TaxonomyTaxonLink)

  def apply(rt: RawTuple): RawCategory =
    RawCategory(head = rt._1, form = rt._2, shadow = rt._3, link = rt._4)
}

object Category {
  implicit val format = JsonFormatters.phoenixFormats

  def fromCreatePayload(payload: CreateCategoryPayload, contextId: Int, scope: LTree): Category = {
    val parentId = payload.location.fold(None: Option[Int])(_.parent)
    val position = payload.location.fold(0)(_.position.getOrElse(0))
    Category(id = 0,
             scope = scope,
             contextId = contextId,
             parentId = parentId,
             name = payload.name,
             position = position,
             headId = None)
  }

  def fromRaw(raw: RawCategory): Category = {
    val name =
      IlluminateAlgorithm.get("name", raw.form.attributes, raw.shadow.attributes).extract[String]
    Category(id = raw.form.id,
             scope = raw.head.scope,
             contextId = raw.head.contextId,
             parentId = None,
             name = name,
             position = raw.link.position,
             headId = raw.head.id.some)
  }
}

object Categories {
  type QuerySeq = Query[(Taxons, ObjectForms, ObjectShadows, TaxonomyTaxonLinks),
                        (Taxon, ObjectForm, ObjectShadow, TaxonomyTaxonLink),
                        Seq]

  def filterById(id: Int, contextId: Int, taxonomyId: Int): QuerySeq =
    for {
      link   ← TaxonomyTaxonLinks.filter(_.taxonomyId === taxonomyId).filter(_.taxonId === id)
      head   ← Taxons.filter(_.formId === link.taxonId).filter(_.contextId === contextId)
      form   ← ObjectForms if head.formId === form.id
      shadow ← ObjectShadows if head.shadowId === shadow.id
    } yield (head, form, shadow, link

  def mustFindById(id: Int, contextId: Int, taxonomyId: Int)(
      implicit ec: EC): DbResultT[Category] =
    for {
      raw ← * <~ filterById(id, contextId, taxonomyId).mustFindOneOr(
               NotFoundFailure404(Category, id))
    } yield Category.fromRaw(RawCategory.tupled(raw))

  def filterChildren(category: Category) = {}

  def create(category: Category)(implicit ec: EC) = {
    val jsonBuilder: AttributesBuilder = AttributesBuilder(StringField("name", category.name))
    val form                           = ObjectForm(kind = Taxon.kind, attributes = jsonBuilder.objectForm)
    val shadow                         = ObjectShadow(attributes = jsonBuilder.objectShadow)

    for {
      insertResult ← * <~ ObjectUtils.insert(form, shadow)
      head ← * <~ Taxons.create(
                Taxon(scope = category.scope,
                      contextId = category.contextId,
                      shadowId = insertResult.shadow.id,
                      formId = insertResult.form.id,
                      commitId = insertResult.commit.id))
    } yield insertResult
  }
}
