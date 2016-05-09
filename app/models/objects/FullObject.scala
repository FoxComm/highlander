package models.objects

case class FullObject[A](model: A, form: ObjectForm, shadow: ObjectShadow)
