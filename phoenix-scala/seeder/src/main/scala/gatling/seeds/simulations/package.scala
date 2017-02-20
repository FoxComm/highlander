package gatling.seeds

import io.gatling.core.structure.{ChainBuilder, StructureBuilder}
import io.gatling.http.request.builder.HttpRequestBuilder
import io.gatling.core.Predef._

package object simulations {

  def step(chains: ChainBuilder*)    = exec(chains).stopOnFailure.doPause
  def step(http: HttpRequestBuilder) = exec(http).stopOnFailure.doPause

  implicit class Stepper[B <: StructureBuilder[B]](val builder: B) extends AnyVal {
    def step(chains: ChainBuilder*)    = builder.exec(chains).stopOnFailure.doPause
    def step(http: HttpRequestBuilder) = builder.exec(http).stopOnFailure.doPause
  }
}
