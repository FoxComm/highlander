package objectframework.content

import java.time.Instant
import scala.concurrent.ExecutionContext

import cats.data._
import cats.implicits._
import org.json4s._
import org.json4s.JsonDSL._

import core.db.ExPostgresDriver.api._
import core.db._

import objectframework.db._
import objectframework.ObjectFailures._

package object illuminated {
  type ContentAttributes = Map[String, ContentAttribute]

  type ContentRelations = Map[String, Seq[Commit#Id]]

  type FullContentRelations = Map[String, Seq[Content]]

  object Contents {
    def findLatestById(id: Form#Id, viewId: View#Id, kind: String)(
        implicit ec: ExecutionContext): DbResultT[Content] = {
      for {
        contentTuple ← * <~ ContentQueries
                        .filterLatestById(id, viewId)
                        .mustFindOneOr(ObjectNotFound(kind, id, viewId))
        (head, commit, form, shadow) = contentTuple
        content ← * <~ Content.build(head, commit, form, shadow)
      } yield content
    }

    def findByCommit(commitId: Commit#Id, kind: String)(
        implicit ec: ExecutionContext): DbResultT[Content] =
      for {
        contentTuple ← * <~ ContentQueries
                        .filterByCommit(commitId)
                        .mustFindOneOr(ObjectNotFoundAtCommit(kind, commitId))
        (commit, form, shadow) = contentTuple
        content ← * <~ Content.build(commit, form, shadow)
      } yield content

    def populateRelations(content: Content)(
        implicit ec: ExecutionContext): DbResultT[FullContentRelations] = {
      val empty = DbResultT.pure(Map.empty[String, Seq[Content]])

      content.relations.foldLeft(empty) { (accRelations, relation) ⇒
        accRelations.flatMap { relations ⇒
          val (kind, commits) = relation

          for {
            rawRelations ← * <~ ContentQueries.filterRelation(kind, commits).result
            contents ← * <~ rawRelations.map {
                        case (commit, form, shadow) ⇒
                          DbResultT.fromEither(Content.build(commit, form, shadow))
                      }
          } yield addRelation(relations, kind, contents)
        }
      }
    }

    private def addRelation(relations: Map[String, Seq[Content]],
                            kind: String,
                            content: Seq[Content]): Map[String, Seq[Content]] =
      relations.get(kind) match {
        case Some(existingContent) ⇒
          val mergedContent = (existingContent ++ content).toSet.toList
          relations + (kind → mergedContent)
        case None ⇒
          relations + (kind → content)
      }

  }

}
