package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints.admin._
import io.gatling.core.Predef._

class BatchActionsBenchSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  def appendCodes(session: Session): Session = {
    val heap = session("gcCodesHeap").as[Vector[String]]
    val currentOption = session("giftCardCodes").asOption[Vector[String]]
    val current = currentOption.fold(Vector[String]())(v ⇒ v)
    session.set("giftCardCodes", current ++ heap)
  }

  def takeCodesPortion(session: Session, count: Int): Session = {
    val portion = session("giftCardCodes").as[Vector[String]].take(count).map(code ⇒ s""""$code"""").mkString(", ")
    session.set("gcCodesPortion", portion).set("gcPortionCount", count.toString)
  }

  // Prepare scenario
  val benchScenario = scenario("Batch Actions Benchmark Scenario")
    .feed(jsonFile("data/admins.json").random)
    .repeat(25) { // 25 * 20 === 500
      exec(GiftCardEndpoint.bulkCreate()).exec(session ⇒ appendCodes(session))
    }
    .exec(session ⇒ {
      val count = session("giftCardCodes").as[Vector[String]].size
      println(s"Succesfully generated $count gift cards during simulation")
      session
    })
    .exitHereIfFailed
    // Benchmark 10 items processing
    .exec(session ⇒ takeCodesPortion(session, 10))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 25 items processing
    .exec(session ⇒ takeCodesPortion(session, 25))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 50 items processing
    .exec(session ⇒ takeCodesPortion(session, 50))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 100 items processing
    .exec(session ⇒ takeCodesPortion(session, 100))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 250 items processing
    .exec(session ⇒ takeCodesPortion(session, 250))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 500 items processing
    .exec(session ⇒ takeCodesPortion(session, 500))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
  /*
    // Benchmark 1000 items processing
    .exec(session ⇒ takeCodesPortion(session, 1000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 2000 items processing
    .exec(session ⇒ takeCodesPortion(session, 2000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 3000 items processing
    .exec(session ⇒ takeCodesPortion(session, 3000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 4000 items processing
    .exec(session ⇒ takeCodesPortion(session, 4000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 5000 items processing
    .exec(session ⇒ takeCodesPortion(session, 5000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 6000 items processing
    .exec(session ⇒ takeCodesPortion(session, 6000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
    // Benchmark 7000 items processing
    .exec(session ⇒ takeCodesPortion(session, 7000))
    .exec(GiftCardEndpoint.bulkWatch())
    .exitHereIfFailed
    .exec(GiftCardEndpoint.bulkUnwatch())
    .exitHereIfFailed
  */

  setUp(
    benchScenario.inject(atOnceUsers(1)).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
