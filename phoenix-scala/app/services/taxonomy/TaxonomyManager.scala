package services.taxonomy

import java.time.Instant

import cats.data.ValidatedNel
import com.github.tminglei.slickpg.LTree
import failures.Failure
import failures.TaxonomyFailures._
import models.objects._
import models.taxonomy._
import payloads.TaxonomyPayloads._
import responses.TaxonomyResponses._
import services.objects.ObjectManager
import utils.Validation
import utils.aliases.{DB, EC, Json, OC}
import utils.db.ExPostgresDriver.api._
import utils.db._

object TaxonomyManager {

  case class MoveSpec(term: TaxonTermLink,
    parent: Option[TaxonTermLink],
    sibling: Option[TaxonTermLink])
    extends Validation[MoveSpec] {

    def moveRequired: Boolean =
      term.path != newPath || (sibling.isDefined && sibling.get.position + 1 != term.position)

    override def validate: ValidatedNel[Failure, MoveSpec] = {

      (Validation.validExpr(sibling.isEmpty || sibling.isDefined && parent.isEmpty,
        "'parent' should be empty if 'sibling' is defined")
        |@|
        Validation.validExpr(term.taxonomyId == parent
          .orElse(sibling)
          .map(_.taxonomyId)
          .getOrElse(term.taxonomyId),
          "term should be in the same taxon as parent (sibling)")).map {
        case _ ⇒ this
      }

    }

    def newPath: LTree =
      parent.map(_.childPath).orElse(sibling.map(_.path)).getOrElse(LTree(List()))
  }

  def getTaxon(
      taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[TaxonResponse.Root] =
    for {
      taxon ← * <~ ObjectUtils.getFullObject(Taxons.mustFindByFormId404(taxonFormId))
      terms ← * <~ TaxonTermLinks.queryRightByLeftWithLinks(taxon.model)
    } yield TaxonResponse.build(taxon, terms)

  def createTaxon(payload: CreateTaxonPayload)(implicit ec: EC,
                                               oc: OC): DbResultT[TaxonResponse.Root] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      ins ← * <~ ObjectUtils.insert(form, shadow)
      taxon ← * <~ Taxons.create(
                 Taxon(id = 0,
                       hierarchical = payload.hierarchical,
                       oc.id,
                       ins.form.id,
                       ins.shadow.id,
                       ins.commit.id))
    } yield TaxonResponse.build(FullObject(taxon, ins.form, ins.shadow), Seq())
  }

  def updateTaxon(taxonFormId: ObjectForm#Id, payload: UpdateTaxonPayload)(
      implicit ec: EC,
      oc: OC,
      db: DB): DbResultT[TaxonResponse.Root] = {
    val form   = ObjectForm.fromPayload(Taxon.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxon ← * <~ ObjectUtils.getFullObject(Taxons.mustFindByFormId404(taxonFormId))
      newTaxon ← * <~ ObjectUtils.commitUpdate(taxon,
                                               form.attributes,
                                               taxon.shadow.attributes.merge(shadow.attributes),
                                               Taxons.updateHead)
      terms ← * <~ TaxonTermLinks.queryRightByLeftWithLinks(newTaxon.model)
    } yield TaxonResponse.build(newTaxon, terms)
  }

  def archiveByContextAndId(
      taxonFormId: ObjectForm#Id)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      //TODO: do we need to archivate ObjectForm too
      taxon ← * <~ Taxons.mustFindByFormId404(taxonFormId)
      archivedAt = Some(Instant.now)
      archived ← * <~ Taxons.update(taxon, taxon.copy(archivedAt = archivedAt))
      allLinks ← * <~ TaxonTermLinks.queryRightByLeftWithLinks(taxon)
      _ ← * <~ allLinks.map {
           case (_, link) ⇒ TaxonTermLinks.update(link, link.copy(archivedAt = archivedAt))
         }
      _ ← * <~ allLinks.map { case (term, _) ⇒ term.model }.distinct.map(term ⇒
               Terms.update(term, term.copy(archivedAt = archivedAt)))
    } yield {}

  def getTerm(termFormId: Int)(implicit ec: EC, oc: OC, db: DB): DbResultT[TermResponse.Root] =
    ObjectUtils.getFullObject(Terms.mustFindByFormId404(termFormId)).map(TermResponse.build)

  def createTerm(payload: CreateTermPayload)(implicit ec: EC,
                                             oc: OC): DbResultT[TermResponse.Root] = {
    val form   = ObjectForm.fromPayload(Term.kind, payload.attributes)
    val shadow = ObjectShadow.fromPayload(payload.attributes)

    for {
      taxon ← * <~ Taxons.mustFindByFormId404(payload.taxonId)

      ins  ← * <~ ObjectUtils.insert(form, shadow)
      term ← * <~ Terms.create(Term(id = 0, oc.id, ins.form.id, ins.shadow.id, ins.commit.id))

      parentLink ← * <~ (if (payload.parent.isDefined)
                           TaxonTermLinks
                             .mustFindByTaxonAndTermFormId(taxon, payload.parent.get)
                             .map(link ⇒ Some(link))
                         else DbResultT.none)
      siblingLink ← * <~ (if (payload.sibling.isDefined)
                            TaxonTermLinks
                              .mustFindByTaxonAndTermFormId(taxon, payload.sibling.get)
                              .map(link ⇒ Some(link))
                          else DbResultT.none)

      index ← * <~ TaxonTermLinks.nextIndex(taxon.id).result
      moveSpec ← * <~ MoveSpec(TaxonTermLink(0, index, taxon.id, term.id, -1, LTree("")),
                               parentLink,
                               siblingLink).validate
      moveSpec2 = moveSpec.copy(term = moveSpec.term.copy(path = moveSpec.newPath))
      link ← * <~ TaxonTermLinks.create(moveSpec.term)
      _ ← * <~ (if (moveSpec.sibling.isDefined)
                  TaxonTermLinks.moveTermAfter(link, moveSpec.sibling.get)
                else
                  DbResultT.good(link))
    } yield TermResponse.build(FullObject(term, ins.form, ins.shadow))
  }

  def mustFindSingleTaxonForTerm(term: Term)(implicit ec: EC,
                                             db: DB): DbResultT[FullObject[Taxon]] =
    for {
      taxons ← * <~ TaxonTermLinks.queryLeftByRight(term)
      taxon ← * <~ (if (taxons.length == 1) DbResultT.good(taxons.head)
                    else DbResultT.failure(InvalidTermTaxons(term, taxons.length)))
    } yield taxon

  def updateTermHierarchy(term: Term, parent: Option[Int], sibling: Option[Int])(implicit ec: EC,
                                                                                 db: DB,
                                                                                 oc: OC) =
    for {
      taxon ← * <~ mustFindSingleTaxonForTerm(term)
      parentTerm ← * <~ (if (parent.isDefined)
                           TaxonTermLinks
                             .mustFindByTaxonAndTermFormId(taxon.model, parent.get)
                             .map(link ⇒ Some(link))
                         else DbResultT.none)
      siblingTerm ← * <~ (if (sibling.isDefined)
                            TaxonTermLinks
                              .mustFindByTaxonAndTermFormId(taxon.model, sibling.get)
                              .map(link ⇒ Some(link))
                          else DbResultT.none)
      link     ← * <~ TaxonTermLinks.mustFindByTaxonAndTermFormId(taxon.model, term.formId)
      moveSpec ← * <~ MoveSpec(link, parentTerm, siblingTerm).validate
      _ ← * <~ (if (moveSpec.moveRequired) moveTerm(taxon.model, moveSpec) else DbResultT.good(Unit))
    } yield term

  private def moveTerm(taxon: Taxon, moveSpec: MoveSpec)(implicit ec: EC, oc: OC, db: DB) = for {
    _ ← * <~ TaxonTermLinks.archivate(moveSpec.term)
    newPath ← * <~ moveSpec.newPath
    newLink ← * <~ TaxonTermLinks.create(moveSpec.term.copy(id = 0, position = -1, path = newPath))
    _ ← * <~ TaxonTermLinks.updatePath(taxon.id, moveSpec.term.path, newPath)
    _ ← * <~ (if (moveSpec.sibling.isDefined)
      TaxonTermLinks.moveTermAfter(newLink, moveSpec.sibling.get)
    else
      DbResultT.good(newLink))
  } yield {}

  def updateTerm(termId: Int, payload: UpdateTermPayload)(implicit ec: EC,
                                                          oc: OC,
                                                          db: DB): DbResultT[TermResponse.Root] = {
    for {
      term ← * <~ Terms.mustFindByFormId404(termId)
      newTerm ← * <~ (payload.attributes match {
                     case Some(attributes) ⇒ updateTermAttributes(term, attributes)
                     case _                ⇒ DbResultT.good(term)
                   })

      _ ← * <~ updateTermHierarchy(term, payload.parent, payload.sibling)
      r ← * <~ ObjectManager.getFullObject(DbResultT.good(newTerm))
    } yield TermResponse.build(r)
  }

  private def updateTermAttributes(
      term: Term,
      newAttributes: Map[String, Json])(implicit ec: EC, db: DB, oc: OC): DbResultT[Term] = {
    val form   = ObjectForm.fromPayload(Term.kind, newAttributes)
    val shadow = ObjectShadow.fromPayload(newAttributes)

    for {
      fullTerm ← * <~ ObjectUtils.getFullObject(DbResultT.good(term))
      newTerm ← * <~ ObjectUtils.commitUpdate(fullTerm,
                                              form.attributes,
                                              fullTerm.shadow.attributes.merge(shadow.attributes),
                                              Terms.updateHead)
    } yield newTerm.model
  }

  def archiveTermByContextAndId(
      termFormId: Int)(implicit ec: EC, oc: OC, db: DB): DbResultT[Unit] =
    for {
      links ← * <~ TaxonTermLinks.filterByTermFormId(termFormId).result
      _     ← * <~ links.map(TaxonTermLinks.archivate)
    } yield {}
}
