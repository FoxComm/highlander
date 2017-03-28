
// @class Customers
// Accessible via [customers](#foxapi-customers) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Customers {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method one(customerId: Number): Promise<CustomerResponse>
   * Find customer by id.
   */
  one(customerId) {
    return this.api.get(endpoints.customer(customerId));
  }

  /**
   * @method create(customer: CustomerCreatePayload): Promise<CustomerResponse>
   * Create new customer.
   */
  create(customer) {
    return this.api.post(endpoints.customers, customer);
  }

  /**
   * @method update(customerId: Number, customer: CustomerUpdatePayload): Promise<CustomerResponse>
   * Update customer details.
   */
  update(customerId, customer) {
    return this.api.patch(endpoints.customer(customerId), customer);
  }

  /**
   * @method issueStoreCredit(customerId: Number, credit: StoreCreditCreateSinglePayload): Promise<StoreCredit>
   * Issue store credit for a customer.
   */
  issueStoreCredit(customerId, credit) {
    return this.api.post(endpoints.customerStoreCredit(customerId), credit);
  }
}
