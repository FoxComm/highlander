/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import ObjectDetails from '../object-page/object-details';
import { Dropdown } from '../dropdown';
import { FormField } from '../forms';
const layout = require('./layout.json');

const SELECT_TAX_CLASS = [
  ['default', 'Default'],
];

export default class SkuDetails extends ObjectDetails {
  layout = layout;

  renderTaxClass() {
    const sku = this.props.object;

    return (
      <FormField
        ref="taxClassField"
        className="fc-object-form__field"
        label="Tax Class"
        getTargetValue={() => sku.taxClass}
      >
        <div>
          <Dropdown
            placeholder="- Select -"
            value={sku.taxClass}
            onChange={this.handleTaxClassChange}
            items={SELECT_TAX_CLASS}
          />
        </div>
      </FormField>
    );
  }

  @autobind
  handleTaxClassChange(value: any) {
    const newSku = assoc(this.props.object, 'taxClass', value);

    this.props.onUpdateObject(newSku);
    this.refs.taxClassField.validate();
  }
}
