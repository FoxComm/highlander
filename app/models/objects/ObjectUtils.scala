package models.objects

import java.time.Instant

import cats.data.NonEmptyList
import cats.implicits._
import failures.Failure
import failures.ObjectFailures._
import org.json4s.JsonAST.{JField, JNothing, JObject, JString}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import slick.driver.PostgresDriver.api._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.db._

object ObjectUtils {

  def get(attr: String, form: ObjectForm, shadow: ObjectShadow): Json = {
    IlluminateAlgorithm.get(attr, form.attributes, shadow.attributes)
  }

  def key(content: Json): String = {
    val KEY_LENGTH = 5
    val md         = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(compact(render(content)).getBytes)
      .slice(0, KEY_LENGTH)
      .map("%02x".format(_))
      .mkString
  }

  def key(content: String): String = key(JString(content))

  def attribute(content: Json): JField = {
    (key(content), content)
  }

  def attributes(values: Seq[Json]): Json =
    JObject(values.map(attribute).toList)

  type KeyMap = Map[String, String]
  def createForm(form: Json): (KeyMap, Json) = {
    form match {
      case JObject(o) ⇒
        val m = o.obj.map {
          case (attr, value) ⇒
            val k = key(value)
            (Map(attr → k), (k, value))
        }
        val keyMap  = m.map(_._1).reduce(_ ++ _)
        val newForm = JObject(m.map(_._2).toList.distinct)
        (keyMap, newForm)
      case _ ⇒
        (Map(), JNothing)
    }
  }

  def updateForm(oldForm: Json, updatedForm: Json): (KeyMap, Json) = {
    val (keyMap, newForm) = createForm(updatedForm)
    (keyMap, oldForm.merge(newForm))
  }

  def newShadow(oldShadow: Json, keyMap: KeyMap): Json =
    oldShadow match {
      case JObject(o) ⇒
        o.obj.map {
          case (key, value) ⇒
            val t = value \ "type"
            val ref = value \ "ref" match {
              case JString(s) ⇒ s
              case _          ⇒ key
            }
            (key, ("type" → t) ~ ("ref" → keyMap.getOrElse(ref, ref)))
        }
      case _ ⇒ JNothing
    }

  case class FormShadowAttributes(form: Json, shadow: Json)
  def updateFormAndShadow(oldForm: Json, newForm: Json, oldShadow: Json): FormShadowAttributes = {
    val (keyMap, updatedForm) = updateForm(oldForm, newForm)
    val updatedShadow         = newShadow(oldShadow, keyMap)
    FormShadowAttributes(updatedForm, updatedShadow)
  }

  def newFormAndShadow(oldForm: Json, oldShadow: Json): FormShadowAttributes = {
    val (keyMap, form) = createForm(oldForm)
    val shadow         = newShadow(oldShadow, keyMap)
    FormShadowAttributes(form, shadow)
  }

  def bakedAttrToFormShadow(attr: String, value: Json): ((String, Json), (String, Json)) = {
    val t = value \ "t"
    val v = value \ "v"
    t match {
      case JString(kind) ⇒
        val k = key(v)
        ((k, v), (attr, ("type" → kind) ~ ("ref" → k)))
      case _ ⇒ ((attr, JNothing), (attr, JNothing))
    }
  }

  def bakedToFormShadow(baked: Json): (Json, Json) =
    baked match {
      case JObject(b) ⇒
        val formShadowPairs = b.obj.map {
          case (attr, obj) ⇒ bakedAttrToFormShadow(attr, obj)
        }

        val form   = JObject(formShadowPairs.map(_._1).toList)
        val shadow = JObject(formShadowPairs.map(_._2).toList)
        (form, shadow)
      case _ ⇒ (JNothing, JNothing)
    }

  case class InsertResult(form: ObjectForm, shadow: ObjectShadow, commit: ObjectCommit)

  def insert(formProto: ObjectForm, shadowProto: ObjectShadow)(
      implicit ec: EC): DbResultT[InsertResult] = {
    val n = ObjectUtils.newFormAndShadow(formProto.attributes, shadowProto.attributes)

    for {
      //Make sure form is correct and shadow links are correct
      form   ← * <~ ObjectForms.create(formProto.copy(attributes = n.form))
      shadow ← * <~ ObjectShadows.create(shadowProto.copy(formId = form.id, attributes = n.shadow))
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
    } yield InsertResult(form, shadow, commit)
  }

  case class UpdateResult(form: ObjectForm, shadow: ObjectShadow, updated: Boolean)

  def update(formId: Int,
             shadowId: Int,
             formAttributes: Json,
             shadowAttributes: Json,
             force: Boolean = false)(implicit db: DB, ec: EC): DbResultT[UpdateResult] = {
    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(shadowId)
      newAttributes ← * <~ ObjectUtils
                       .updateFormAndShadow(oldForm.attributes, formAttributes, shadowAttributes)
      result ← * <~ updateIfDifferent(oldForm,
                                      oldShadow,
                                      newAttributes.form,
                                      newAttributes.shadow,
                                      force)
    } yield result
  }

  def commit(u: UpdateResult)(implicit ec: EC): DbResultT[Option[ObjectCommit]] =
    commit(u.form, u.shadow, u.updated)

  def commit(form: ObjectForm, shadow: ObjectShadow, doIt: Boolean)(
      implicit ec: EC): DbResultT[Option[ObjectCommit]] = {
    if (doIt)
      for {
        commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
      } yield commit.some
    else DbResultT.pure(None)
  }

  def updateLink(oldLeftId: Int,
                 leftId: Int,
                 oldRightId: Int,
                 rightId: Int,
                 linkType: ObjectLink.LinkType)(implicit ec: EC): DbResultT[Unit] =
    //Create a new link a product changes.
    if (oldLeftId != leftId)
      for {
        _ ← * <~ ObjectLinks.create(
               ObjectLink(leftId = leftId, rightId = rightId, linkType = linkType))
      } yield Unit
    //If the product didn't change but the sku changed, update the link
    //This is because we never want two skus of the same type pointing to
    //the same sku shadow.
    else if (oldRightId != rightId)
      for {
        link ← * <~ ObjectLinks
                .findByLeftRight(leftId, oldRightId)
                .mustFindOneOr(ObjectLinkCannotBeFound(leftId, oldRightId))
        _ ← * <~ ObjectLinks.update(link, link.copy(leftId = leftId, rightId = rightId))
      } yield Unit
    //otherwise nothing changed so do nothing.
    else DbResultT.pure(Unit)

  case class Child(form: ObjectForm, shadow: ObjectShadow)

  def getChildren(leftId: Int, linkType: ObjectLink.LinkType)(
      implicit ec: EC): DbResultT[Seq[Child]] =
    for {
      links ← * <~ ObjectLinks.findByLeftAndType(leftId, linkType).result
      shadowIds = links.map(_.rightId)
      shadows ← * <~ ObjectShadows.filter(_.id.inSet(shadowIds)).result
      formIds = shadows.map(_.formId)
      forms  ← * <~ ObjectForms.filter(_.id.inSet(formIds)).result
      pairs  ← * <~ forms.sortBy(_.id).zip(shadows.sortBy(_.formId))
      result ← * <~ pairs.map { case (form, shadow) ⇒ Child(form, shadow) }
    } yield result

  private def updateIfDifferent(
      oldForm: ObjectForm,
      oldShadow: ObjectShadow,
      newFormAttributes: Json,
      newShadowAttributes: Json,
      force: Boolean = false)(implicit ec: EC): DbResultT[UpdateResult] = {
    if (oldShadow.attributes != newShadowAttributes || force)
      for {
        form ← * <~ ObjectForms.update(
                  oldForm,
                  oldForm.copy(attributes = newFormAttributes, updatedAt = Instant.now))
        shadow ← * <~ ObjectShadows.create(
                    ObjectShadow(formId = form.id, attributes = newShadowAttributes))
        _ ← * <~ validateShadow(form, shadow)
      } yield UpdateResult(form, shadow, updated = true)
    else DbResultT.pure(UpdateResult(oldForm, oldShadow, updated = false))
  }

  private def validateShadow(form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Unit] =
    failIfErrors(IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes))

  def failIfErrors(errors: Seq[Failure])(implicit ec: EC): DbResultT[Unit] = errors match {
    case head :: tail ⇒ DbResultT.failures(NonEmptyList(head, tail))
    case Nil          ⇒ DbResultT.pure(Unit)
  }

  def updateAssociatedLefts[M <: ObjectHead[M], T <: ObjectHeads[M]](
      Left: FoxTableQuery[M, T],
      contextId: Int,
      oldRightId: Int,
      newRightId: Int,
      linkType: ObjectLink.LinkType)(implicit ec: EC, db: DB): DbResultT[Seq[ObjectLink]] =
    for {
      links ← * <~ ObjectLinks.findByRightAndType(oldRightId, linkType).result
      upLinks ← * <~ links.map { link ⇒
                 for {
                   shadow    ← * <~ ObjectShadows.mustFindById404(link.leftId)
                   newShadow ← * <~ ObjectShadows.create(shadow.copy(id = 0))
                   optModel ← * <~ Left
                               .filter(_.formId === shadow.formId)
                               .filter(_.contextId === contextId)
                               .one
                   newLink ← * <~ updateLeftLinkIfObject(optModel,
                                                         Left,
                                                         newShadow.id,
                                                         newRightId,
                                                         linkType)
                 } yield newLink.getOrElse(link)
               }
    } yield upLinks

  def updateAssociatedRights[M <: ObjectHead[M], T <: ObjectHeads[M]](
      Right: FoxTableQuery[M, T],
      oldLinks: Seq[ObjectLink],
      newLeftId: Int)(implicit ec: EC, db: DB): DbResultT[Seq[ObjectLink]] =
    DbResultT.sequence(oldLinks.map(link ⇒ updateAssociatedRight(Right, link, newLeftId)))

  def updateAssociatedRight[M <: ObjectHead[M], T <: ObjectHeads[M]](
      Right: FoxTableQuery[M, T],
      oldLink: ObjectLink,
      newLeftId: Int)(implicit ec: EC, db: DB): DbResultT[ObjectLink] =
    for {
      shadow    ← * <~ ObjectShadows.mustFindById404(oldLink.rightId)
      newShadow ← * <~ ObjectShadows.create(shadow.copy(id = 0))
      optModel  ← * <~ Right.filter(_.shadowId === oldLink.rightId).one
      newLink ← * <~ updateRightLinkIfObject(optModel,
                                             Right,
                                             newLeftId,
                                             newShadow.id,
                                             oldLink.linkType)
    } yield newLink.getOrElse(oldLink)

  def updateLeftLinkIfObject[M <: ObjectHead[M], T <: ObjectHeads[M]](
      maybe: Option[M],
      table: FoxTableQuery[M, T],
      newShadowId: Int,
      newRightId: Int,
      linkType: ObjectLink.LinkType)(implicit ec: EC, db: DB): DbResultT[Option[ObjectLink]] = {

    maybe match {
      case Some(model) ⇒
        for {
          commit ← * <~ ObjectCommits.create(
                      ObjectCommit(formId = model.formId, shadowId = newShadowId))
          update ← * <~ table.update(model, model.withNewShadowAndCommit(newShadowId, commit.id))
          link ← * <~ ObjectLinks.create(
                    ObjectLink(leftId = update.shadowId,
                               rightId = newRightId,
                               linkType = linkType))
        } yield link.some
      case None ⇒
        DbResultT.pure(None)
    }
  }

  private def updateRightLinkIfObject[M <: ObjectHead[M], T <: ObjectHeads[M]](
      maybe: Option[M],
      table: FoxTableQuery[M, T],
      newLeftId: Int,
      newShadowId: Int,
      linkType: ObjectLink.LinkType)(implicit ec: EC, db: DB): DbResultT[Option[ObjectLink]] = {

    maybe match {
      case Some(model) ⇒
        for {
          commit ← * <~ ObjectCommits.create(
                      ObjectCommit(formId = model.formId, shadowId = newShadowId))
          update ← * <~ table.update(model, model.withNewShadowAndCommit(newShadowId, commit.id))
          link ← * <~ ObjectLinks.create(
                    ObjectLink(leftId = newLeftId, rightId = update.shadowId, linkType = linkType))
        } yield link.some
      case None ⇒
        DbResultT.pure(None)
    }
  }
}
