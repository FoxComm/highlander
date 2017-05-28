package objectframework.db

import core.db.ExPostgresDriver.api._
import core.db._

import objectframework.content._

object ContentQueries {
  type QuerySeq     = Query[(Commits, Forms, Shadows), (Commit, Form, Shadow), Seq]
  type HeadQuerySeq = Query[(Heads, Commits, Forms, Shadows), (Head, Commit, Form, Shadow), Seq]

  def filterLatestById(id: Form#Id, viewId: View#Id): HeadQuerySeq =
    for {
      head   ← Heads.filter(h ⇒ h.id === id && h.viewId === viewId)
      commit ← Commits if commit.id === head.commitId
      form   ← Forms if form.id === commit.formId
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (head, commit, form, shadow)

  def filterByCommit(commitId: Commit#Id): QuerySeq =
    for {
      commit ← Commits.filter(_.id === commitId)
      form   ← Forms if form.id === commit.formId
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)

  def filterRelation(kind: String, commits: Seq[Commit#Id]): QuerySeq =
    for {
      commit ← Commits.filter(_.id.inSet(commits))
      form   ← Forms if form.id === commit.formId && form.kind === kind
      shadow ← Shadows if shadow.id === commit.shadowId
    } yield (commit, form, shadow)
}
