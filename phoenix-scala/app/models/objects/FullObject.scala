package models.objects

import org.json4s.JsonDSL._
import utils.IlluminateAlgorithm
import utils.aliases.Json
import FormAndShadow._
import org.json4s.JsonAST.JObject

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled = form → shadow

  def toPayload: Map[String, Json] = {
    val attributes = IlluminateAlgorithm
      .projectAttributes(formJson = this.form.attributes, shadowJson = this.shadow.attributes)
    attributes match {
      case JObject(o) ⇒ o.toMap
      case _ ⇒ throw new IllegalArgumentException("Invalid attributes")
    }

  }

  def mergeShadowAttrs(newShadowAttrs: Json): FormAndShadow = {
    val newShadow = this.shadow.copy(attributes = this.shadow.attributes.merge(newShadowAttrs))
    FormAndShadowSimple(form = this.form, shadow = newShadow)
  }

  def getAttribute(attr: String) =
    IlluminateAlgorithm.get(attr, form.attributes, shadow.attributes)

  def setAttribute(attr: String, attrType: String, value: Json): FormAndShadow = {
    val (keyMap, newForm) = ObjectUtils.createForm(attr → value)

    assert(keyMap.size == 1)
    assert(keyMap.head._1 == attr)

    val (_, key) = keyMap.head

    val newAttribute: Json        = attr → (("type" → attrType) ~ ("ref" → key))
    val newShadowAttributes: Json = shadow.attributes.merge(newAttribute)
    update(form.copy(attributes = form.attributes.merge(newForm)),
           shadow.copy(attributes = newShadowAttributes))
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
