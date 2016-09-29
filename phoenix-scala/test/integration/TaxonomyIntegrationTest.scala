import akka.http.scaladsl.model.StatusCodes

import Extensions._
import failures.TaxonomyFailures
import models.taxonomy.{Taxon, Taxonomies, Taxons}
import org.json4s.JsonDSL._
import org.json4s._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import util._
import util.fixtures.BakedFixtures

class TaxonomyIntegrationTest
    extends IntegrationTestBase
    with HttpSupport
    with AutomaticAuth
    with BakedFixtures
    with TaxonomySeeds {

  def findTermById(terms: TermList, id: Int): Option[TaxonResponse.Root] =
    terms
      .find(_.id == id)
      .orElse(
          terms.map(t ⇒ findTermById(t.children, id)).find(_.isDefined).flatten
      )

  def queryGetTaxon(formId: Int): TaxonomyResponse.Root = {
    val response = GET(s"v1/taxonomy/${ctx.name}/$formId")
    response.status must === (StatusCodes.OK)
    response.as[TaxonomyResponse.Root]
  }

  "GET v1/taxonomy/{contextName}/{taxonomyFormId}" - {

    "gets taxonomy" in new Taxonomy_Seed {
      val response = queryGetTaxon(taxonomy.formId)
      response.id must === (taxonomy.formId)
      response.terms mustBe empty
      response.attributes must === (JObject(taxonomyAttributes.toList: _*))
    }
  }

  "POST v1/taxonomy/{contextName}" - {
    "creates taxonomy" in {
      val payload: CreateTaxonomyPayload =
        CreateTaxonomyPayload(Map("name" → (("t" → "string") ~ ("v" → "name"))),
                              hierarchical = false)
      val resp = POST(s"v1/taxonomy/${ctx.name}", payload)
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonomyResponse.Root]
      queryGetTaxon(taxonResp.id) must === (taxonResp)
      taxonResp.attributes must === (JObject(payload.attributes.toList: _*))
      taxonResp.terms mustBe empty
    }
  }
  "PATCH v1/taxonomy/{contextName}/{taxonomyFormId}" - {
    "updates taxonomy" in new Taxonomy_Seed {
      val newAttributes                  = taxonomyAttributes + ("testValue" → (("t" → "string") ~ ("v" → "test")))
      val payload: UpdateTaxonomyPayload = UpdateTaxonomyPayload(newAttributes)
      val resp                           = PATCH(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}", payload)
      resp.status must === (StatusCodes.OK)
      val taxonomyResp = resp.as[TaxonomyResponse.Root]
      taxonomyResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  "DELETE v1/taxonomy/{contextName}/{taxonomyFormId}" - {
    "deletes taxonomy" in new Taxonomy_Seed {
      val resp = DELETE(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}")
      resp.status must === (StatusCodes.NoContent)
      val updatedTaxonomy = Taxonomies.mustFindByFormId404(taxonomy.formId).gimme
      updatedTaxonomy.archivedAt mustBe defined
    }
  }

  "GET v1/taxonomy/{contextName}/taxon/{taxonFormId}" - {
    "gets taxon" in new FlatTaxons_Baked {
      private val taxonToQuery: Taxon = taxons.head
      val resp                        = GET(s"v1/taxonomy/${ctx.name}/taxon/${taxonToQuery.formId}")
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonResponse.Root]

      taxonResp.id must === (taxonToQuery.formId)
      taxonResp.attributes must === (JObject(taxonAttributes.head.toList: _*))
    }
  }

  "POST v1/taxonomy/{contextName}/{taxonomyFormId}" - {
    "creates taxon" in new Taxonomy_Seed {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}",
                      CreateTaxonPayload(attributes = attributes, parent = None, sibling = None))

      resp.status must === (StatusCodes.OK)
      val createdTaxon = resp.as[TaxonResponse.Root]
      createdTaxon.attributes must === (JObject(attributes.toList: _*))

      val taxons = queryGetTaxon(taxonomy.formId).terms
      taxons.size must === (1)
      taxons.head.id must === (createdTaxon.id)
      taxons.head.attributes must === (createdTaxon.attributes)
    }

    "creates taxon at position" in new FlatTaxons_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}",
                      CreateTaxonPayload(attributes = attributes,
                                         parent = None,
                                         sibling = Some(taxons.head.formId)))

      resp.status must === (StatusCodes.OK)
      val createdTaxon = resp.as[TaxonResponse.Root]

      val newTaxons = queryGetTaxon(taxonomy.formId).terms
      newTaxons.map(_.id) must contain theSameElementsInOrderAs Seq(taxons.head.formId,
                                                                    createdTaxon.id,
                                                                    taxons(1).formId)
    }

    "creates child taxon" in new HierarchyTaxons_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling  = links.filter(_.parentIndex.isDefined).head
      val parent   = links.filter(_.index == sibling.parentIndex.get).head
      val children = links.filter(link ⇒ link.parentIndex.contains(parent.index))

      val siblingFormId = Taxons.mustFindById404(sibling.taxonId).gimme.formId
      val parentFormId  = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp =
        POST(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}",
             CreateTaxonPayload(attributes = attributes, parent = Some(parentFormId), None))

      resp.status must === (StatusCodes.OK)
      val createdTerm = resp.as[TaxonResponse.Root]

      val newTaxons      = queryGetTaxon(taxonomy.formId).terms
      val responseParent = findTermById(newTaxons, parentFormId).get
      responseParent.children.size must === (children.size + 1)
      responseParent.children.map(_.id) must contain(createdTerm.id)
    }

    "fails if both parent and sibling are specified" in new HierarchyTaxons_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling = links.filter(_.parentIndex.isDefined).head
      val parent  = links.filter(_.index == sibling.parentIndex.get).head

      val siblingFormId = Taxons.mustFindById404(sibling.taxonId).gimme.formId
      val parentFormId  = Taxons.mustFindById404(parent.taxonId).gimme.formId

      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxonomy.formId}",
                      CreateTaxonPayload(attributes = attributes,
                                         parent = Some(parentFormId),
                                         Some(siblingFormId)))

      resp.status must === (StatusCodes.BadRequest)
    }
  }

  "PATCH v1/taxonomy/{contextName}/taxon/{termFormId}" - {
    "moves taxons between subtrees" in new HierarchyTaxons_Baked {
      val taxonsBefore = queryGetTaxon(taxonomy.formId).terms

      val List(left, right) = taxonsBefore.take(2)
      val taxonToMoveId     = left.children.head.id
      val newParentId       = right.id

      val resp = PATCH(s"v1/taxonomy/${ctx.name}/taxon/$taxonToMoveId",
                       UpdateTaxonPayload(None, Some(newParentId), None))
      resp.status must === (StatusCodes.OK)

      val taxonsAfter = queryGetTaxon(taxonomy.formId).terms

      val List(leftAfter, rightAfter) = taxonsAfter.take(2)
      leftAfter.children.size must === (left.children.size - 1)
      rightAfter.children.size must === (right.children.size + 1)
      rightAfter.children.map(_.id) must contain(taxonToMoveId)
    }

    "fails to move taxon to children" in new HierarchyTaxons_Baked {
      val taxonsBefore = queryGetTaxon(taxonomy.formId).terms

      val List(left, right) = taxonsBefore.take(2)

      val newParentId   = left.children.head.id
      val taxonToMoveId = left.id

      val resp = PATCH(s"v1/taxonomy/${ctx.name}/taxon/$taxonToMoveId",
                       UpdateTaxonPayload(None, Some(newParentId), None))
      resp.status must === (StatusCodes.BadRequest)
    }
  }

  "DELETE v1/taxonomy/{contextName}/taxon/{taxonFormId}" - {
    "deletes taxon" in new FlatTaxons_Baked {
      val resp = DELETE(s"v1/taxonomy/${ctx.name}/taxon/${taxons.head.formId}")
      resp.status must === (StatusCodes.NoContent)

      val taxon = Taxons.mustFindByFormId404(taxons.head.formId).gimme
      taxon.archivedAt mustBe defined
    }

    "rejects to delete taxon if it has children" in new HierarchyTaxons_Baked {
      val parent         = links.filter(_.parentIndex.isDefined).head
      val taxonToArchive = Taxons.mustFindById404(parent.taxonId).gimme

      val resp = DELETE(s"v1/taxonomy/${ctx.name}/taxon/${taxonToArchive.formId}")
      resp.status must === (StatusCodes.BadRequest)
      resp.error must === (
          TaxonomyFailures.CannotArchiveParentTaxon(taxonToArchive.formId).description)
    }
  }

}
