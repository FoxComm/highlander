package models.product

import cats.data._
import failures._
import cms._
import cms.content._

/**
  * A version of Product based on the new CMS construct.
  * If this proposal is accepted, it will replace the base Product model.
  */
case class ProductContent(id: Int,
                          commitId: Int,
                          contextId: Int,
                          title: String,
                          attributes: ContentAttributes)

object ProductContent {
  def build(content: Content): Failures Xor ProductContent = {
    content.attributes.getString("title").flatMap { title ⇒
      Xor.right(
          ProductContent(id = content.id,
                         commitId = content.commitId,
                         contextId = content.contextId,
                         title = title,
                         attributes = content.attributes))
    }
  }
}
