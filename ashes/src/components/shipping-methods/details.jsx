/**
 * @flow
 */

// libs
import _ from 'lodash';
import React, { Component, Element } from 'react';

// actions
import * as ShippingMethodActions from 'modules/shipping-methods/details';

// components
import { connectPage, ObjectPage } from '../object-page/object-page';

// type
import type { ShippingMethod, CreatePayload, UpdatePayload } from 'paragons/shipping-method';

type Props = {
  actions: {
    fetchShippingMethod: (id: string) => Promise,
    createShippingMethod: (payload: CreatePayload) => Promise,
    updateShippingMethod: (payload: UpdatePayload) => Promise,
    archiveShippingMethod: (id: string) => Promise,
  },
  details: {
    shippingMethod: ?ShippingMethod,
  },
};

class ShippingMethodDetails extends ObjectPage {
  props: Props;

  get pageTitle(): string {
    if (this.isNew) {
      return 'New Shipping Method';
    }

    return _.get(this.props.details.shippingMethod, 'attributes.code', '');
  }

  createEntity(entity) {
    console.log('Creating');
  }

  updateEntity(entity) {
    console.log('Updating');
  }

  archiveEntity(entity) {
    console.log('Deleting');
  }
}

export default connectPage('shippingMethod', ShippingMethodActions)(ShippingMethodDetails);
