'use strict';

import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import CustomerConstants from '../constants/customers';
import { List, Map } from 'immutable';

class CustomerActions {
  insertCustomer(customer) {
    AshesDispatcher.handleAction({
      actionType: CustomerConstants.INSERT_CUSTOMERS,
      customer: customer
    });
  }

  updateCustomers(customers) {
    AshesDispatcher.handleAction({
      actionType: CustomerConstants.UPDATE_CUSTOMERS,
      customers: customers
    });
  }

  failedCustomers(errorMessage) {
    AshesDispatcher.handleAction({
      actionType: CustomerConstants.FAILED_CUSTOMERS,
      errorMessage: errorMessage
    });
  }

  fetchCustomers() {
    return Api.get('/customers')
      .then((customers) => {
        this.updateCustomers(List(customers));
      })
      .catch((err) => {
        this.failedCustomers(err);
      });
  }

  createCustomer(form) {
    return Api.submitForm(form)
      .then((customer) => {
        this.insertCustomer(customer);
      })
      .catch((err) => {
        this.failedCustomers(err);
      });
  }
}

export default new CustomerActions();
