package models.objects

import io.circe.Json
import models.objects.FormAndShadow._
import utils.IlluminateAlgorithm
import utils.aliases.Json

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled: (ObjectForm, ObjectShadow) = form → shadow

  // FIXME @kjanosz: why not empty map
  def toPayload: Map[String, Json] = {
    val attributes = IlluminateAlgorithm
      .projectAttributes(formJson = form.attributes, shadowJson = shadow.attributes)
    attributes.asObject
      .map(_.toMap)
      .getOrElse(throw new IllegalArgumentException("Invalid attributes"))
  }

  def mergeShadowAttrs(newShadowAttrs: Json): FormAndShadow = {
    val newShadow = this.shadow.copy(attributes = this.shadow.attributes.deepMerge(newShadowAttrs))
    FormAndShadowSimple(form = this.form, shadow = newShadow)
  }

  def getAttribute(attr: String): Option[Json] =
    IlluminateAlgorithm.get(attr, form.attributes, shadow.attributes)

  // FIXME @kjanosz: remove assertions
  def setAttribute(attr: String, attrType: String, value: Json): FormAndShadow = {
    val (keyMap, newForm) = ObjectUtils.createForm(Json.obj(attr → value))

    assert(keyMap.size == 1)
    assert(keyMap.head._1 == attr)

    val (_, key) = keyMap.head

    val newAttribute =
      Json.obj(attr → Json.obj("type" → Json.fromString(attrType), "ref" → Json.fromString(key)))
    update(form.copy(attributes = form.attributes.deepMerge(newForm)),
           shadow.copy(attributes = shadow.attributes.deepMerge(newAttribute)))
  }

  def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow
}

object FormAndShadow {
  case class FormAndShadowSimple(form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow {
    override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
      copy(form = form, shadow = shadow)
  }

  def fromPayload(kind: String, attributes: Map[String, Json]): FormAndShadow = {
    FormAndShadowSimple(form = ObjectForm.fromPayload(kind, attributes),
                        shadow = ObjectShadow.fromPayload(attributes))
  }
}

case class FullObject[A](model: A, form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow {
  override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
    copy(form = form, shadow = shadow)
}
