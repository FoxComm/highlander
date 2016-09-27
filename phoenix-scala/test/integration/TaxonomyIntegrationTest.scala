import akka.http.scaladsl.model.StatusCodes

import Extensions._
import models.taxonomy.Terms
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

  def findTermById(terms: TermList, id: Int): Option[TermResponse.Root] =
    terms
      .find(_.id == id)
      .orElse(
          terms.map(t ⇒ findTermById(t.children, id)).find(_.isDefined).flatten
      )

  def queryGetTaxon(formId: Int): TaxonResponse.Root = {
    val response = GET(s"v1/taxonomy/${ctx.name}/$formId")
    response.status must === (StatusCodes.OK)
    response.as[TaxonResponse.Root]
  }

  "GET v1/taxonomy/{contextName}/{taxonomyFormId}" - {

    "gets taxon" in new Taxon_Seed {
      val response = queryGetTaxon(taxon.formId)
      response.id must === (taxon.formId)
      response.terms mustBe empty
      response.attributes must === (JObject(taxonAttributes.toList: _*))
    }
  }

  "POST v1/taxonomy/{contextName}" - {
    "creates taxon" in {
      val payload: CreateTaxonPayload =
        CreateTaxonPayload(Map("name" → (("t" → "string") ~ ("v" → "name"))), hierarchical = false)
      val resp = POST(s"v1/taxonomy/${ctx.name}", payload)
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonResponse.Root]
      queryGetTaxon(taxonResp.id) must === (taxonResp)
      taxonResp.attributes must === (JObject(payload.attributes.toList: _*))
      taxonResp.terms mustBe empty
    }
  }
  "PATCH v1/taxonomy/{contextName}/{taxonomyFormId}" - {
    "updates taxon" in new Taxon_Seed {
      val newAttributes               = taxonAttributes + ("testValue" → (("t" → "string") ~ ("v" → "test")))
      val payload: UpdateTaxonPayload = UpdateTaxonPayload(newAttributes)
      val resp                        = PATCH(s"v1/taxonomy/${ctx.name}/${taxon.formId}", payload)
      resp.status must === (StatusCodes.OK)
      val taxonResp = resp.as[TaxonResponse.Root]
      taxonResp.attributes must === (JObject(payload.attributes.toList: _*))
    }
  }

  //TODO: "DELETE v1/taxonomy/{contextName}/{taxonomyFormId}" - {}

  "GET v1/taxonomy/{contextName}/term/{termFormId}" - {}

  "POST v1/taxonomy/{contextName}/{taxonomyFormId}" - {
    "creates term" in new Taxon_Seed {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxon.formId}",
                      CreateTermPayload(attributes = attributes, parent = None, sibling = None))

      resp.status must === (StatusCodes.OK)
      val createdTerm = resp.as[TermResponse.Root]
      createdTerm.attributes must === (JObject(attributes.toList: _*))

      val taxonTerms = queryGetTaxon(taxon.formId).terms
      taxonTerms.size must === (1)
      taxonTerms.head.id must === (createdTerm.id)
      taxonTerms.head.attributes must === (createdTerm.attributes)
    }

    "creates term at position" in new FlatTerms_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))
      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxon.formId}",
                      CreateTermPayload(attributes = attributes,
                                        parent = None,
                                        sibling = Some(terms.head.formId)))

      resp.status must === (StatusCodes.OK)
      val createdTerm = resp.as[TermResponse.Root]

      val taxonTerms = queryGetTaxon(taxon.formId).terms
      taxonTerms.map(_.id) must contain theSameElementsInOrderAs Seq(terms.head.formId,
                                                                     createdTerm.id,
                                                                     terms(1).formId)
    }

    "creates child term" in new HierarchyTerms_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling  = links.filter(_.parentIndex.isDefined).head
      val parent   = links.filter(_.index == sibling.parentIndex.get).head
      val children = links.filter(link ⇒ link.parentIndex.contains(parent.index))

      val siblingFormId = Terms.mustFindById404(sibling.taxonTermId).gimme.formId
      val parentFormId  = Terms.mustFindById404(parent.taxonTermId).gimme.formId

      val resp =
        POST(s"v1/taxonomy/${ctx.name}/${taxon.formId}",
             CreateTermPayload(attributes = attributes, parent = Some(parentFormId), None))

      resp.status must === (StatusCodes.OK)
      val createdTerm = resp.as[TermResponse.Root]

      val taxonTerms     = queryGetTaxon(taxon.formId).terms
      val responseParent = findTermById(taxonTerms, parentFormId).get
      responseParent.children.size must === (children.size + 1)
      responseParent.children.map(_.id) must contain(createdTerm.id)
    }

    "fails if both parent and sibling are specified" in new HierarchyTerms_Baked {
      val attributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

      val sibling = links.filter(_.parentIndex.isDefined).head
      val parent  = links.filter(_.index == sibling.parentIndex.get).head

      val siblingFormId = Terms.mustFindById404(sibling.taxonTermId).gimme.formId
      val parentFormId  = Terms.mustFindById404(parent.taxonTermId).gimme.formId

      val resp = POST(s"v1/taxonomy/${ctx.name}/${taxon.formId}",
                      CreateTermPayload(attributes = attributes,
                                        parent = Some(parentFormId),
                                        Some(siblingFormId)))

      resp.status must === (StatusCodes.BadRequest)
    }
  }

  "PATCH v1/taxonomy/{contextName}/term/{termFormId}" - {
    "moves terms between subtrees" in new HierarchyTerms_Baked {
      val taxonTerms = queryGetTaxon(taxon.formId).terms

      val List(left, right) = taxonTerms.take(2)
      val termToMoveId      = left.children.head.id
      val newParentId       = right.id

      val resp = PATCH(s"v1/taxonomy/${ctx.name}/term/$termToMoveId",
                       UpdateTermPayload(None, Some(newParentId), None))
      resp.status must === (StatusCodes.OK)

      val taxonTermsAfter = queryGetTaxon(taxon.formId).terms

      val List(leftAfter, rightAfter) = taxonTermsAfter.take(2)
      leftAfter.children.size must === (left.children.size - 1)
      rightAfter.children.size must === (right.children.size + 1)
      rightAfter.children.map(_.id) must contain(termToMoveId)
    }
  }
  /*
  "PATCH v1/taxonomy/{contextName}/term/{termFormId}" - {}
  "DELETE v1/taxonomy/{contextName}/term/{termFormId}" - {}
 */
}
