import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import failures.TaxonomyFailures._
import models.objects.ObjectForm
import models.taxonomy._
import org.json4s.JsonDSL._
import org.json4s._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import utils.db.ExPostgresDriver.api._
import slick.jdbc.GetResult
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures

class TaxonomyIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with TaxonomySeeds
    with PhoenixAdminApi {

  def findTermById(terms: TaxonList, id: Int): Option[TaxonResponse] =
    terms
      .find(_.id == id)
      .orElse(
          terms.map(t ⇒ findTermById(t.children, id)).find(_.isDefined).flatten
      )

  def queryGetTaxon(formId: Int): TaxonomyResponse =
    taxonomyApi(formId).get.as[TaxonomyResponse]

  "GET v1/taxonomy/:contextName/:taxonomyFormId" - {
    "gets taxonomy" in new TaxonomyFixture {
      val response = queryGetTaxon(taxonomy.formId)
      response.id must === (taxonomy.formId)
      response.taxons mustBe empty
      response.attributes must === (JObject(taxonomyAttributes.toList: _*))
    }
  }

  "POST v1/taxonomy/:contextName" - {
    "creates taxonomy" in {
      val payload = CreateTaxonomyPayload(Map("name" → (("t" → "string") ~ ("v" → "name"))),
                                          hierarchical = false)
      val resp = taxonomyApi.create(payload)
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonomyResponse]
      queryGetTaxon(taxonResp.id) must === (taxonResp)
      taxonResp.attributes must === (JObject(payload.attributes.toList: _*))
      taxonResp.taxons mustBe empty
    }
  }

  "PATCH v1/taxonomy/:contextName/:taxonomyFormId" - {
    "updates taxonomy" in new TaxonomyFixture {
      val newAttributes = taxonomyAttributes + ("testValue" → (("t" → "string") ~ ("v" → "test")))
      val payload       = UpdateTaxonomyPayload(newAttributes)
      val resp          = taxonomyApi(taxonomy.formId).update(payload)
      resp.status must === (StatusCodes.OK)

      val taxonomyResp = resp.as[TaxonomyResponse]
      taxonomyResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  "DELETE v1/taxonomy/:contextName/:taxonomyFormId" - {
    "deletes taxonomy" in new TaxonomyFixture {
      val resp = taxonomyApi(taxonomy.formId).delete
      resp.status must === (StatusCodes.NoContent)
      val updatedTaxonomy = Taxonomies.mustFindByFormId404(taxonomy.formId).gimme
      updatedTaxonomy.archivedAt mustBe defined
    }

    "deletes taxonomy with all related taxons" in new HierarchyTaxonsFixture {
      val resp = taxonomyApi(taxonomy.formId).delete
      resp.status must === (StatusCodes.NoContent)

      val allLinks: Seq[TaxonomyTaxonLink] =
        TaxonomyTaxonLinks.filter(_.id.inSet(links.map(_.id))).result.gimme
      allLinks.map(_.archivedAt).filter(_.isEmpty) mustBe empty
      val allTaxons: Seq[Taxon] = Taxons.filter(_.id.inSet(taxons.map(_.id))).result.gimme
      allTaxons.map(_.archivedAt).filter(_.isEmpty) mustBe empty
    }
  }

  "GET v1/taxonomy/:contextName/taxon/:taxonFormId" - {
    "gets taxon" in new FlatTaxonsFixture {
      private val taxonToQuery: Taxon = taxons.head
      val resp                        = taxonApi(taxonToQuery.formId).get
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonResponse]

      taxonResp.id must === (taxonToQuery.formId)
      taxonResp.attributes must === (JObject(taxonAttributes.head.toList: _*))
    }
  }

  "POST v1/taxonomy/:contextName/:taxonomyFormId" - {
    "creates taxon" in new TaxonomyFixture {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp       = taxonomyApi(taxonomy.formId).createTaxon(CreateTaxonPayload(attributes, None))

      resp.status must === (StatusCodes.OK)
      val createdTaxon = resp.as[TaxonResponse]
      createdTaxon.attributes must === (JObject(attributes.toList: _*))

      val taxons = queryGetTaxon(taxonomy.formId).taxons
      taxons.size must === (1)
      taxons.head.id must === (createdTaxon.id)
      taxons.head.attributes must === (createdTaxon.attributes)
    }

    "creates taxon at position" in new FlatTaxonsFixture {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp = taxonomyApi(taxonomy.formId).createTaxon(
          CreateTaxonPayload(attributes = attributes,
                             location = TaxonLocation(parent = None, position = 1).some))

      resp.status must === (StatusCodes.OK)
      val createdTaxon = resp.as[TaxonResponse]

      val newTaxons = queryGetTaxon(taxonomy.formId).taxons
      newTaxons.map(_.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                    createdTaxon.id,
                                                                    taxons(1).formId)
    }

    "creates child taxon" in new HierarchyTaxonsFixture {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling  = links.filter(_.parentIndex.isDefined).head
      val parent   = links.filter(_.index == sibling.parentIndex.get).head
      val children = links.filter(link ⇒ link.parentIndex.contains(parent.index))

      val siblingFormId = Taxons.mustFindById404(sibling.taxonId).gimme.formId
      val parentFormId  = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp = taxonomyApi(taxonomy.formId)
        .createTaxon(CreateTaxonPayload(attributes, TaxonLocation(parentFormId.some, 0).some))

      resp.status must === (StatusCodes.OK)
      val createdTerm = resp.as[TaxonResponse]

      val newTaxons      = queryGetTaxon(taxonomy.formId).taxons
      val responseParent = findTermById(newTaxons, parentFormId).get
      responseParent.children.size must === (children.size + 1)
      responseParent.children.map(_.id) must contain(createdTerm.id)
    }

    "fails if position is invalid" in new HierarchyTaxonsFixture {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling = links.filter(_.parentIndex.isDefined).head
      val parent  = links.filter(_.index == sibling.parentIndex.get).head

      val parentFormId = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp = taxonomyApi(taxonomy.formId).createTaxon(
          CreateTaxonPayload(attributes,
                             Some(TaxonLocation(Some(parentFormId), Integer.MAX_VALUE))))

      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (NoTaxonAtPosition(parentFormId.some, Integer.MAX_VALUE).description)
    }
  }

  "PATCH v1/taxonomy/:contextName/taxon/:termFormId" - {
    "moves taxons between subtrees" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxon(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)
      val taxonToMoveId     = left.children.head.id
      val newParentId       = right.id

      val resp = taxonApi(taxonToMoveId).update(
          UpdateTaxonPayload(None, TaxonLocation(newParentId.some, 0).some))
      resp.status must === (StatusCodes.OK)

      val taxonsAfter = queryGetTaxon(taxonomy.formId).taxons

      val List(leftAfter, rightAfter) = taxonsAfter.take(2)
      leftAfter.children.size must === (left.children.size - 1)
      rightAfter.children.size must === (right.children.size + 1)
      rightAfter.children.map(_.id) must contain(taxonToMoveId)
    }

    "fails to move taxon to children" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxon(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)

      val newParentId   = left.children.head.id
      val taxonToMoveId = left.id

      val resp = taxonApi(taxonToMoveId).update(
          UpdateTaxonPayload(None, TaxonLocation(newParentId.some, 0).some))
      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (CannotMoveParentTaxonUnderChild.description)
    }
  }

  "DELETE v1/taxonomy/:contextName/taxon/:taxonFormId" - {
    "deletes taxon" in new FlatTaxonsFixture {
      val resp = taxonApi(taxons.head.formId).delete
      resp.status must === (StatusCodes.NoContent)

      val taxon = Taxons.mustFindByFormId404(taxons.head.formId).gimme
      taxon.archivedAt mustBe defined
    }

    "rejects to delete taxon if it has children" in new HierarchyTaxonsFixture {
      val parent         = links.filter(_.parentIndex.isDefined).head
      val taxonToArchive = Taxons.mustFindById404(parent.taxonId).gimme

      val resp = taxonApi(taxonToArchive.formId).delete
      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (CannotArchiveParentTaxon(taxonToArchive.formId).description)
    }
  }

  "Taxonomy_search_view" - {
    case class TaxonomiesSearchViewItem(id: Int,
                                        taxonomyId: Int,
                                        name: String,
                                        context: String,
                                        `type`: String,
                                        valuesCount: Int,
                                        activeFrom: Option[String],
                                        activeTo: Option[String],
                                        archivedAt: Option[String])
    implicit val getTaxonomiesSearchViewResult = GetResult(
        r ⇒
          TaxonomiesSearchViewItem(r.nextInt,
                                   r.nextInt,
                                   r.nextString,
                                   r.nextString,
                                   r.nextString,
                                   r.nextInt,
                                   r.nextStringOption(),
                                   r.nextStringOption(),
                                   r.nextStringOption()))

    def selectByTaxonId(taxonomy_id: Int) =
      sql"""select * from taxonomies_search_view where taxonomy_id = ${taxonomy_id}"""
        .as[TaxonomiesSearchViewItem]
        .gimme

    "should insert data on taxonomy creation" in new HierarchyTaxonsFixture {
      val taxonomies = selectByTaxonId(taxonomy.id)

      taxonomies.size must === (1)
      val item = taxonomies.head
      item must === (
          item.copy(taxonomyId = taxonomy.id,
                    context = ctx.name,
                    `type` = "hierarchical",
                    valuesCount = taxons.size,
                    archivedAt = None))
    }

    "should update data on taxon removal" in new HierarchyTaxonsFixture {
      TaxonomyTaxonLinks.update(links.head, links.head.copy(archivedAt = Some(Instant.now))).gimme

      val taxonomies = selectByTaxonId(taxonomy.id)

      taxonomies.size must === (1)
      val item = taxonomies.head
      item must === (
          item.copy(taxonomyId = taxonomy.id,
                    context = ctx.name,
                    `type` = "hierarchical",
                    valuesCount = taxons.size - 1,
                    archivedAt = None))
    }

    "should update taxonomy name" in new HierarchyTaxonsFixture {
      val newName       = "new name"
      val newAttributes = taxonomyAttributes + ("name" → (("t" → "string") ~ ("v" → newName)))

      val resp = taxonomyApi(taxonomy.formId).update(UpdateTaxonomyPayload(newAttributes))
      resp.status must === (StatusCodes.OK)

      val taxonomies = selectByTaxonId(taxonomy.id)

      taxonomies.size must === (1)
      val item = taxonomies.head
      item.name must === (newName)
    }
  }

  "Taxon is assigned to a product" in new ProductAndSkus_Baked with FlatTaxonsFixture {
    private val taxonToBeAssigned: ObjectForm#Id = taxons.head.formId
    taxonApi(taxonToBeAssigned).assignProduct(simpleProduct.formId).mustBeOk

    val productTaxons = productsApi(simpleProduct.formId).taxons.get.as[TaxonList]
    productTaxons.map(_.id) must contain only taxonToBeAssigned
  }

  "Taxon is unassigned to a product" in new ProductAndSkus_Baked with FlatTaxonsFixture {
    private val taxonToBeAssigned: ObjectForm#Id = taxons.head.formId
    taxonApi(taxonToBeAssigned).assignProduct(simpleProduct.formId).mustBeOk()
    taxonApi(taxonToBeAssigned).unassignProduct(simpleProduct.formId).mustBeOk()

    val productTaxons = productsApi(simpleProduct.formId).taxons.get.as[TaxonList]
    productTaxons.map(_.id) mustBe empty
  }

  trait FlatTaxonsFixture extends StoreAdmin_Seed with FlatTaxons_Baked {
    def au = storeAdminAuthData
  }

  trait HierarchyTaxonsFixture extends StoreAdmin_Seed with HierarchyTaxons_Baked {
    def au = storeAdminAuthData
  }

  trait TaxonomyFixture extends StoreAdmin_Seed with Taxonomy_Raw {
    def au = storeAdminAuthData
  }
}
