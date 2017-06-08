package phoenix.models

import objectframework.models.{ObjectContext, ObjectForm, ObjectShadow}

package object category {

  case class CategoryFull(context: ObjectContext, category: Category, form: ObjectForm, shadow: ObjectShadow)
}
