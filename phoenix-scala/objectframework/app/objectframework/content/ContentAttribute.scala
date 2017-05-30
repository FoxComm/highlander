package objectframework.content

import org.json4s._

import cats.implicits._
import core.failures.Failures

import objectframework.failures._

case class ContentAttribute(t: String, v: JValue)

object ContentAttribute {
  def build(shadowAttribute: JValue, form: Form): Either[Failures, ContentAttribute] = {
    def attributeType = shadowAttribute \ "type"
    def attributeRef  = shadowAttribute \ "ref"

    (attributeType, attributeRef) match {
      case (JString(attrType), JString(attrRef)) ⇒
        val attrVal = form.attributes \ attrRef
        Either.right(ContentAttribute(t = attrType, v = attrVal))
      case _ ⇒
        Either.left(CorruptedContentObject.single)
    }
  }
}
