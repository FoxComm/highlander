package models.objects

trait FormAndShadow {
  def form: ObjectForm
  def shadow: ObjectShadow

  def tupled = form → shadow
}

case class FullObject[A](model: A, form: ObjectForm, shadow: ObjectShadow) extends FormAndShadow
