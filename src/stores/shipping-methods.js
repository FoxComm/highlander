'use strict';

import Api from '../lib/api';
import BaseStore from '../lib/base-store';
import OrderStore from './orders';

class ShippingMethods extends BaseStore {
  uri(refNum) {
    return `/orders/${refNum}/shipping-methods`;
  }

  fetch(refNum) {
    let willAvialableMethods = super.fetch(refNum);
    let willOrderInfo = OrderStore.fetch(refNum);

    return Promise.all([willAvialableMethods, willOrderInfo])
      .then(([avialableMethods, order]) => {
        const allShippingMethods = [order.shippingMethod, ...avialableMethods];
        this.update(allShippingMethods);

        return allShippingMethods;
      });
  }
}

export default new ShippingMethods();
