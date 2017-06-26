/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { autobind } from 'core-decorators';

// components
import TextInput from 'components/core/text-input';
import GenericDropdown from './generic-dropdown';
import DropdownItem from './dropdownItem';

// styles
import styles from './dropdown.css';

import type { Props as GenericProps, ValueType, DropdownItemType } from './generic-dropdown';

type Props = GenericProps;

type State = {
  token: string,
};

const omitProps = [
  'items',
  'emptyMessage',
  'renderDropdownInput',
  'renderNullTitle',
  'renderPrepend',
  'onChange'
];

const mapValues = (items: Array<DropdownItemType>): Array<ValueType> => items.map(([value]) => value);

const filterValues = (items: Array<DropdownItemType>, token: string) =>
  items.filter(([_, name]) => String(name).toLowerCase().indexOf(token.toLowerCase()) > -1);

const wrap = (reg: RegExp) => (str: string, i: number) =>
  str.search(reg) > -1 ? <span styleName="needle" key={`${str}-${i}`}>{str}</span> : str;

const highlightOccurrence = (needle: string) => (title: string) => {
  const reg = new RegExp(`(${needle})`, 'gi');
  const wrapper = wrap(reg);

  return title
    .split(reg)
    .filter(_.identity)
    .map(wrapper);
};

export default class Dropdown extends Component {
  props: Props;

  state: State = {
    token: '',
  };

  static defaultProps = {
    name: '',
  };

  @autobind
  handleInputChange(value: string) {
    this.setState({ token: value });
  }

  @autobind
  buildInput(value: string | number, title: string, props: Props, handleToggleClick: Function) {
    if (value === null) value = '';

    if (props.editable) {
      return (
        <div className="fc-dropdown__value">
          <TextInput
            name={props.name}
            value={this.state.token}
            onChange={this.handleInputChange}
            placeholder={props.placeholder}
            disabled={props.disabled}
            key={`${props.name}-${value}-selected`}
            autoComplete="off"
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
    const { name, editable } = this.props;
    const { token } = this.state;

    const items = _.get(this.props, 'items', []);
    const filtered = editable && token.length ? filterValues(items, token) : items;
    const processItem = editable && token.length ? highlightOccurrence(token) : _.identity;

    return _.map(filtered, ([value, title, isHidden]) => (
      <DropdownItem value={value} key={`${name}-${value}`} isHidden={isHidden}>
        {processItem(title)}
      </DropdownItem>
    ));
  }

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
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
    const tokenChanged = this.state.token !== nextState.token;

    return itemsChanged || propsChanged || tokenChanged;
  }

  render() {
    const restProps = _.omit(this.props, 'children');

    return (
      <GenericDropdown
        placeholder="- Select -"
        {...restProps}
        renderDropdownInput={this.buildInput}
      >
        { this.renderItems() }
      </GenericDropdown>
    );
  }
}
