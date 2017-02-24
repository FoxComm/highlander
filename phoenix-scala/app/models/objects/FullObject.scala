package models.objects

import java.time.Instant

import models.objects.FormAndShadow._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases.Json

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled: (ObjectForm, ObjectShadow) = form → shadow

  def toPayload: Map[String, Json] = {
    val attributes = IlluminateAlgorithm
      .projectAttributes(formJson = this.form.attributes, shadowJson = this.shadow.attributes)
    attributes match {
      case JObject(o) ⇒
        o.foldLeft(Map.empty[String, Json]) {
          case (acc, (fieldName, jvalue)) ⇒
            acc + (fieldName → jvalue)
        }
      case _ ⇒ throw new IllegalArgumentException("Invalid attributes")
    }

  }

  def mergeShadowAttrs(newShadowAttrs: Json): FormAndShadow = {
    val newShadow = this.shadow.copy(attributes = this.shadow.attributes.merge(newShadowAttrs))
    FormAndShadowSimple(form = this.form, shadow = newShadow)
  }

  def isActive: Boolean = {
    implicit val formats = JsonFormatters.phoenixFormats

    def beforeNow(time: Instant) = time.isBefore(Instant.now)

    val activeFrom = getAttribute("activeFrom").extractOpt[Instant]
    val activeTo   = getAttribute("activeTo").extractOpt[Instant]

    activeFrom.exists(beforeNow) && !activeTo.exists(beforeNow)
  }

  def getAttribute(attr: String): Json =
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
