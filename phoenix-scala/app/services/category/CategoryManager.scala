package services.category

import failures.CategoryFailures._
import failures.ObjectFailures._
import models.account._
import models.category._
import models.objects._
import payloads.CategoryPayloads._
import responses.CategoryResponses._
import responses.ObjectResponses.ObjectContextResponse
import services.LogActivity
import slick.driver.PostgresDriver.api._
import com.github.tminglei.slickpg.LTree
import utils.aliases._
import utils.db._

object CategoryManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): DbResultT[CategoryFormResponse.Root] =
    for {
      category ← * <~ Categories.filterByFormId(id).mustFindOneOr(CategoryFormNotFound(id))
      form     ← * <~ ObjectForms.findById(id)
    } yield CategoryFormResponse.build(category, form)

  def getShadow(formId: Int, contextName: String)(implicit ec: EC,
                                                  db: DB): DbResultT[CategoryShadowResponse.Root] =
    for {
      context ← * <~ contextByName(contextName)
      category ← * <~ Categories
                  .withContextAndForm(context.id, formId)
                  .mustFindOneOr(CategoryNotFoundForContext(formId, context.id))
      shadow ← * <~ ObjectShadows.findById(category.shadowId)
    } yield CategoryShadowResponse.build(shadow)

  def getCategory(categoryId: Int, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[FullCategoryResponse.Root] =
    getCategoryFull(categoryId, contextName).map(c ⇒
          FullCategoryResponse.build(c.category, c.form, c.shadow))

  def createCategory(admin: User, payload: CreateFullCategory, contextName: String)(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[FullCategoryResponse.Root] =
    for {
      scope    ← * <~ Scope.resolveOverride(payload.scope)
      context  ← * <~ contextByName(contextName)
      form     ← * <~ ObjectForm(kind = Category.kind, attributes = payload.form.attributes)
      shadow   ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      insert   ← * <~ ObjectUtils.insert(form, shadow, payload.schema)
      category ← * <~ Categories.create(Category.build(scope, context.id, insert))
      response = FullCategoryResponse.build(category, insert.form, insert.shadow)
      _ ← * <~ LogActivity
           .fullCategoryCreated(Some(admin), response, ObjectContextResponse.build(context))
    } yield response

  def updateCategory(
      admin: User,
      categoryId: Int,
      payload: UpdateFullCategory,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[FullCategoryResponse.Root] =
    for {
      context  ← * <~ contextByName(contextName)
      category ← * <~ categoryById(categoryId, context)
      updated ← * <~ ObjectUtils.update(category.formId,
                                        category.shadowId,
                                        payload.form.attributes,
                                        payload.shadow.attributes)
      commit   ← * <~ ObjectUtils.commit(updated.form, updated.shadow, updated.updated)
      category ← * <~ updateCategoryHead(category, updated.shadow, commit)
      categoryResponse = FullCategoryResponse.build(category, updated.form, updated.shadow)
      contextResp      = ObjectContextResponse.build(context)
      _ ← * <~ LogActivity.fullCategoryUpdated(Some(admin), categoryResponse, contextResp)
    } yield categoryResponse

  def getIlluminatedCategory(categoryId: Int, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedCategoryResponse.Root] =
    getCategoryFull(categoryId, contextName).map { full ⇒
      val cat = IlluminatedCategory.illuminate(full.context, full.category, full.form, full.shadow)
      IlluminatedCategoryResponse.build(cat)
    }

  def getIlluminatedCategoryAtCommit(categoryId: Int, contextName: String, commitId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedCategoryResponse.Root] =
    for {
      context  ← * <~ contextByName(contextName)
      category ← * <~ categoryById(categoryId, context)
      commit ← * <~ ObjectCommits
                .filter(commit ⇒ commit.id === commitId && commit.formId === categoryId)
                .mustFindOneOr(CategoryNotFoundAtCommit(categoryId, commitId))
      categoryForm   ← * <~ ObjectForms.findById(commit.formId)
      categoryShadow ← * <~ ObjectShadows.findById(commit.shadowId)
    } yield
      IlluminatedCategoryResponse.build(
          IlluminatedCategory.illuminate(context, category, categoryForm, categoryShadow))

  private def updateCategoryHead(
      category: Category,
      categoryShadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Category] =
    maybeCommit match {
      case Some(commit) ⇒
        Categories
          .update(category, category.copy(shadowId = categoryShadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.pure(category)
    }

  private def contextByName(contextName: String)(implicit ec: EC): DbResultT[ObjectContext] =
    ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))

  private def categoryById(categoryId: Int, context: ObjectContext)(
      implicit ec: EC): DbResultT[Category] =
    Categories
      .withContextAndCategory(context.id, categoryId)
      .mustFindOneOr(CategoryNotFoundForContext(categoryId, context.id))

  private def getCategoryFull(categoryId: Int, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[CategoryFull] =
    for {
      context ← * <~ contextByName(contextName)
      result  ← * <~ getCategoryFull(categoryId, context)
    } yield result

  private def getCategoryFull(categoryId: Int, context: ObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[CategoryFull] =
    for {
      category ← * <~ categoryById(categoryId, context)
      form     ← * <~ ObjectForms.findById(category.formId)
      shadow   ← * <~ ObjectShadows.findById(category.shadowId)
    } yield CategoryFull(context, category, form, shadow)
}
