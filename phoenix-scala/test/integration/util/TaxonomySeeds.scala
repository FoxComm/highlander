package util

import com.github.tminglei.slickpg.LTree
import models.objects.{ObjectForm, ObjectShadow, ObjectUtils}
import models.taxonomy._
import org.json4s.JsonDSL._
import util.fixtures.TestFixtureBase
import utils.aliases.Json
import utils.db._

trait TaxonomySeeds extends TestFixtureBase {

  trait FlatTerms_Baked extends Taxon_Seed with FlatTerms_Raw
  trait HierarchyTerms_Baked extends Taxon_Seed with HierarchicalTerms_Raw {
    override def taxonHierarchical = true
  }

  trait Taxon_Seed {
    val taxonAttributes: Map[String, Json] = Map("name" → (("t" → "string") ~ ("v" → "taxon")),
                                                 "test" → (("t" → "string") ~ ("v" → "taxon")))
    def taxonHierarchical = false

    private def _taxon: Taxon = {
      val form   = ObjectForm.fromPayload(Taxon.kind, taxonAttributes)
      val shadow = ObjectShadow.fromPayload(taxonAttributes)

      (for {
        ins ← * <~ ObjectUtils.insert(form, shadow)
        taxon ← * <~ Taxons.create(
                   Taxon(id = 0,
                         hierarchical = taxonHierarchical,
                         ctx.id,
                         ins.form.id,
                         ins.shadow.id,
                         ins.commit.id))
      } yield taxon).gimme
    }

    val taxon: Taxon = _taxon
  }

  trait TermSeedBase {
    val termAttributesBase = Map("name" → (("t" → "string") ~ ("v" → "term")))

    def createTerm(attributes: Map[String, Json]) = {
      val form   = ObjectForm.fromPayload(Term.kind, attributes)
      val shadow = ObjectShadow.fromPayload(attributes)

      (for {
        ins  ← * <~ ObjectUtils.insert(form, shadow)
        term ← * <~ Terms.create(Term(0, ctx.id, ins.shadow.id, ins.form.id, ins.commit.id))
      } yield term).gimme
    }

    def createLink(taxon: Taxon, term: Term, path: String, index: Int, position: Int) =
      TaxonTermLinks
        .create(TaxonTermLink(0, index, taxon.id, term.id, position, LTree(path)))
        .gimme
  }

  trait FlatTerms_Raw extends TermSeedBase {
    def taxon: Taxon

    val terms: Seq[Term] = createTerms(
        (1 to 2).map(i ⇒
              termAttributesBase + ("testIdx" → (("t" → "number") ~ ("v" →
                            i.toString)))))

    val links: Seq[TaxonTermLink] = {
      require(!taxon.hierarchical)
      Seq(createLink(taxon, terms.head, "", 1, 0), createLink(taxon, terms(1), "", 2, 1))
    }

    def createTerms(attributes: Seq[Map[String, Json]]): Seq[Term] =
      attributes.map(i ⇒ createTerm(i))
  }

  /*creates tree
       1   2
      /\  /\
     3 4 5 6
   */
  trait HierarchicalTerms_Raw extends TermSeedBase {
    def taxon: Taxon

    val terms: Seq[Term] = createTerms(
        (1 to 6).map(i ⇒
              termAttributesBase + ("testIdx" → (("t" → "number") ~ ("v" →
                            i.toString)))))

    val links: Seq[TaxonTermLink] = {
      require(taxon.hierarchical)
      Seq(createLink(taxon, terms.head, "", 1, 0), createLink(taxon, terms(1), "", 2, 1)) ++
      Seq(createLink(taxon, terms(2), "1", 3, 0), createLink(taxon, terms(3), "1", 4, 1)) ++
      Seq(createLink(taxon, terms(4), "2", 5, 0), createLink(taxon, terms(5), "2", 6, 1))
    }

    def createTerms(attributes: Seq[Map[String, Json]]): Seq[Term] =
      attributes.map(i ⇒ createTerm(i))
  }
}
