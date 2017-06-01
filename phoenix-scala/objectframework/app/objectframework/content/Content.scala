package objectframework.content

import java.time.Instant

import cats.implicits._
import core.failures.Failures
import org.json4s._

import objectframework.services.ContentUtils

/**
  * Content is an illuminated content object: it's the most primitive Object
  * Framework entity that should be used outside this library. Any objects
  * that are implemented on top of this model should leverage Content.
  */
case class Content(id: Int,
                   kind: String,
                   viewId: Option[View#Id],
                   commitId: Commit#Id,
                   attributes: Content.ContentAttributes,
                   relations: Content.ContentRelations,
                   createdAt: Instant,
                   updatedAt: Instant,
                   archivedAt: Option[Instant])

object Content {
  implicit val formats = DefaultFormats

  type ContentAttributes = Map[String, ContentAttribute]
  type ContentRelations  = Map[String, Seq[Commit#Id]]

  def build(commit: Commit, form: Form, shadow: Shadow): Either[Failures, Content] =
    buildContentAttributes(form, shadow).map { attributes ⇒
      Content(
        id = form.id,
        kind = form.kind,
        viewId = None,
        commitId = commit.id,
        attributes = attributes,
        relations = ContentUtils.buildRelations(shadow.relations),
        createdAt = form.createdAt,
        updatedAt = shadow.createdAt,
        archivedAt = None
      )
    }

  def build(head: Head, commit: Commit, form: Form, shadow: Shadow): Either[Failures, Content] =
    build(commit, form, shadow).map(_.copy(viewId = Some(head.viewId), archivedAt = head.archivedAt))

  private def buildContentAttributes(form: Form, shadow: Shadow): Either[Failures, ContentAttributes] = {
    val emptyAttrs: Either[Failures, ContentAttributes] = Either.right(Map.empty)

    shadow.attributes match {
      case JObject(shadow) ⇒
        shadow.obj.foldLeft(emptyAttrs) { (contentAttributes, shadowAttr) ⇒
          contentAttributes match {
            case Right(attrs) ⇒ updateAttributes(attrs, shadowAttr, form)
            case _            ⇒ contentAttributes
          }
        }
      case _ ⇒
        emptyAttrs
    }
  }

  private def updateAttributes(attributes: ContentAttributes,
                               shadow: JField,
                               form: Form): Either[Failures, ContentAttributes] = {
    val (name, attr) = shadow
    ContentAttribute.build(attr, form).map { value ⇒
      attributes + (name → value)
    }
  }
}
