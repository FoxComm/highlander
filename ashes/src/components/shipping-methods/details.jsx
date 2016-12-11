/**
 * @flow
 */

// libs
import React, { Component, Element } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

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

  @autobind
  createEntity(entity) {
    // Strip attributes out because server doesn't use them.
    this.props.actions.createShippingMethod(entity.attributes);
  }

  @autobind
  updateEntity(entity) {
    // Strip attributes out because server doesn't use them.
    this.props.actions.updateShippingMethod(entity.id, entity.attributes);
  }

  @autobind
  archiveEntity() {
    this.props.actions.archiveShippingMethod(this.entityId);
  }
}

export default connectPage('shippingMethod', ShippingMethodActions)(ShippingMethodDetails);
