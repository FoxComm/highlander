package services.objects

import failures._
import failures.ObjectFailures._
import models.objects._
import models.account.Scope
import payloads.ContextPayloads._
import payloads.GenericObjectPayloads._
import responses.ObjectResponses._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

object ObjectManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): DbResultT[ObjectFormResponse.Root] =
    for {
      form ← * <~ ObjectForms.mustFindById404(id)
    } yield ObjectFormResponse.build(form)

  def getShadow(shadowId: Int)(implicit ec: EC, db: DB): DbResultT[ObjectShadowResponse.Root] =
    for {
      shadow ← * <~ ObjectShadows.mustFindById404(shadowId)
    } yield ObjectShadowResponse.build(shadow)

  def getContextByName(name: String)(implicit ec: EC,
                                     db: DB): DbResultT[ObjectContextResponse.Root] =
    for {
      context ← * <~ mustFindByName404(name)
    } yield ObjectContextResponse.build(context)

  def createContext(payload: CreateObjectContext)(implicit ec: EC,
                                                  db: DB): DbResultT[ObjectContextResponse.Root] =
    for {
      context ← * <~ ObjectContexts.create(
                   ObjectContext(name = payload.name, attributes = payload.attributes))
    } yield ObjectContextResponse.build(context)

  def updateContextByName(name: String, payload: UpdateObjectContext)(
      implicit ec: EC,
      db: DB): DbResultT[ObjectContextResponse.Root] =
    for {
      context ← * <~ mustFindByName404(name)
      update ← * <~ ObjectContexts.update(
                  context,
                  context.copy(name = payload.name, attributes = payload.attributes))
    } yield ObjectContextResponse.build(update)

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectContext] =
    ObjectContexts.filterByName(name).mustFindOneOr(ObjectContextNotFound(name))

  def mustFindFormById404(id: Int)(implicit ec: EC): DbResultT[ObjectForm] =
    ObjectForms.findOneById(id).mustFindOr(NotFoundFailure404(ObjectForm, id))

  def mustFindShadowById404(id: Int)(implicit ec: EC): DbResultT[ObjectShadow] =
    ObjectShadows.findOneById(id).mustFindOr(NotFoundFailure404(ObjectShadow, id))

  def create(payload: CreateGenericObject, contextName: String)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[IlluminatedObjectResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      genericObject ← * <~ createInternal(payload, context)
    } yield
      IlluminatedObjectResponse.build(
          IlluminatedObject.illuminate(genericObject.form, genericObject.shadow))

  case class CreateInternalResult(genericObject: GenericObject,
                                  commit: ObjectCommit,
                                  form: ObjectForm,
                                  shadow: ObjectShadow)
      extends FormAndShadow {
    override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
      copy(form = form, shadow = shadow)
  }

  def createInternal(payload: CreateGenericObject, context: ObjectContext)(
      implicit ec: EC,
      au: AU): DbResultT[CreateInternalResult] = {
    val fs = FormAndShadow.fromPayload(payload.kind, payload.attributes)
    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(fs.form, fs.shadow, payload.schema)
      genericObject ← * <~ GenericObjects.create(
                         GenericObject(scope = scope,
                                       kind = payload.kind,
                                       contextId = context.id,
                                       formId = ins.form.id,
                                       shadowId = ins.shadow.id,
                                       commitId = ins.commit.id))
    } yield CreateInternalResult(genericObject, ins.commit, ins.form, ins.shadow)
  }

  def update(genericObjectId: Int, payload: UpdateGenericObject, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedObjectResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      genericObject ← * <~ updateInternal(genericObjectId, payload.attributes, context)
    } yield
      IlluminatedObjectResponse.build(
          IlluminatedObject.illuminate(genericObject.form, genericObject.shadow))

  case class UpdateInternalResult(oldGenericObject: GenericObject,
                                  genericObject: GenericObject,
                                  form: ObjectForm,
                                  shadow: ObjectShadow)
  def updateInternal(
      genericObjectId: Int,
      attributes: Map[String, Json],
      context: ObjectContext,
      forceUpdate: Boolean = false)(implicit ec: EC, db: DB): DbResultT[UpdateInternalResult] = {

    for {
      genericObject ← * <~ GenericObjects
                       .filter(_.contextId === context.id)
                       .filter(_.formId === genericObjectId)
                       .mustFindOneOr(ObjectNotFoundForContext(genericObjectId, context.name))
      fs = FormAndShadow.fromPayload(genericObject.kind, attributes)
      update ← * <~ ObjectUtils.update(genericObject.formId,
                                       genericObject.shadowId,
                                       fs.form.attributes,
                                       fs.shadow.attributes,
                                       forceUpdate)
      commit  ← * <~ ObjectUtils.commit(update)
      updated ← * <~ updateHead(genericObject, update.shadow, commit)
    } yield UpdateInternalResult(genericObject, updated, update.form, update.shadow)
  }

  def getIlluminated(id: Int, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedObjectResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      genericObject ← * <~ GenericObjects
                       .filter(_.contextId === context.id)
                       .filter(_.formId === id)
                       .mustFindOneOr(NotFoundFailure404(GenericObject, id))
      form   ← * <~ ObjectForms.mustFindById404(genericObject.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(genericObject.shadowId)
    } yield IlluminatedObjectResponse.build(IlluminatedObject.illuminate(form, shadow))

  private def updateHead(
      genericObject: GenericObject,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[GenericObject] =
    maybeCommit match {
      case Some(commit) ⇒
        GenericObjects
          .update(genericObject, genericObject.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(genericObject)
    }

  def getFullObject[T <: ObjectHead[T]](
      readHead: ⇒ DbResultT[T])(implicit ec: EC, db: DB): DbResultT[FullObject[T]] =
    for {
      modelHead ← * <~ readHead
      form      ← * <~ ObjectForms.mustFindById404(modelHead.formId)
      shadow    ← * <~ ObjectShadows.mustFindById404(modelHead.shadowId)
    } yield FullObject(modelHead, form, shadow)

  def getFullObjects[T <: ObjectHead[T]](models: Seq[T])(implicit ec: EC,
                                                         db: DB): DbResultT[Seq[FullObject[T]]] =
    for {
      forms   ← * <~ ObjectForms.findAllByIds(models.map(_.formId).toSet).result
      shadows ← * <~ ObjectShadows.findAllByIds(models.map(_.shadowId).toSet).result
      result  ← * <~ buildFullObjects(models, forms, shadows)
    } yield result

  private def buildFullObjects[T <: ObjectHead[T]](models: Seq[T],
                                                   forms: Seq[ObjectForm],
                                                   shadows: Seq[ObjectShadow])(implicit ec: EC) = {
    val formsMap   = forms.groupBy(_.id)
    val shadowsMap = shadows.groupBy(_.id)

    def getByIdOrFail[M](items: Map[Int, Traversable[M]], id: Int, failure: ⇒ Failures) =
      items.get(id).flatMap(_.headOption).toXor(failure)

    def buildFullObject(model: T) =
      for {
        form ← * <~ getByIdOrFail(formsMap,
                                  model.formId,
                                  ObjectForms.notFound404K(model.formId).single)

        shadow ← * <~ getByIdOrFail(shadowsMap,
                                    model.shadowId,
                                    ObjectShadows.notFound404K(model.shadowId).single)

      } yield FullObject(model, form, shadow)

    models.map(buildFullObject)
  }
}
