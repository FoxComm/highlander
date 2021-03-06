import testutils._
import testutils.fixtures.api.ApiFixtures

import org.json4s._
import org.json4s.jackson.JsonMethods._
import objectframework.content._
import objectframework.failures._
import objectframework.payloads.ContentPayloads._
import objectframework.services._
import objectframework.ObjectFailures._
import phoenix.models.product.SimpleContext
import phoenix.utils.JsonFormatters

// Ideally this code should live under the objectframework module. However,
// it currently lives here to take advantage of the test infrastructure and DB
// migration logic that lives in the main integration tests. At some point,
// moving it back to that project will make sense once we have a better idea
// of how modules will do things like manage the database.
// -- Jeff

class ContentManagerTest extends IntegrationTestBase with TestObjectContext with ApiFixtures {

  "ContentManager.findLatest" - {
    "successfully finds with no relations" in new SkuFixture {
      val content = ContentManager.findLatest(sku.id, SimpleContext.id, sku.kind).gimme
      content.kind must === ("sku")
      content.attributes("code").v must === (JString("TEST-SKU"))
    }

    "fails with 404 when invalid id" in new SkuFixture {
      val failures = ContentManager.findLatest(2, SimpleContext.id, sku.kind).gimmeFailures
      failures.head must === (ObjectNotFound(sku.kind, 2, SimpleContext.id))
    }

    "fails with 404 when invalid kind" in new SkuFixture {
      val failures = ContentManager.findLatest(sku.id, SimpleContext.id, "variant").gimmeFailures
      failures.head must === (ObjectNotFound("variant", sku.id, SimpleContext.id))
    }
  }

  "ContentManager.findByCommit" - {
    "successfully with no relations" in new SkuFixture {
      val content = ContentManager.findByCommit(sku.commitId, sku.kind).gimme
      content.kind must === ("sku")
      content.attributes("code").v must === (JString("TEST-SKU"))
    }

    "fails with 404 when invalid commit id" in new SkuFixture {
      val failures = ContentManager.findByCommit(2, sku.kind).gimmeFailures
      failures.head must === (ObjectNotFoundAtCommit(sku.kind, 2))
    }

    "fails with 404 when invalid kind" in new SkuFixture {
      val failures = ContentManager.findByCommit(sku.commitId, "variant").gimmeFailures
      failures.head must === (ObjectNotFoundAtCommit("variant", sku.commitId))
    }
  }

  "ContentManager.create" - {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    "successfully with no relations" in new Fixture {
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = relations)

      val content = ContentManager.create(SimpleContext.id, payload).gimme
      content.attributes("title").v must === (JString("a test product"))
    }

    "successfully with a relation" in new SkuFixture {
      val productRelations = Map("sku" → Seq(sku.commitId))
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = productRelations)

      val content = ContentManager.create(SimpleContext.id, payload).gimme
      content.attributes("title").v must === (JString("a test product"))
      content.relations("sku") must === (Seq(sku.commitId))
    }

    "fails if the relation does not exist" in new Fixture {
      val productRelations = Map("sku" → Seq(1))
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("sku", 1))
    }

    "fails if the relation has the wrong commit id" in new SkuFixture {
      val productRelations = Map("sku" → Seq(2))
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("sku", 2))
    }

    "fails if the relation has the wrong kind" in new SkuFixture {
      val productRelations = Map("variant" → Seq(1))
      val payload =
        CreateContentPayload(kind = "product", attributes = attributes, relations = productRelations)

      val failures = ContentManager.create(SimpleContext.id, payload).gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("variant", 1))
    }

  }

  "ContentManager.update" - {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    "successfully with no relations" in new SkuFixture {
      val newSkuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKEW")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newSkuAttributes), relations = None)

      val content = ContentManager.update(sku.id, SimpleContext.id, updatePayload, "sku").gimme
      content.attributes("code").v must === (JString("TEST-SKEW"))
    }

    "successfully with relations" in new ProductFixture {
      val newProductAttributes = attributes + ("title" → ContentAttribute(t = "string",
                                                                          v = JString("new name")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newProductAttributes), relations = None)

      val content =
        ContentManager.update(product.id, SimpleContext.id, updatePayload, "product").gimme
      content.attributes("title").v must === (JString("new name"))
      content.attributes("description").v must === (JString("<p>A test description</p>"))
    }

    "successfully removes an attribute" in new ProductFixture {
      val newProductAttributes =
        Map("title" → ContentAttribute(t = "string", v = JString("new name")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newProductAttributes), relations = None)

      val content =
        ContentManager.update(product.id, SimpleContext.id, updatePayload, "product").gimme
      content.attributes("title").v must === (JString("new name"))
      content.attributes.get("description") must === (None)
    }

    "successfully updates a parent when updating a related child" in new ProductFixture {
      val newSkuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKEW")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newSkuAttributes), relations = None)

      val content = ContentManager.update(sku.id, SimpleContext.id, updatePayload, "sku").gimme
      content.attributes("code").v must === (JString("TEST-SKEW"))

      val productContent = ContentManager.findLatest(product.id, SimpleContext.id, "product").gimme
      productContent.relations("sku") must === (Seq(content.commitId))
    }

    "successfully updates a parent but not child when updating an entity" in new VariantFixture {
      val newSkuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKEW")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newSkuAttributes), relations = None)

      val content = ContentManager.update(sku.id, SimpleContext.id, updatePayload, "sku").gimme
      content.attributes("code").v must === (JString("TEST-SKEW"))

      val productContent = ContentManager.findLatest(product.id, SimpleContext.id, "product").gimme
      productContent.relations("sku") must === (Seq(content.commitId))

      val skuContent = ContentManager.findLatest(sku.id, SimpleContext.id, "sku").gimme
      skuContent.commitId must === (content.commitId)
      skuContent.relations("variant") must === (Seq(variant.commitId))
    }

    "fails if the relation does not exist" in new VariantFixture {
      val newRelations  = Map("variant" → Seq(200))
      val updatePayload = UpdateContentPayload(attributes = None, relations = Some(newRelations))

      val failures =
        ContentManager.update(sku.id, SimpleContext.id, updatePayload, "sku").gimmeFailures
      failures.head must === (RelatedContentDoesNotExist("variant", 200))
    }

    "fails with 404 when invalid id" in new SkuFixture {
      val newSkuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKEW")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newSkuAttributes), relations = None)

      val failures =
        ContentManager.update(200, SimpleContext.id, updatePayload, sku.kind).gimmeFailures
      failures.head must === (ObjectNotFound(sku.kind, 200, SimpleContext.id))
    }

    "fails with 404 when invalid kind" in new SkuFixture {
      val newSkuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKEW")))
      val updatePayload =
        UpdateContentPayload(attributes = Some(newSkuAttributes), relations = None)

      val failures =
        ContentManager.update(sku.id, SimpleContext.id, updatePayload, "variant").gimmeFailures
      failures.head must === (ObjectNotFound("variant", sku.id, SimpleContext.id))
    }

    "fails when trying to create content with relations cycles" in new VariantFixture {
      val newVariantRels = Map("product" → Seq(product.commitId))
      val updatePayload  = UpdateContentPayload(attributes = None, relations = Some(newVariantRels))

      ContentManager.update(variant.id, SimpleContext.id, updatePayload, "variant").gimmeFailures
    }

  }

  "ContentManager.archive" - {
    "successfully with no relations" in new SkuFixture {
      ContentManager.archive(sku.id, SimpleContext.id, sku.kind).gimme
    }

    "successfully with relations" in new VariantFixture {
      ContentManager.archive(sku.id, SimpleContext.id, sku.kind).gimme
    }

    "content is not returned by parent after being archived" in new VariantFixture {
      ContentManager.archive(sku.id, SimpleContext.id, sku.kind).gimme
      val productContent =
        ContentManager.findLatest(product.id, SimpleContext.id, product.kind).gimme
      productContent.relations.get("sku") must === (None)
    }

    "content is accessible in previous commits after being archived" in new VariantFixture {
      ContentManager.archive(sku.id, SimpleContext.id, sku.kind).gimme
      ContentManager.findByCommit(sku.commitId, sku.kind).gimme
    }

    "content is still accessible by previous parent commit after being archived" in new VariantFixture {
      ContentManager.archive(sku.id, SimpleContext.id, sku.kind).gimme
      val productContent = ContentManager.findByCommit(product.commitId, product.kind).gimme
      productContent.relations.get("sku") must === (Some(Seq(sku.commitId)))
    }
  }

  trait Fixture {
    val attributes = Map(
      "title"       → ContentAttribute(t = "string", v = JString("a test product")),
      "description" → ContentAttribute(t = "richText", v = JString("<p>A test description</p>"))
    )

    val relations = Map.empty[String, Seq[Commit#Id]]
  }

  trait SkuFixture extends Fixture {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    val skuAttributes = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKU")))
    val skuRelations  = Map.empty[String, Seq[Commit#Id]]

    val skuPayload =
      CreateContentPayload(kind = "sku", attributes = skuAttributes, relations = skuRelations)

    val sku = ContentManager.create(SimpleContext.id, skuPayload).gimme
  }

  trait ProductFixture extends SkuFixture {
    val productRelations = Map("sku" → Seq(sku.commitId))

    val productPayload =
      CreateContentPayload(kind = "product", attributes = attributes, relations = productRelations)
    val product = ContentManager.create(SimpleContext.id, productPayload).gimme
  }

  trait VariantFixture {
    implicit val formats: Formats = JsonFormatters.phoenixFormats

    val variantRels  = Map.empty[String, Seq[Commit#Id]]
    val variantAttrs = Map("title" → ContentAttribute(t = "string", v = JString("color")))
    val variantPayload =
      CreateContentPayload(kind = "variant", attributes = variantAttrs, relations = variantRels)
    val variant = ContentManager.create(SimpleContext.id, variantPayload).gimme

    val skuRels    = Map("variant" → Seq(variant.commitId))
    val skuAttrs   = Map("code" → ContentAttribute(t = "string", v = JString("TEST-SKU")))
    val skuPayload = CreateContentPayload(kind = "sku", attributes = skuAttrs, relations = skuRels)
    val sku        = ContentManager.create(SimpleContext.id, skuPayload).gimme

    val productRels  = Map("sku"   → Seq(sku.commitId))
    val productAttrs = Map("title" → ContentAttribute(t = "string", v = JString("test product")))
    val productPayload =
      CreateContentPayload(kind = "product", attributes = productAttrs, relations = productRels)
    val product = ContentManager.create(SimpleContext.id, productPayload).gimme
  }
}
