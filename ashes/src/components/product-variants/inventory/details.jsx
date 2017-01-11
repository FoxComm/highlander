/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectDetails from 'components/object-page/object-details';
import InventoryAndTransactions from './inventory-txn-container';

const layout = require('./layout.json');

export default class SkuInventoryDetails extends ObjectDetails {
  layout = layout;

  @autobind
  handleChange123(value: any, field: string|Array<string>, ref: string) {
    const attributes = assoc(this.props.object.attributes, field, value);
    const sku = assoc(this.props.object, 'attributes', attributes);

    this.props.onUpdateObject(sku);
    this.refs[ref].validate();
  }

  renderInventoryAndTransactions() {
    return <div>hello kitty</div>;

    /*return (
      <InventoryAndTransactions skuId={this.props.entity.entityId} />
    );*/
  }
}
