package responses

import services.Failures

final case class TheResponse[A](
  result  : A,
  alerts  : Option[List[String]] = None,
  errors  : Option[List[String]] = None,
  warnings: Option[List[String]] = None)

object TheResponse {

  def build[A](value: A, alerts: Option[Failures] = None, errors: Option[Failures] = None,
    warnings: Option[Failures] = None): TheResponse[A] =
    TheResponse(result   = value,
                alerts   = alerts.map(_.flatten),
                errors   = errors.map(_.flatten),
                warnings = warnings.map(_.flatten))
}
