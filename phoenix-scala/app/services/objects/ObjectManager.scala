package services.objects

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.objects._
import payloads.ContextPayloads._
import responses.ObjectResponses._
import utils.aliases._
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
      update ← * <~ ObjectContexts
        .update(context, context.copy(name = payload.name, attributes = payload.attributes))
    } yield ObjectContextResponse.build(update)

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectContext] =
    ObjectContexts.filterByName(name).mustFindOneOr(ObjectContextNotFound(name))

  def mustFindFormById404(id: Int)(implicit ec: EC): DbResultT[ObjectForm] =
    ObjectForms.findOneById(id).mustFindOr(NotFoundFailure404(ObjectForm, id))

  def mustFindShadowById404(id: Int)(implicit ec: EC): DbResultT[ObjectShadow] =
    ObjectShadows.findOneById(id).mustFindOr(NotFoundFailure404(ObjectShadow, id))

  def getFullObject[T <: ObjectHead[T]](
      readHead: ⇒ DbResultT[T])(implicit ec: EC, db: DB): DbResultT[FullObject[T]] =
    for {
      modelHead ← * <~ readHead
      form      ← * <~ ObjectForms.mustFindById404(modelHead.formId)
      shadow    ← * <~ ObjectShadows.mustFindById404(modelHead.shadowId)
    } yield FullObject(modelHead, form, shadow)
}
