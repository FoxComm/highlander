package models.objects

object ObjectReference {
  def apply(id: Int): ObjectReference = ObjectId(id)
  def apply(slug: String): ObjectReference = ObjectSlug(slug)
}

trait ObjectReference

case class ObjectId(id: ObjectForm#Id) extends ObjectReference
case class ObjectSlug(slug: String) extends ObjectReference