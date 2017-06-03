package objectframework.services

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import objectframework.content._

object ContentUtils {

  val KeyLength = 5

  /**
    * We compute a SHA-1 hash of the json content and return the first
    * 10 characters in hex representation of the hash.
    * We don't care about the whole hash because it would take up too much space.
    * Collisions are handled below in the findKey function.
    */
  private def hash(content: JValue): String =
    java.security.MessageDigest
      .getInstance("SHA-1") // shared instance would not be thread-safe
      .digest(compact(render(content)).getBytes)
      .slice(0, KeyLength)
      .map("%02x".format(_))
      .mkString

  /**
    * The key algorithm will compute a hash of the content and then search
    * for a valid key. The search function looks for hash collisions.
    * If a hash collision is found, an index is appended to the hash and the
    * new hash+index key is searched until we find a key with same content or
    * we reach the end of the list.
    */
  private[objectframework] def key(content: JValue, alreadyExistingFields: JValue): String = {
    val hashKey = hash(content)

    def noHashCollision(newHash: String): Boolean = {
      val value = alreadyExistingFields \ newHash
      value == JNothing || value == content
    }

    Stream
      .from(0)
      .map(i ⇒ if (i == 0) hashKey else s"$hashKey/$i")
      .find(noHashCollision)
      .get // safe as the stream is [0;∞)
  }

  /**
    * Take a raw, illuminated set of attributes in the form of what exists
    * in payloads or Content, and turn it into the encoded Form and Shadow
    * attributes.
    */
  def encodeContentAttributes(attributes: Content.ContentAttributes): (JValue, JValue) =
    attributes.foldLeft((JNothing: JValue, JNothing: JValue)) {
      case (acc, (rawKey, value)) ⇒
        val (form, shadow) = acc

        val hashedKey = key(rawKey, form)

        val formJson: JValue   = hashedKey → value.v
        val shadowJson: JValue = rawKey    → (("type" → value.t) ~ ("ref" → hashedKey))

        (form.merge(formJson), shadow.merge(shadowJson))
    }

  def attributesForUpdate(form: Form,
                          shadow: Shadow,
                          attributes: Option[Content.ContentAttributes]): (JValue, JValue) =
    attributes match {
      case Some(attrs) ⇒
        val (newForm, newShadow) = encodeContentAttributes(attrs)
        (form.attributes.merge(newForm), newShadow)
      case None ⇒
        (form.attributes, shadow.attributes)
    }

  def buildRelations(rawRelations: Option[JValue])(implicit fmt: Formats): Content.ContentRelations =
    rawRelations.flatMap(_.extract[Option[Content.ContentRelations]]) match {
      case Some(relations) ⇒ relations
      case None            ⇒ Map.empty[String, Seq[Commit#Id]]
    }

  def updateRelations(existingRelations: Option[JValue], newRelations: Option[Content.ContentRelations])(
      implicit fmt: Formats): Content.ContentRelations =
    (buildRelations(existingRelations), newRelations) match {
      case (existingRels, None) ⇒
        existingRels
      case (existingRels, Some(newRels)) if existingRels.isEmpty ⇒
        newRels
      case (existingRels, Some(newRels)) ⇒
        existingRels.foldLeft(Content.emptyRelations) {
          case (acc, (key, commits)) ⇒
            newRels.get(key) match {
              case Some(newCommits) if newCommits.isEmpty ⇒
                acc
              case Some(newCommits) if newCommits.nonEmpty ⇒
                acc + (key → newCommits)
              case None ⇒
                acc + (key → commits)
            }
        }
    }
}
