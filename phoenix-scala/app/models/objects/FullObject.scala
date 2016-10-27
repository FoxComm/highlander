package models.objects

import utils.IlluminateAlgorithm
import utils.aliases.Json
import FormAndShadow._

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled = form → shadow

  def toPayload: Map[String, Json] = {
    val attributes = IlluminateAlgorithm
      .projectAttributes(formJson = this.form.attributes, shadowJson = this.shadow.attributes)
    attributes.foldField(Map.empty[String, Json]) {
      case (acc, (fieldName, jvalue)) ⇒
        acc + (fieldName → jvalue)
    }
  }

  def mergeShadowAttrs(newShadowAttrs: Json): FormAndShadow = {
    val newShadow = this.shadow.copy(attributes = this.shadow.attributes.merge(newShadowAttrs))
    FormAndShadowSimple(form = this.form, shadow = newShadow)
  }
}

object FormAndShadow {
  case class FormAndShadowSimple(form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow

  def fromPayload(kind: String, attributes: Map[String, Json]): FormAndShadow = {
    FormAndShadowSimple(form = ObjectForm.fromPayload(kind, attributes),
                        shadow = ObjectShadow.fromPayload(attributes))
  }
}

case class FullObject[A](model: A, form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow
