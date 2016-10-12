package models.objects

import java.time.Instant

import cats.data.NonEmptyList
import cats.implicits._
import failures.Failure
import org.json4s.JsonAST.{JField, JNothing, JObject, JString}
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import utils.IlluminateAlgorithm
import services.objects.ObjectSchemasManager
import utils.aliases._
import utils.db._

object ObjectUtils {

  implicit class FormShadowTuple(pair: (ObjectForm, ObjectShadow)) extends FormAndShadow {
    override def form: ObjectForm     = pair._1
    override def shadow: ObjectShadow = pair._2
  }

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
        val keyMap  = m.map(_._1).reduceOption(_ ++ _).getOrElse(Map.empty)
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

  case class InsertResult(form: ObjectForm, shadow: ObjectShadow, commit: ObjectCommit)
      extends FormAndShadow

  def insert(formAndShadow: FormAndShadow)(implicit ec: EC): DbResultT[InsertResult] =
    insert(formAndShadow, None)

  def insert(formProto: ObjectForm, shadowProto: ObjectShadow)(
      implicit ec: EC): DbResultT[InsertResult] =
    insert(formProto, shadowProto, None)

  def insert(formAndShadow: FormAndShadow, schema: Option[String])(
      implicit ec: EC): DbResultT[InsertResult] =
    insert(formProto = formAndShadow.form, shadowProto = formAndShadow.shadow, schema = schema)

  def insert(formProto: ObjectForm, shadowProto: ObjectShadow, schema: Option[String])(
      implicit ec: EC): DbResultT[InsertResult] = {
    val n = ObjectUtils.newFormAndShadow(formProto.attributes, shadowProto.attributes)

    for {
      optSchema ← * <~ ObjectSchemasManager.getSchemaByOptNameOrKind(schema, formProto.kind)
      form      ← * <~ ObjectForms.create(formProto.copy(attributes = n.form))
      shadow ← * <~ ObjectShadows.create(
                  shadowProto
                    .copy(formId = form.id, attributes = n.shadow, schemaId = optSchema.map(_.id)))
      _ ← * <~ failIfErrors(
             IlluminateAlgorithm.validateAttributesTypes(form.attributes, shadow.attributes))
      //Make sure form is correct and shadow links are correct
      _ ← * <~ optSchema.map { schema ⇒
           IlluminateAlgorithm.validateObjectBySchema(schema, form, shadow)
         }
      commit ← * <~ ObjectCommits.create(ObjectCommit(formId = form.id, shadowId = shadow.id))
    } yield InsertResult(form, shadow, commit)
  }

  def insertFullObject[H <: ObjectHead[H]](
      proto: FormAndShadow,
      updateHead: (InsertResult) ⇒ DbResultT[H])(implicit ec: EC): DbResultT[FullObject[H]] =
    for {
      insert ← * <~ insert(proto.form, proto.shadow, None)
      head   ← * <~ updateHead(insert)
    } yield FullObject[H](head, insert.form, insert.shadow)

  case class UpdateResult(form: ObjectForm, shadow: ObjectShadow, updated: Boolean)
      extends FormAndShadow

  def commitUpdate[T <: ObjectHead[T]](
      fullObject: FullObject[T],
      formAttributes: Json,
      shadowAttributes: Json,
      updateHead: (FullObject[T], Int) ⇒ DbResultT[FullObject[T]],
      force: Boolean = false)(implicit db: DB, ec: EC): DbResultT[FullObject[T]] =
    for {
      updateResult ← * <~ updateFormAndShadow(fullObject, formAttributes, shadowAttributes, force)
      maybeCommit  ← * <~ ObjectUtils.commit(updateResult)
      committedObject ← * <~ (maybeCommit match {
                             case Some(commit) ⇒
                               val newObject = fullObject
                                 .copy[T](form = updateResult.form, shadow = updateResult.shadow)
                               updateHead(newObject, commit.id)
                             case _ ⇒
                               DbResultT.good(fullObject)
                           })
    } yield committedObject

  def updateFormAndShadow(
      oldFormAndShadow: FormAndShadow,
      formAttributes: Json,
      shadowAttributes: Json,
      force: Boolean = false)(implicit db: DB, ec: EC): DbResultT[UpdateResult] = {
    for {
      newAttributes ← * <~ ObjectUtils.updateFormAndShadow(oldFormAndShadow.form.attributes,
                                                           formAttributes,
                                                           shadowAttributes)
      result ← * <~ updateIfDifferent(oldFormAndShadow,
                                      newAttributes.form,
                                      newAttributes.shadow,
                                      force)
    } yield result
  }

  def update(formId: Int,
             shadowId: Int,
             formAttributes: Json,
             shadowAttributes: Json,
             force: Boolean = false)(implicit db: DB, ec: EC): DbResultT[UpdateResult] =
    for {
      oldForm   ← * <~ ObjectForms.mustFindById404(formId)
      oldShadow ← * <~ ObjectShadows.mustFindById404(shadowId)
      result ← * <~ updateFormAndShadow((oldForm, oldShadow),
                                        formAttributes,
                                        shadowAttributes,
                                        force)
    } yield result

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

  private def updateIfDifferent(
      old: FormAndShadow,
      newFormAttributes: Json,
      newShadowAttributes: Json,
      force: Boolean = false)(implicit ec: EC, db: DB): DbResultT[UpdateResult] = {
    if (old.shadow.attributes != newShadowAttributes || force)
      for {
        form ← * <~ ObjectForms.update(
                  old.form,
                  old.form.copy(attributes = newFormAttributes, updatedAt = Instant.now))
        shadow ← * <~ ObjectShadows.create(
                    ObjectShadow(formId = form.id,
                                 attributes = newShadowAttributes,
                                 schemaId = old.shadow.schemaId))
        // TODO: add schema validation here
//        optSchema ← * <~ ObjectFullSchemas.findOneById(old.shadow.schemaId)
//        _ ← * <~ optSchema.map { schema ⇒
//             IlluminateAlgorithm.validateObjectBySchema(schema, form, shadow)
//           }
        _ ← * <~ validateShadow(form, shadow)
      } yield UpdateResult(form, shadow, updated = true)
    else DbResultT.pure(UpdateResult(old.form, old.shadow, updated = false))
  }

  private def validateShadow(form: ObjectForm, shadow: ObjectShadow)(
      implicit ec: EC): DbResultT[Unit] =
    failIfErrors(IlluminateAlgorithm.validateAttributes(form.attributes, shadow.attributes))

  def failIfErrors(errors: Seq[Failure])(implicit ec: EC): DbResultT[Unit] = errors match {
    case head :: tail ⇒ DbResultT.failures(NonEmptyList(head, tail))
    case Nil          ⇒ DbResultT.pure(Unit)
  }

  def getFullObject[T <: ObjectHead[T]](
      readHead: ⇒ DbResultT[T])(implicit ec: EC, db: DB): DbResultT[FullObject[T]] =
    for {
      head   ← * <~ readHead
      form   ← * <~ ObjectForms.mustFindById404(head.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(head.shadowId)
    } yield FullObject(head, form, shadow)
}
