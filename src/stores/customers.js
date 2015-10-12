'use strict';

import { List } from 'immutable';
import BaseStore from './base-store';
import CustomerConstants from '../constants/customers';

class CustomerStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-customers';
    this.state = List([]);

    this.bindListener(CustomerConstants.UPDATE_CUSTOMERS, this.handleUpdateCustomers);
    this.bindListener(CustomerConstants.FAILED_CUSTOMERS, this.handleFailedCustomers);
  }

  handleUpdateCustomers(action) {
    this.setState(List(action.customers));
  }

  handleFailedCustomers(action) {
    console.log(action.errorMessage.data.errors);
  }
}

let customerStore = new CustomerStore();
export default customerStore;
