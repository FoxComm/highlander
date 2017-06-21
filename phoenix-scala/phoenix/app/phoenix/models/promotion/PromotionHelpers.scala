package phoenix.models.promotion

import objectframework.ObjectUtils
import objectframework.models._
import phoenix.utils.aliases._

object PromotionHelpers {

  def name(form: ObjectForm, shadow: ObjectShadow): Json =
    ObjectUtils.get("name", form, shadow)

  def description(form: ObjectForm, shadow: ObjectShadow): Json =
    ObjectUtils.get("description", form, shadow)
}
