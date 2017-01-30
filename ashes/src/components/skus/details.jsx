// @flow

// libs
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectDetails from 'components/object-page/object-details';
import InventoryAndTransactions from './inventory-and-transactions';
import ObjectFormFlat from '../object-form/object-form-flat';

import type { Fields, NodeDesc } from '../object-page/object-details';

const layouts = {
  details: require('./layouts/details.json'),
  inventory: require('./layouts/inventory.json'),
};

export default class SkuDetails extends ObjectDetails {
  get layout(): Object {
    return layouts[this.props.route.layout];
  }

  get attributes(): Object {
    return this.props.object;
  }

  @autobind
  handleObjChange(attributes: Object) {
    const { object } = this.props;
    const newObject = {
      ...object,
      ...attributes,
    };
    this.props.onUpdateObject(newObject);
  }

  renderFields(fields: Fields, section: Array<NodeDesc>): Element {
    const fieldsToRender = this.calcFieldsToRender(fields, section);
    return (
      <ObjectFormFlat
        canAddProperty={fields.canAddProperty}
        onChange={this.handleObjChange}
        fieldsToRender={fieldsToRender}
        attributes={this.attributes}
        schema={this.schema}
      />
    );
  }

  renderInventoryAndTransactions(): Element {
    return (
      <InventoryAndTransactions
        skuId={this.props.entity.entityId}
        // @TODO: get rid of passing skuCode here
        skuCode={this.attributes.code}
      />
    );
  }
}
