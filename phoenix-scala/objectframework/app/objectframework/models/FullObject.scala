package objectframework.models

import objectframework.models.FormAndShadow._
import objectframework.{IlluminateAlgorithm, ObjectUtils}
import org.json4s.JsonAST.{JObject, JValue}
import org.json4s.JsonDSL._

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled: (ObjectForm, ObjectShadow) = form → shadow

  def toPayload: Map[String, JValue] = {
    val attributes = IlluminateAlgorithm
      .projectAttributes(formJson = this.form.attributes, shadowJson = this.shadow.attributes)
    attributes match {
      case JObject(o) ⇒
        o.foldLeft(Map.empty[String, JValue]) {
          case (acc, (fieldName, jvalue)) ⇒
            acc + (fieldName → jvalue)
        }
      case _ ⇒ throw new IllegalArgumentException("Invalid attributes")
    }

  }

  def mergeShadowAttrs(newShadowAttrs: JValue): FormAndShadow = {
    val newShadow = this.shadow.copy(attributes = this.shadow.attributes.merge(newShadowAttrs))
    FormAndShadowSimple(form = this.form, shadow = newShadow)
  }

  def getAttribute(attr: String): JValue =
    IlluminateAlgorithm.get(attr, form.attributes, shadow.attributes)

  def setAttribute(attr: String, attrType: String, value: JValue): FormAndShadow = {
    val (keyMap, newForm) = ObjectUtils.createForm(attr → value)

    assert(keyMap.size == 1)
    assert(keyMap.head._1 == attr)

    val (_, key) = keyMap.head

    val newAttribute: JValue        = attr → (("type" → attrType) ~ ("ref" → key))
    val newShadowAttributes: JValue = shadow.attributes.merge(newAttribute)
    update(form.copy(attributes = form.attributes.merge(newForm)),
           shadow.copy(attributes = newShadowAttributes))
  }

  def projectAttributes(): JValue =
    IlluminateAlgorithm.projectAttributes(form.attributes, shadow.attributes)

  def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow
}

object FormAndShadow {
  case class FormAndShadowSimple(form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow {
    override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
      copy(form = form, shadow = shadow)
  }

  def fromPayload(kind: String, attributes: Map[String, JValue]): FormAndShadow =
    FormAndShadowSimple(form = ObjectForm.fromPayload(kind, attributes),
                        shadow = ObjectShadow.fromPayload(attributes))
}

case class FullObject[A](model: A, form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow {
  override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
    copy(form = form, shadow = shadow)
}
