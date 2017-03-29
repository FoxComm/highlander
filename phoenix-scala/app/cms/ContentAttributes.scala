package cms

import cats.data._
import cats.implicits._
import failures._
import org.json4s._
import org.json4s.jackson.JsonMethods._

package object content {
  type ContentAttributes = Map[String, ContentType]

  implicit class EnhancedContentAttributes(ca: ContentAttributes) {
    def getString(key: String): Failures Xor String = {
      ca.get(key) match {
        case Some(contentType) ⇒
          contentType.getString
        case None ⇒
          Xor.left(GeneralFailure("Cannot find attribute").single)
      }
    }
  }

  implicit class EnhancedContentType(ct: ContentType) {
    implicit val formats = DefaultFormats

    def getString(): Failures Xor String =
      ct.v.extractOpt[String] match {
        case Some(v) ⇒ Xor.right(v)
        case None    ⇒ Xor.left(GeneralFailure("Cannot extract string").single)
      }
  }
}
