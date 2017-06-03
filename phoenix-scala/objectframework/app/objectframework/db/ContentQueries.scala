package objectframework.db

import org.json4s._
import org.json4s.JsonDSL._

import core.db.ExPostgresDriver.api._
import core.db._
import shapeless._
import slick.jdbc.GetResult
import slick.sql.SqlStreamingAction

import objectframework.content._

object ContentQueries {
  type QuerySeq          = Query[(Commits, Forms, Shadows), (Commit, Form, Shadow), Seq]
  type LatestQuerySeq    = Query[(Heads, Commits, Forms, Shadows), (Head, Commit, Form, Shadow), Seq]
  type HeadQuerySeq      = Query[Heads, Head, Seq]
  type QueryCommitSeq    = Query[Commits, Commit, Seq]
  type IntSeq            = Query[Rep[Int], Int, Seq]
  type StreamingQuery[T] = SqlStreamingAction[Vector[T], T, Effect.All]

  def filterLatest(id: Form#Id, viewId: View#Id, kind: String): LatestQuerySeq =
    for {
      head   ← filterHead(id, viewId, kind)
      commit ← Commits if commit.id === head.commitId
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (head, commit, form, shadow)

  def filterByCommit(commitId: Commit#Id, kind: String): QuerySeq =
    for {
      commit ← Commits.filter(_.id === commitId)
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)

  def filterHead(id: Form#Id, viewId: View#Id, kind: String): HeadQuerySeq =
    Heads.filter(h ⇒ h.id === id && h.viewId === viewId && h.kind === kind)

  def filterRelation(kind: String, commits: Seq[Commit#Id]): QuerySeq =
    for {
      commit ← Commits.filter(_.id.inSet(commits))
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)

  def filterCommits(kind: String, commits: Seq[Commit#Id]): QueryCommitSeq =
    for {
      commit ← Commits.filter(_.id.inSet(commits))
      form   ← Forms if form.id === commit.formId && form.kind === kind
    } yield commit

  def filterCommitIds(kind: String, commits: Seq[Commit#Id]): IntSeq =
    filterCommits(kind, commits).map(_.id)

  def filterParentIds(commitId: Commit#Id, kind: String): StreamingQuery[(Int, String)] =
    sql"""select commit.id, head.kind
          from object_commits as commit
            inner join object_shadows as shadow on (shadow.id = commit.shadow_id)
            inner join heads as head on (head.commit_id = commit.id)
          where
            (shadow.relations->>'#$kind')::jsonb @> '[#$commitId]'""".as[(Int, String)]
}
