package testutils

import java.time.Instant

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import objectframework.ObjectUtils
import objectframework.models._
import org.json4s.JsonDSL._
import phoenix.models.account.{Scope, User}
import phoenix.models.taxonomy._
import phoenix.services.Authenticator.AuthData
import phoenix.utils.aliases.Json
import testutils.fixtures.TestFixtureBase
import core.db._

trait TaxonomySeeds extends TestFixtureBase {

  trait FlatTaxons_Baked extends Taxonomy_Raw with FlatTaxons_Raw
  trait HierarchyTaxons_Baked extends Taxonomy_Raw with HierarchicalTaxons_Raw {
    override def taxonomyHierarchical = true
  }

  trait Taxonomy_Raw {
    implicit def au: AuthData[User]
    val taxonomyAttributes: Map[String, Json] =
      Map("name" → (("t" → "string") ~ ("v" → "taxon")), "test" → (("t" → "string") ~ ("v" → "taxon")))
    def taxonomyHierarchical = false

    private def _taxonomy: Taxonomy = {
      val form   = ObjectForm.fromPayload(Taxonomy.kind, taxonomyAttributes)
      val shadow = ObjectShadow.fromPayload(taxonomyAttributes)

      (for {
        ins ← * <~ ObjectUtils.insert(form, shadow)
        taxonomy ← * <~ Taxonomies.create(
                    Taxonomy(id = 0,
                             scope = Scope.current,
                             hierarchical = taxonomyHierarchical,
                             ctx.id,
                             ins.form.id,
                             ins.shadow.id,
                             ins.commit.id))
      } yield taxonomy).gimme
    }

    val taxonomy: Taxonomy = _taxonomy
  }

  trait TaxonSeedBase {
    implicit def au: AuthData[User]

    def createTaxon(attributes: Map[String, Json]) = {
      val form   = ObjectForm.fromPayload(Taxon.kind, attributes)
      val shadow = ObjectShadow.fromPayload(attributes)

      (for {
        ins  ← * <~ ObjectUtils.insert(form, shadow)
        term ← * <~ Taxons.create(Taxon(0, Scope.current, ctx.id, ins.shadow.id, ins.form.id, ins.commit.id))
      } yield term).gimme
    }

    def createLink(taxonomy: Taxonomy, taxon: Taxon, path: String, index: Int, position: Int) =
      TaxonomyTaxonLinks
        .create(TaxonomyTaxonLink(0, index, taxonomy.id, taxon.id, position, LTree(path)))
        .gimme
  }

  trait FlatTaxons_Raw extends TaxonSeedBase {
    def taxonomy: Taxonomy

    val taxonAttributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

    val taxonsNames        = (1 to 2).map("taxon" + _.toString)
    val taxonsAttributes   = taxonsNames.map(name ⇒ Map("name" → (("t" → "string") ~ ("v" → name))))
    val taxons: Seq[Taxon] = createTaxons(taxonsAttributes)

    val links: Seq[TaxonomyTaxonLink] = {
      require(!taxonomy.hierarchical)
      Seq(createLink(taxonomy, taxons.head, "", 1, 0), createLink(taxonomy, taxons(1), "", 2, 1))
    }

    def createTaxons(attributes: Seq[Map[String, Json]]): Seq[Taxon] =
      attributes.map(i ⇒ createTaxon(i))
  }

  /**
  Creates tree:
       1   2
      /\  /\
     3 4 5 6
    /
   7
    */
  trait HierarchicalTaxons_Raw extends TaxonSeedBase {
    def taxonomy: Taxonomy

    val taxonAttributes = Map("name" → (("t" → "string") ~ ("v" → "name")))

    val taxonsNames = (1 to 7).map(i ⇒ s"taxon$i")
    val taxons: Seq[Taxon] = {
      val attributes      = taxonsNames.map(name ⇒ Map("name" → (("t" → "string") ~ ("v" → name))))
      val taxonsToArchive = createTaxons(attributes)
      DbResultT
        .seqCollectFailures(
          taxonsToArchive
            .map(taxon ⇒ Taxons.update(taxon, taxon.copy(archivedAt = Some(Instant.now))))
            .toList)
        .gimme
      createTaxons(attributes)
    }

    val links: Seq[TaxonomyTaxonLink] = {
      require(taxonomy.hierarchical)
      def createLinks: Seq[TaxonomyTaxonLink] =
        Seq(createLink(taxonomy, taxons.head, "", 1, 0), createLink(taxonomy, taxons(1), "", 2, 1)) ++
          Seq(createLink(taxonomy, taxons(2), "1", 3, 0), createLink(taxonomy, taxons(3), "1", 4, 1)) ++
          Seq(createLink(taxonomy, taxons(4), "2", 5, 0), createLink(taxonomy, taxons(5), "2", 6, 1)) ++
          Seq(createLink(taxonomy, taxons(6), "1.3", 7, 1))
      val linksToArchive = createLinks

      DbResultT
        .seqCollectFailures(
          linksToArchive
            .map(l ⇒ TaxonomyTaxonLinks.update(l, l.copy(archivedAt = Some(Instant.now()))))
            .toList)
        .gimme
      createLinks
    }

    def createTaxons(attributes: Seq[Map[String, Json]]): Seq[Taxon] =
      attributes.map(i ⇒ createTaxon(i))
  }
}
