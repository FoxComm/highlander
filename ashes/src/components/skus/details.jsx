/**
 * @flow
 */

// libs
import React, { Element } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectDetails from 'components/object-page/object-details';
import InventoryAndTransactions from './inventory-and-transactions';
import ObjectFormFlat from '../object-form/object-form-flat';

import type { Fields, NodeDesc } from '../object-form/object-form-inner';

const layout = require('./layout.json');

export default class SkuDetails extends ObjectDetails {
  layout = layout;

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

  renderInventoryAndTransactions() {
    return (
      <InventoryAndTransactions skuId={this.props.entity.entityId} />
    );
  }
}
