/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectDetails from 'components/object-page/object-details';
import { FormField } from 'components/forms';
import { BooleanOptions } from 'components/boolean-options/boolean-options';
import Counter from 'components/forms/counter';
import InventoryAndTransactions from './inventory-txn-container';

const layout = require('./layout.json');

export default class SkuInventoryDetails extends ObjectDetails {
  layout = layout;

  @autobind
  handleChange(value: any, field: string|Array<string>, ref: string) {
    const attributes = assoc(this.props.object.attributes, field, value);
    const sku = assoc(this.props.object, 'attributes', attributes);

    this.props.onUpdateObject(sku);
    this.refs[ref].validate();
  }

  renderInventoryAndTransactons() {
    return (
      <InventoryAndTransactions />
    );
  }

  renderQuantityInCart(field: string, ref: string, label: string) {
    const { isEnabled, level } = this.props.object.attributes[field];

    const counter = (
      <Counter
        id={field}
        name={field}
        value={level}
        decreaseAction={() => this.handleChange(level - 1, [field, 'level'], ref)}
        increaseAction={() => this.handleChange(level + 1, [field, 'level'], ref)}
        onChange={({target}) => this.handleChange(target.value, [field, 'level'], ref)}
        min={1}
      />
    );

    return (
      <FormField
        ref={ref}
        className="fc-object-form__field"
        label={label}
      >
        <div>
          <BooleanOptions
            value={isEnabled}
            onChange={v => this.handleChange(v, [field, 'isEnabled'], ref)}
          />
          {isEnabled && counter}
        </div>
      </FormField>
    );
  }

  renderMaximumQuantityInCart() {
    return this.renderQuantityInCart(
      'maximumQuantityInCart',
      'maximumQuantityField',
      'Maximum quantity allowed in shopping cart'
    );
  }

  renderMinimumQuantityInCart() {
    return this.renderQuantityInCart(
      'minimumQuantityInCart',
      'minimumQuantityField',
      'Minimum quantity allowed in shopping cart'
    );
  }

  renderInventoryWarningLevel() {
    const { isEnabled, level } = this.props.object.attributes.inventoryWarningLevel;

    return (
      <FormField
        ref="inventoryWarningField"
        className="fc-object-form__field"
        label="Low inventory warning threshold"
      >
        <div>
          <BooleanOptions
            value={isEnabled}
            onChange={v => this.handleChange(v, ['inventoryWarningLevel', 'isEnabled'], 'inventoryWarningField')}
          />
          <input
            type="text"
            id="inventoryWarningLevel"
            name="inventoryWarningLevel"
            value={level}
            onChange={({target}) => {
              this.handleChange(target.value, ['inventoryWarningLevel', 'level'], 'inventoryWarningField');
            }}
            disabled={!isEnabled}
          />
          <label htmlFor="inventoryWarningLevel">AFS</label>
        </div>
      </FormField>
    );
  }
}
