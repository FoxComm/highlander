/* @flow */

import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';

import type { Props as GenericProps, ValueType, DropdownItemType } from './generic-dropdown';

type Props = GenericProps;

const omitProps = [
  'items',
  'emptyMessage',
  'renderDropdownInput',
  'renderNullTitle',
  'renderPrepend',
  'onChange'
];

const mapValues = (items: Array<DropdownItemType>): Array<ValueType> => items.map(([value]) => value);

export default class Dropdown extends Component {
  props: Props;

  @autobind
  buildInput(value: string|number, title: string, props: Props, handleToggleClick: Function) {
    if (value === null) value = '';
    if (props.editable) {
      return (
        <div className="fc-dropdown__value">
          <input
            name={props.name}
            placeholder={props.placeholder}
            disabled={props.disabled}
            defaultValue={title}
            key={`${props.name}-${value}-selected`}
          />
        </div>
      );
    }

    return (
      <div id={props.dropdownValueId} className="fc-dropdown__value" onClick={handleToggleClick}>
        {title}
        <input name={props.name} type="hidden" value={value} readOnly />
      </div>
    );
  }


  renderItems() {
    const { name, items } = this.props;

      return _.map(items, ([value, title, isHidden]) => (
        <DropdownItem value={value} key={`${name}-${value}`} isHidden={isHidden}>
          {title}
        </DropdownItem>
      ));
  }

  shouldComponentUpdate(nextProps: Props): boolean {
    const oldItems = _.get(this.props, 'items', []);
    const newItems = _.get(nextProps, 'items', []);

    // Not items (still)
    if (!oldItems && !newItems) {
      return false;
    }

    // Items became available/unavailable
    if (!oldItems && newItems || oldItems && !newItems) {
      return true;
    }

    const oldItemsValues = mapValues(oldItems);
    const newItemsValues = mapValues(newItems);

    const itemsChanged = !_.eq(oldItemsValues, newItemsValues);
    const propsChanged = !_.eq(_.omit(this.props, omitProps), _.omit(nextProps, omitProps));

    return itemsChanged || propsChanged;
  }

  render() {
    const restProps = _.omit(this.props, 'children');

    return (
      <GenericDropdown
        {...restProps}
        renderDropdownInput={this.buildInput}>
        { this.renderItems() }
      </GenericDropdown>
    );
  }
}
