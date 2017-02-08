import java.time.Instant

import akka.http.scaladsl.model.StatusCodes

import cats.implicits._
import failures.TaxonomyFailures._
import models.objects.ObjectForm
import models.taxonomy.{Taxon ⇒ ModelTaxon, _}
import org.json4s.JsonDSL._
import org.json4s._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import slick.jdbc.GetResult
import testutils._
import testutils.apis.PhoenixAdminApi
import testutils.fixtures.BakedFixtures
import utils.db.ExPostgresDriver.api._

class TaxonomyIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with TaxonomySeeds
    with PhoenixAdminApi {

  def findTaxonsById(taxons: Seq[TaxonTreeResponse], id: Int): Option[TaxonTreeResponse] =
    taxons
      .find(_.taxon.id == id)
      .orElse(
          taxons.map(t ⇒ findTaxonsById(t.children.toList, id)).find(_.isDefined).flatten
      )

  def queryGetTaxonomy(formId: Int): FullTaxonomyResponse =
    taxonomiesApi(formId).get.as[FullTaxonomyResponse]

  "GET v1/taxonomies/:contextName/:taxonomyFormId" - {
    "gets taxonomy" in new TaxonomyFixture {
      val response = queryGetTaxonomy(taxonomy.formId)
      response.id must === (taxonomy.formId)
      response.taxons mustBe empty
      response.attributes must === (JObject(taxonomyAttributes.toList: _*))
    }

    "gets full hierarchy" in new HierarchyTaxonsFixture {
      val response = queryGetTaxonomy(taxonomy.formId)

      response.id must === (taxonomy.formId)

      response.taxons.map(_.taxon.name) must === (Seq("taxon1", "taxon2"))
      response.taxons.head.children.map(_.taxon.name) must === (Seq("taxon3", "taxon4"))
      response.taxons.head.children.head.children.map(_.taxon.name) must === (Seq("taxon7"))
    }
  }

  "POST v1/taxonomies/:contextName" - {
    "creates taxonomy" in {
      val payload = CreateTaxonomyPayload(Map("name" → (("t" → "string") ~ ("v" → "name"))),
                                          hierarchical = false,
                                          scope = None)
      val taxonResp = taxonomiesApi.create(payload).as[FullTaxonomyResponse]
      queryGetTaxonomy(taxonResp.id) must === (taxonResp)
      taxonResp.attributes must === (JObject(payload.attributes.toList: _*))
      taxonResp.taxons mustBe empty
    }
  }

  "PATCH v1/taxonomies/:contextName/:taxonomyFormId" - {
    "updates taxonomy" in new TaxonomyFixture {
      val newAttributes = taxonomyAttributes + ("testValue" → (("t" → "string") ~ ("v" → "test")))
      val payload       = UpdateTaxonomyPayload(newAttributes)
      val taxonomyResp  = taxonomiesApi(taxonomy.formId).update(payload).as[FullTaxonomyResponse]
      taxonomyResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  "DELETE v1/taxonomies/:contextName/:taxonomyFormId" - {
    "deletes taxonomy" in new TaxonomyFixture {
      val resp = taxonomiesApi(taxonomy.formId).delete
      resp.status must === (StatusCodes.NoContent)
      val updatedTaxonomy = Taxonomies.mustFindByFormId404(taxonomy.formId).gimme
      updatedTaxonomy.archivedAt mustBe defined
    }

    "deletes taxonomy with all related taxons" in new HierarchyTaxonsFixture {
      val resp = taxonomiesApi(taxonomy.formId).delete
      resp.status must === (StatusCodes.NoContent)

      val allLinks: Seq[TaxonomyTaxonLink] =
        TaxonomyTaxonLinks.filter(_.id.inSet(links.map(_.id))).result.gimme
      allLinks.map(_.archivedAt).filter(_.isEmpty) mustBe empty
      val allTaxons: Seq[ModelTaxon] = Taxons.filter(_.id.inSet(taxons.map(_.id))).result.gimme
      allTaxons.map(_.archivedAt).filter(_.isEmpty) mustBe empty
    }
  }

  "GET v1/taxons/:contextName/:taxonFormId" - {
    "gets taxon" in new FlatTaxonsFixture {
      private val taxonToQuery: ModelTaxon = taxons.head
      val taxonToQueryName                 = taxonNames.head

      val response      = taxonsApi(taxonToQuery.formId).get.as[SingleTaxonResponse]
      val responseTaxon = response.taxon

      responseTaxon.id must === (taxonToQuery.formId)
      responseTaxon.name must === (taxonToQueryName)
    }
  }

  "POST v1/taxonomies/:contextName/:taxonomyFormId" - {
    "creates taxon" in new TaxonomyFixture {
      private val taxonName: String = "name"
      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(CreateTaxonPayload(taxonName, None))
        .as[SingleTaxonResponse]
      val createdTaxon = response.taxon
      createdTaxon.name must === (taxonName)

      val taxons = queryGetTaxonomy(taxonomy.formId).taxons
      taxons.size must === (1)
      taxons.head.taxon.id must === (createdTaxon.id)
      taxons.head.taxon.name must === (createdTaxon.name)
    }

    "creates taxon at position" in new FlatTaxonsFixture {
      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(
            CreateTaxonPayload("name",
                               location = TaxonLocation(parent = None, position = 1.some).some))
        .as[SingleTaxonResponse]

      val newTaxons = queryGetTaxonomy(taxonomy.formId).taxons

      newTaxons.map(_.taxon.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                          response.taxon.id,
                                                                          taxons(1).formId)
    }

    "creates taxon at last position" in new FlatTaxonsFixture {
      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(
            CreateTaxonPayload("name",
                               location = TaxonLocation(parent = None, position = None).some))
        .as[SingleTaxonResponse]

      val newTaxons = queryGetTaxonomy(taxonomy.formId).taxons

      newTaxons.map(_.taxon.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                          taxons(1).formId,
                                                                          response.taxon.id)
    }

    "creates child taxon" in new HierarchyTaxonsFixture {
      val sibling  = links.filter(_.parentIndex.isDefined).head
      val parent   = links.filter(_.index == sibling.parentIndex.get).head
      val children = links.filter(link ⇒ link.parentIndex.contains(parent.index))

      val siblingFormId = Taxons.mustFindById404(sibling.taxonId).gimme.formId
      val parentFormId  = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(CreateTaxonPayload("name", TaxonLocation(parentFormId.some, Some(0)).some))
        .as[SingleTaxonResponse]
      val createdTaxon = response.taxon

      val newTaxons      = queryGetTaxonomy(taxonomy.formId).taxons
      val responseParent = findTaxonsById(newTaxons, parentFormId).get
      responseParent.children.size must === (children.size + 1)
      responseParent.children.map(_.taxon.id) must contain(createdTaxon.id)
    }

    "fails if position is invalid" in new HierarchyTaxonsFixture {
      val sibling = links.filter(_.parentIndex.isDefined).head
      val parent  = links.filter(_.index == sibling.parentIndex.get).head

      val parentFormId = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp = taxonomiesApi(taxonomy.formId).createTaxon(
          CreateTaxonPayload("name",
                             Some(TaxonLocation(Some(parentFormId), Some(Integer.MAX_VALUE)))))

      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (NoTaxonAtPosition(parentFormId.some, Integer.MAX_VALUE).description)
    }
  }

  "PATCH v1/taxonomies/:contextName/taxon/:termFormId" - {
    "moves taxons between subtrees" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxonomy(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)
      val taxonToMoveId     = left.children.head.taxon.id
      val newParentId       = right.taxon.id

      val resp = taxonsApi(taxonToMoveId).update(
          UpdateTaxonPayload(None, TaxonLocation(newParentId.some, Some(0)).some))
      resp.status must === (StatusCodes.OK)

      val taxonsAfter = queryGetTaxonomy(taxonomy.formId).taxons

      val List(leftAfter, rightAfter) = taxonsAfter.take(2)
      leftAfter.children.size must === (left.children.size - 1)
      rightAfter.children.size must === (right.children.size + 1)
      rightAfter.children.map(_.taxon.id) must contain(taxonToMoveId)
    }

    "fails to move taxon to children" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxonomy(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)

      val newParentId   = left.children.head.taxon.id
      val taxonToMoveId = left.taxon.id

      val resp = taxonsApi(taxonToMoveId).update(
          UpdateTaxonPayload(None, TaxonLocation(newParentId.some, Some(0)).some))
      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (CannotMoveParentTaxonUnderChild.description)
    }
  }

  "DELETE v1/taxonomies/:contextName/taxon/:taxonFormId" - {
    "deletes taxon" in new FlatTaxonsFixture {
      val resp = taxonsApi(taxons.head.formId).delete
      resp.status must === (StatusCodes.NoContent)

      val taxon = Taxons.mustFindByFormId404(taxons.head.formId).gimme
      taxon.archivedAt mustBe defined
    }

    "rejects to delete taxon if it has children" in new HierarchyTaxonsFixture {
      val parent         = links.filter(_.parentIndex.isDefined).head
      val taxonToArchive = Taxons.mustFindById404(parent.taxonId).gimme

      val resp = taxonsApi(taxonToArchive.formId).delete
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

      val resp = taxonomiesApi(taxonomy.formId).update(UpdateTaxonomyPayload(newAttributes))
      resp.status must === (StatusCodes.OK)

      val taxonomies = selectByTaxonId(taxonomy.id)

      taxonomies.size must === (1)
      val item = taxonomies.head
      item.name must === (newName)
    }
  }

  "Taxon is assigned to a product" in new ProductAndSkus_Baked with FlatTaxonsFixture {
    private val taxonToBeAssigned: ObjectForm#Id = taxons.head.formId
    taxonsApi(taxonToBeAssigned).assignProduct(simpleProduct.formId).mustBeOk

    val productTaxons =
      productsApi(simpleProduct.formId).taxons.get.as[Seq[AssignedTaxonsResponse]]
    productTaxons.flatMap(_.taxons.map(_.id)) must contain only taxonToBeAssigned
  }

  "Taxon is unassigned to a product" in new ProductAndSkus_Baked with FlatTaxonsFixture {
    private val taxonToBeAssigned: ObjectForm#Id = taxons.head.formId
    taxonsApi(taxonToBeAssigned).assignProduct(simpleProduct.formId).mustBeOk()
    taxonsApi(taxonToBeAssigned).unassignProduct(simpleProduct.formId).mustBeOk()

    val productTaxons =
      productsApi(simpleProduct.formId).taxons.get.as[Seq[AssignedTaxonsResponse]]
    productTaxons.flatMap(_.taxons.map(_.id)) mustBe empty
  }

  trait FlatTaxonsFixture extends StoreAdmin_Seed with FlatTaxons_Baked

  trait HierarchyTaxonsFixture extends StoreAdmin_Seed with HierarchyTaxons_Baked

  trait TaxonomyFixture extends StoreAdmin_Seed with Taxonomy_Raw
}
