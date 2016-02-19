package services

import responses.{CustomerResponse, TheResponse}

package object customers {
  type BulkCustomerUpdateResponse = TheResponse[Seq[CustomerResponse.Root]]
}