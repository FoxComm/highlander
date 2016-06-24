package services.category

import failures.CategoryFailures._
import failures.ObjectFailures._
import models.StoreAdmin
import models.category._
import models.objects._
import payloads.CategoryPayloads._
import responses.CategoryResponses._
import responses.ObjectResponses.ObjectContextResponse
import services.{LogActivity, Result}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object CategoryManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[CategoryFormResponse.Root] =
    (for {
      category ← * <~ Categories.filterByFormId(id).mustFindOneOr(CategoryFormNotFound(id))
      form     ← * <~ ObjectForms.mustFindById404(id)
    } yield CategoryFormResponse.build(category, form)).run()

  def getShadow(formId: Int, contextName: String)(implicit ec: EC,
                                                  db: DB): Result[CategoryShadowResponse.Root] =
    (for {
      context ← * <~ contextByName(contextName)
      category ← * <~ Categories
                  .withContextAndForm(context.id, formId)
                  .mustFindOneOr(CategoryNotFoundForContext(formId, context.id))
      shadow ← * <~ ObjectShadows.mustFindById404(category.shadowId)
    } yield CategoryShadowResponse.build(shadow)).run()

  def getCategory(categoryId: Int, contextName: String)(
      implicit ec: EC,
      db: DB): Result[FullCategoryResponse.Root] =
    getCategoryFull(categoryId, contextName)
      .map(c ⇒ FullCategoryResponse.build(c.category, c.form, c.shadow))
      .run()

  def createCategory(admin: StoreAdmin, payload: CreateFullCategory, contextName: String)(
      implicit ec: EC,
      db: DB,
      ac: AC): Result[FullCategoryResponse.Root] =
    (for {
      context  ← * <~ contextByName(contextName)
      form     ← * <~ ObjectForm(kind = Category.kind, attributes = payload.form.attributes)
      shadow   ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      insert   ← * <~ ObjectUtils.insert(form, shadow)
      category ← * <~ Categories.create(Category.build(context.id, insert))
      response = FullCategoryResponse.build(category, insert.form, insert.shadow)
      _ ← * <~ LogActivity.fullCategoryCreated(Some(admin),
                                               response,
                                               ObjectContextResponse.build(context))
    } yield response).runTxn()

  def updateCategory(
      admin: StoreAdmin,
      categoryId: Int,
      payload: UpdateFullCategory,
      contextName: String)(implicit ec: EC, db: DB, ac: AC): Result[FullCategoryResponse.Root] =
    (for {
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
    } yield categoryResponse).runTxn()

  def getIlluminatedCategory(categoryId: Int, contextName: String)(
      implicit ec: EC,
      db: DB): Result[IlluminatedCategoryResponse.Root] =
    getCategoryFull(categoryId, contextName).map { full ⇒
      val cat = IlluminatedCategory.illuminate(full.context, full.category, full.form, full.shadow)
      IlluminatedCategoryResponse.build(cat)
    }.run()

  def getIlluminatedCategoryAtCommit(categoryId: Int, contextName: String, commitId: Int)(
      implicit ec: EC,
      db: DB): Result[IlluminatedCategoryResponse.Root] =
    (for {
      context  ← * <~ contextByName(contextName)
      category ← * <~ categoryById(categoryId, context)
      commit ← * <~ ObjectCommits
                .filter(commit ⇒ commit.id === commitId && commit.formId === categoryId)
                .mustFindOneOr(CategoryNotFoundAtCommit(categoryId, commitId))
      categoryForm   ← * <~ ObjectForms.mustFindById404(commit.formId)
      categoryShadow ← * <~ ObjectShadows.mustFindById404(commit.shadowId)
    } yield
      IlluminatedCategoryResponse.build(
          IlluminatedCategory.illuminate(context, category, categoryForm, categoryShadow))).run()

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

  private def contextByName(contextName: String)(implicit ec: EC): DbResult[ObjectContext] =
    ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))

  private def categoryById(categoryId: Int, context: ObjectContext)(
      implicit ec: EC): DbResult[Category] =
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
      form     ← * <~ ObjectForms.mustFindById404(category.formId)
      shadow   ← * <~ ObjectShadows.mustFindById404(category.shadowId)
    } yield CategoryFull(context, category, form, shadow)
}
