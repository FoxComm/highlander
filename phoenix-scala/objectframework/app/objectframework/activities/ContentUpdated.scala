package objectframework.activities

import objectframework.content._

case class ContentUpdatedAttribute(name: String,
                                   oldAttribute: Option[ContentAttribute],
                                   newAttribute: Option[ContentAttribute])

case class ContentUpdatedRelation(kind: String, oldCommit: Commit#Id, newCommit: Commit#Id)

case class ContentUpdated(id: Int,
                          viewId: Int,
                          commitId: Int,
                          updatedAttributes: Option[Seq[ContentUpdatedAttribute]],
                          updatedRelations: Option[Seq[ContentUpdatedRelation]])
    extends Activity[ContentUpdated]

object ContentUpdated {
  def build(oldContent: Content, newContent: Content): ContentUpdated = {
    val oldKeySet = oldContent.attributes.keySet
    val newKeySet = newContent.attributes.keySet
    val allKeys   = oldKeySet.union(newKeySet)

    val attributesDiff = allKeys.foldLeft(Seq.empty[ContentUpdatedAttribute]) { (attributes, key) ⇒
      val oldAttr = oldContent.attributes.get(key)
      val newAttr = newContent.attributes.get(key)

      (oldAttr, newAttr) match {
        case (Some(oldA), Some(newA)) ⇒
          if (oldA.t != newA.t || oldA.v != newA.v)
            attributes :+ ContentUpdatedAttribute(key, oldAttr, newAttr)
          else
            attributes

        case (None, None) ⇒
          attributes

        case (_, _) ⇒
          attributes :+ ContentUpdatedAttribute(key, oldAttr, newAttr)
      }
    }

    val attributes = attributesDiff match {
      case attrs if attrs.isEmpty ⇒ None
      case _                      ⇒ Some(attributesDiff)
    }

    ContentUpdated(id = newContent.id,
                   viewId = newContent.viewId.getOrElse(0),
                   commitId = newContent.commitId,
                   updatedAttributes = attributes,
                   updatedRelations = None)
  }
}
