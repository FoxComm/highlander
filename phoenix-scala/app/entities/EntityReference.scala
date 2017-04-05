package entities

import models.objects.ObjectForm

object EntityReference {
  def apply(id: Int): EntityReference      = EntityId(id)
  def apply(slug: String): EntityReference = EntitySlug(slug)
}

trait EntityReference
case class EntityId(id: ObjectForm#Id) extends EntityReference
case class EntitySlug(slug: String) extends EntityReference
