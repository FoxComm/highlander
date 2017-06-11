package pim.models

import java.time.Instant

import objectframework.content._

case class Image(id: Int,
                 viewId: Option[View#Id],
                 commitId: Option[Commit#Id],
                 attributes: Content.ContentAttributes,
                 createdAt: Instant,
                 updatedAt: Instant,
                 archivedAt: Option[Instant]) {

  def alt: Option[String] = {
    attributes.get("alt").flatMap { attr ⇒
      attr.v.extract[Option[String]]
    }
  }
}

object Image {
  // def build(content: Content): Either[Failures, Image] = {
  //   content.attributes.get("src") match {
  //     case Some(src) ⇒
  //       Image(id = content.id,
  //         viewId = content.viewId,
  //         commitId = content.commitId,
  //         alt = content.attributes.get("alt"),
  //         src = src,
  //         title = content.attributes.get("title").map
  //     case None ⇒
  //   }

  //   Image(id = content.id,
  //     viewId = content.viewId,
  //     commitId = content.commitId,
  //     alt = None,

  // }
}
