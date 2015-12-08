import akka.http.scaladsl.model.StatusCodes
import cats.implicits._
import models.Customers
import models.StoreAdmins
import models.activity.Activities
import models.activity.Trails
import models.activity.Dimensions
import models.activity.Connections
import org.scalatest.mock.MockitoSugar
import payloads.AppendActivity
import responses.ActivityConnectionResponse
import services.{GeneralFailure, NotFoundFailure404}
import services.activity.CustomerInfoChanged
import util.IntegrationTestBase
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Money.Currency
import utils.Slick.implicits._
import utils.jdbc._
import utils.seeds.Seeds
import utils.seeds.Seeds.Factories
import utils.seeds.SeedsGenerator.generateCustomer
import utils.{Apis, CustomDirectives}
import slick.driver.PostgresDriver.api._
import util.SlickSupport.implicits._

import concurrent.ExecutionContext.Implicits.global

import Extensions._

class ActivityTrailIntegrationTest extends IntegrationTestBase
  with HttpSupport
  with AutomaticAuth
  with MockitoSugar {

  // paging and sorting API
  val uriPrefix = "v1/trails"
  val customerActivity = "customer_activity"
  "Activity Tests" - {
    "successfully creates activity after updating customer attributes" in new Fixture {

      //Update email, name, and phone number
      val payload = payloads.UpdateCustomerPayload(name = "Crazy Larry".some, email = "crazy.lary@crazy.com".some,
        phoneNumber = "666 666 6666".some)

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      //Check the activity log to see if it was created
      val activity = Activities.filterByData(
        CustomerInfoChanged.typeName, 
        "customerId", customer.id.toString).result.headOption.run().futureValue.value

      //make sure the activity has all the correct information
      activity.activityType must === (CustomerInfoChanged.typeName)

      val customerInfoChanged = activity.data.extract[CustomerInfoChanged]

      customerInfoChanged.customerId must === (customer.id)

      customerInfoChanged.oldInfo must === (
        payloads.UpdateCustomerPayload(
          name = customer.name,
          email = Some(customer.email),
          phoneNumber = customer.phoneNumber))

      customerInfoChanged.newInfo must === (payload)
    }
  }

  "PATCH /v1/trails/:dimensionName/:objectId" - {

    "successfully append one activity to new trail" in new Fixture {

      //Update email, name, and phone number. This should generate a CustomerInfoChanged 
      //activity
      val payload = payloads.UpdateCustomerPayload(
        name = "Updated Name".some, 
        email = "updated.name@name.com".some,
        phoneNumber = "666 666 6666".some)

      val response = PATCH(s"v1/customers/${customer.id}", payload)
      response.status must === (StatusCodes.OK)

      //Check the activity log to see if it was created
      val activity = Activities.filterByData(
        CustomerInfoChanged.typeName, 
        "customerId", customer.id.toString).result.headOption.run().futureValue.value

      //make sure the activity has all the correct information
      activity.activityType must === (CustomerInfoChanged.typeName)
      val customerInfoChanged = activity.data.extract[CustomerInfoChanged]

      
      //append the activity to the trail
      val appendPayload = AppendActivity(activity.id)
      val appendResponse = POST(s"v1/trails/${customerActivity}/${customer.id}", appendPayload)

      response.status must === (StatusCodes.OK)
      val appendedConnection = appendResponse.as[ActivityConnectionResponse.Root]

      appendedConnection.activityId must === (activity.id)
      appendedConnection.previousId must === (None)
      appendedConnection.nextId must === (None)

      //make sure the dimension was created
      val dimension = Dimensions.findByName(customerActivity).result.headOption.run().futureValue.value

      dimension.name must === (customerActivity)

      //make sure the trail was created
      val trail = Trails.findById(appendedConnection.trailId).result.headOption.run().futureValue.value

      //make sure things are linked up correctly
      trail.id must === (appendedConnection.trailId)
      trail.dimensionId must === (dimension.id)
      trail.tailConnectionId must === (Some(appendedConnection.id))

    }
  }

  trait Fixture {
    val (customer, admin) = (for {
      customer ← * <~ Customers.create(Factories.customer)
      admin ← * <~ StoreAdmins.create(authedStoreAdmin)
    } yield (customer, admin)).runT().futureValue.rightVal
  }
}
