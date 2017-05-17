package phoenix.models

import models.objects.{ObjectContext, ObjectForm, ObjectShadow}

package object category {

  case class CategoryFull(context: ObjectContext,
                          category: Category,
                          form: ObjectForm,
                          shadow: ObjectShadow)
}
