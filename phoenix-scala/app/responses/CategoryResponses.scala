package responses

import entities.{Category ⇒ CategoryEntity}

object CategoryResponses {

  case class Location(parentId: Option[Int], position: Int)

  case class Category(id: Int, name: String, location: Location)

  object Category {
    def build(category: CategoryEntity): Category = {
      Category(id = category.id,
               name = category.name,
               location = Location(parentId = category.parentId, position = category.position))
    }
  }
}
