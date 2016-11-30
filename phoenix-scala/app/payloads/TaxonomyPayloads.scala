package payloads

import cats.data.ValidatedNel
import failures.Failure
import models.objects.ObjectUtils._
import models.objects.{FormAndShadow, ObjectForm, ObjectShadow}
import models.taxonomy.Taxon
import payloads.ObjectPayloads.{AttributesBuilder, StringField}
import utils.Validation
import utils.aliases._

object TaxonomyPayloads {
  type AttributesMap = Map[String, Json]

  case class CreateTaxonomyPayload(attributes: AttributesMap,
                                   hierarchical: Boolean,
                                   scope: Option[String] = None)

  case class UpdateTaxonomyPayload(attributes: AttributesMap)

  case class TaxonLocation(parent: Option[Int], position: Option[Int])
      extends Validation[TaxonLocation] {
    override def validate: ValidatedNel[Failure, TaxonLocation] =
      position
        .fold(Validation.ok)(Validation.greaterThanOrEqual(_, 0, "location.position"))
        .map(_ ⇒ this)
  }

  case class CreateTaxonPayload(name: String,
                                location: Option[TaxonLocation],
                                scope: Option[String] = None)
      extends Validation[CreateTaxonPayload] {
    override def validate: ValidatedNel[Failure, CreateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)

    def formAndShadow: FormAndShadow = {

      val jsonBuilder: AttributesBuilder =
        ObjectPayloads.AttributesBuilder(StringField("name", name))

      (ObjectForm(kind = Taxon.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }

  case class UpdateTaxonPayload(name: Option[String], location: Option[TaxonLocation])
      extends Validation[UpdateTaxonPayload] {
    override def validate: ValidatedNel[Failure, UpdateTaxonPayload] =
      location.map(_.validate).getOrElse(Validation.ok).map(_ ⇒ this)

    def formAndShadow: FormAndShadow = {
      val jsonBuilder: AttributesBuilder =
        ObjectPayloads.optionalAttributes(name.map(StringField("name", _)))

      (ObjectForm(kind = Taxon.kind, attributes = jsonBuilder.objectForm),
       ObjectShadow(attributes = jsonBuilder.objectShadow))
    }
  }
}
