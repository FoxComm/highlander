/**
 * @flow
 */

// libs
import React, { Component } from 'react';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';

// components
import ObjectDetails from '../object-page/object-details';
import { Dropdown } from '../dropdown';
import { FormField } from '../forms';
import { BooleanOptions } from '../boolean-options/boolean-options';
import Counter from '../forms/counter';

const layout = require('./layout.json');

const SELECT_CLASS = [
  ['default', 'Default'],
  ['other', 'Other'],
];

export default class SkuDetails extends ObjectDetails {
  layout = layout;

  renderTaxClass() {
    const { taxClass } = this.props.object.attributes;

    return (
      <FormField
        ref="taxClassField"
        className="fc-object-form__field"
        label="Tax Class"
        getTargetValue={() => taxClass}
      >
        <div>
          <Dropdown
            placeholder="- Select -"
            value={taxClass}
            onChange={(v) => this.handleChange(v, 'taxClass', 'taxClassField')}
            items={SELECT_CLASS}
          />
        </div>
      </FormField>
    );
  }

  renderShippingClass() {
    const { shippingClass } = this.props.object.attributes;

    return (
      <FormField
        ref="shippingClassField"
        className="fc-object-form__field"
        label="Shipping Class"
        getTargetValue={() => shippingClass}
      >
        <div>
          <Dropdown
            placeholder="- Select -"
            value={shippingClass}
            onChange={(v) => this.handleChange(v, 'shippingClass', 'shippingClassField')}
            items={SELECT_CLASS}
          />
        </div>
      </FormField>
    );
  }

  @autobind
  handleChange(value: any, field: string|Array<string>, ref: string) {
    const attributes = assoc(this.props.object.attributes, field, value);
    const sku = assoc(this.props.object, 'attributes', attributes);

    this.props.onUpdateObject(sku);
    this.refs[ref].validate();
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
