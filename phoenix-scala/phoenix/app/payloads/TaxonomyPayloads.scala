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
}
