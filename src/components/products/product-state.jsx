/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';

import { Dropdown, DropdownItem } from '../dropdown';
import TextInput from '../forms/text-input';

import type { FullProduct } from '../../modules/products/details';

type Props = {
  product: FullProduct,
};

type State = {
  activeState: ?string,
  showActiveFromPicker: bool,
  showActiveToPicker: bool,
};

export default class ProductState extends Component<void, Props, State> {
  state: State;

  constructor(props: Props) {
    super(props);

    const productShadow = this.props.product.shadow.product;
    const { activeFrom, activeTo } = productShadow;
    const activeState = this.isActive(activeFrom, activeTo) ? 'active' : 'inactive';

    this.state = {
      activeState,
      showActiveFromPicker: true,
      showActiveToPicker: false,
    };
  }

  get activeFromPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      return (
        <div className="fc-product-state__picker">
          <div className="fc-product-state__picker-header">
            Product will be active on:
          </div>
          <div className="fc-product-state__picker-label">
            Start
          </div>
          <div className="fc-product-state__picker-fields">
            <TextInput
              className="fc-product-state__picker-date"
              placeholder="mm/dd/yy"
              value=""
              onChange={_.noop} />
            <TextInput
              className="fc-product-state__picker-hour"
              value="12"
              onChange={_.noop} />
            <div className="fc-product-state__picker-separator">:</div>
            <TextInput
              className="fc-product-state__picker-minute"
              value="00"
              onChange={_.noop} />
            <TextInput
              className="fc-product-state__picker-ampm"
              value="am"
              onChange={_.noop} />
          </div>
        </div>
      );
    }
  }

  isActive(activeFrom: ?string, activeTo: ?string): bool {
    const now = moment();

    if (!activeFrom) {
      return false;
    } else if (now.diff(activeFrom) < 0) {
      return false;
    } else if (!activeTo && now.diff(activeTo) > 0) {
      return false;
    }

    return true;
  }

  @autobind
  handleActiveChange(value: string) {
    this.setState({ activeState: value });
  }

  render(): Element {
    return (
      <div className="fc-product-state">
        <div className="fc-product-state__header">
          <div className="fc-product-state__text">
            State
          </div>
          <div className="fc-product-state__icon">
            <a onClick={_.noop}><i className="icon-calendar" /></a>
          </div>
        </div>
        <Dropdown
          className="fc-product-state__active-state"
          value={this.state.activeState}
          onChange={this.handleActiveChange}>
          <DropdownItem value="active">Active</DropdownItem>
          <DropdownItem value="inactive">Inactive</DropdownItem>
        </Dropdown>
        {this.activeFromPicker}
      </div>
    );
  }
}
