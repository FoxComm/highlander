package com.foxcommerce

import com.foxcommerce.common._
import com.foxcommerce.endpoints.AuthEndpoint
import com.foxcommerce.endpoints.storefront._
import com.foxcommerce.fixtures._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

class StorefrontAccessSimulation extends Simulation {

  val conf = Config.load()

  before {
    conf.before()
  }

  // Fixtures
  val baseAddress = AddressFixture(name = Utils.randomString(), regionId = 1, address1 = "Donkey street, 38",
    address2 = "Donkey street, 39", city = "Donkeyville", zip = "55555")
  val address = baseAddress.copy(city = "Seattle", zip = "66666")
  
  // Prepare scenario
  val storefrontScenario = scenario("Storefront Access Scenario")
    .feed(jsonFile("data/customers.json").random)
    // Customer Init and Intruder Registration
    .exec(session ⇒ {
      session
        .set("intruderEmail", Utils.randomEmail("intruder"))
        .set("intruderPassword", Utils.randomString())
    })
    // Intruder registration and login, customer login
    .exec(IntruderActivity.register())
    .exec(AuthEndpoint.loginAsIntruder())
    .exec(AuthEndpoint.loginAsCustomer())
    .exec(flushCookieJar)
    .exitHereIfFailed
    // Customer Address Activity
    .exec(CustomerActivity.asCustomer())
    .exec(AddressEndpoint.create(baseAddress))
    .exec(AddressEndpoint.update(address))
    .exec(AddressEndpoint.get(address))
    .exec(AddressEndpoint.setAsDefault(address))
    .exitHereIfFailed
    // Intruder Address Activity
    .exec(IntruderActivity.asIntruder())
    .exec(IntruderActivity.Address.get())
    .exec(IntruderActivity.Address.update(address))
    .exec(IntruderActivity.Address.setAsDefault())
    .exec(IntruderActivity.Address.delete())
    .exec(IntruderActivity.Cart.shippingAddressAdd())
    // Cleanup
    .exec(CustomerActivity.asCustomer())
    .exec(AddressEndpoint.removeDefault())
    .exec(AddressEndpoint.delete())

  setUp(
    storefrontScenario.inject(conf.defaultInjectionProfile).protocols(conf.httpConf)
  ).assertions(conf.defaultAssertion)
}
