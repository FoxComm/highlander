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
import utils.aliases.Json
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
          taxons.map(t ⇒ findTaxonsById(t.childrenAsList.toList, id)).find(_.isDefined).flatten
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

//    "gets full hierarchy" in new HierarchyTaxonsFixture {
//      val response = queryGetTaxonomy(taxonomy.formId)
//
//      response.id must === (taxonomy.formId)
//
//      response.taxons.map((_.taxon.attributes \ "name" \ "v").extract[String]) must === (
//          Seq("taxon1", "taxon2"))
//      response.taxons.head.childrenAsList
//        .map((_.taxon.attributes \ "name" \ "v").extract[String]) must === (
//          Seq("taxon3", "taxon4"))
//      response.taxons.head.childrenAsList.head.childrenAsList
//        .map((_.taxon.attributes \ "name" \ "v").extract[String]) must === (Seq("taxon7"))
//    }
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
      val taxonToQueryName                 = taxonsNames.head

      val response = taxonsApi(taxonToQuery.formId).get.as[TaxonResponse]

      response.id must === (taxonToQuery.formId)
      (response.attributes \ "name" \ "v").extract[String] must === (taxonToQueryName)
    }
  }

  "POST v1/taxonomies/:contextName/:taxonomyFormId/taxons" - {
    "creates taxon" in new FlatTaxonsFixture {
      private val taxonName: String = "name"

      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(CreateTaxonPayload(taxonAttributes, None))
        .as[TaxonResponse]
      (response.attributes \ "name" \ "v").extract[String] must === (taxonName)

      val taxonsResp = queryGetTaxonomy(taxonomy.formId).taxons
      taxonsResp.size must === (1)
      taxonsResp.head.taxon.id must === (response.id)
      (taxonsResp.head.taxon.attributes \ "name" \ "v").extract[String] must === (
          (response.attributes \ "name" \ "v").extract[String])
    }

    "creates taxon at position" in new FlatTaxonsFixture {
      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(
            CreateTaxonPayload(taxonAttributes,
                               location = TaxonLocation(parent = None, position = 1.some).some))
        .as[TaxonResponse]

      val newTaxons = queryGetTaxonomy(taxonomy.formId).taxons

      newTaxons.map(_.taxon.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                          response.id,
                                                                          taxons(1).formId)
    }

    "creates taxon at last position" in new FlatTaxonsFixture {
      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(
            CreateTaxonPayload(taxonAttributes,
                               location = TaxonLocation(parent = None, position = None).some))
        .as[TaxonResponse]

      val newTaxons = queryGetTaxonomy(taxonomy.formId).taxons

      newTaxons.map(_.taxon.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                          taxons(1).formId,
                                                                          response.id)
    }

    "creates child taxon" in new HierarchyTaxonsFixture {
      val sibling  = links.filter(_.parentIndex.isDefined).head
      val parent   = links.filter(_.index == sibling.parentIndex.get).head
      val children = links.filter(link ⇒ link.parentIndex.contains(parent.index))

      val siblingFormId = Taxons.mustFindById404(sibling.taxonId).gimme.formId
      val parentFormId  = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val response = taxonomiesApi(taxonomy.formId)
        .createTaxon(
            CreateTaxonPayload(taxonAttributes, TaxonLocation(parentFormId.some, Some(0)).some))
        .as[TaxonResponse]

      val newTaxons      = queryGetTaxonomy(taxonomy.formId).taxons
      val responseParent = findTaxonsById(newTaxons, parentFormId).get
      responseParent.childrenAsList.size must === (children.size + 1)
      responseParent.childrenAsList.map(_.taxon.id) must contain(response.id)
    }

    "fails if position is invalid" in new HierarchyTaxonsFixture {
      val sibling = links.filter(_.parentIndex.isDefined).head
      val parent  = links.filter(_.index == sibling.parentIndex.get).head

      val parentFormId = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp = taxonomiesApi(taxonomy.formId).createTaxon(
          CreateTaxonPayload(taxonAttributes,
                             Some(TaxonLocation(Some(parentFormId), Some(Integer.MAX_VALUE)))))

      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (NoTaxonAtPosition(parentFormId.some, Integer.MAX_VALUE).description)
    }
  }

  "PATCH v1/taxonomies/:contextName/taxon/:termFormId" - {
    "moves taxons between subtrees" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxonomy(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)
      val taxonToMoveId     = left.childrenAsList.head.taxon.id
      val newParentId       = right.taxon.id

      val resp = taxonsApi(taxonToMoveId).update(
          UpdateTaxonPayload(right.taxon.attributes.asInstanceOf[Map[String, Json]],
                             TaxonLocation(newParentId.some, Some(0)).some))
      resp.status must === (StatusCodes.OK)

      val taxonsAfter = queryGetTaxonomy(taxonomy.formId).taxons

      val List(leftAfter, rightAfter) = taxonsAfter.take(2)
      leftAfter.childrenAsList.size must === (left.childrenAsList.size - 1)
      rightAfter.childrenAsList.size must === (right.childrenAsList.size + 1)
      rightAfter.childrenAsList.map(_.taxon.id) must contain(taxonToMoveId)
    }

    "fails to move taxon to children" in new HierarchyTaxonsFixture {
      val taxonsBefore = queryGetTaxonomy(taxonomy.formId).taxons

      val List(left, right) = taxonsBefore.take(2)

      val newParentId   = left.childrenAsList.head.taxon.id
      val taxonToMoveId = left.taxon.id

      val resp = taxonsApi(taxonToMoveId).update(
          UpdateTaxonPayload(left.taxon.attributes.asInstanceOf[Map[String, Json]],
                             TaxonLocation(newParentId.some, Some(0)).some))
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
