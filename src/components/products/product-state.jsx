/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';

import { Dropdown, DropdownItem } from '../dropdown';
import DateTimePicker from '../date-time-picker/date-time-picker';
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
      showActiveFromPicker: false,
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
          <DateTimePicker onCancel={this.handleCancelFrom} />
        </div>
      );
    }
  }

  get activeToPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const picker = this.state.showActiveToPicker
        ? <DateTimePicker onCancel={this.handleCancelTo} />
        : <a onClick={this.handleShowActiveTo}><i className="icon-add" /></a>;

      return (
        <div className="fc-product-state__picker _end">
          <div className="fc-product-state__picker-label">
            End 
          </div>
          {picker}
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

  @autobind
  handleCancelFrom() {
    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    });
  }

  @autobind
  handleCancelTo() {
    this.setState({
      showActiveToPicker: false,
    });
  }

  @autobind
  handleShowActiveTo() {
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: true,
    });
  }


  get activeDropdown(): Element {
    const isDisabled = this.state.showActiveFromPicker;
    return (
      <Dropdown
        className="fc-product-state__active-state"
        disabled={isDisabled}
        value={this.state.activeState}
        onChange={this.handleActiveChange}>
        <DropdownItem value="active">Active</DropdownItem>
        <DropdownItem value="inactive">Inactive</DropdownItem>
      </Dropdown>
    );
  }

  @autobind
  handleClickCalendar() {
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: false,
    });
  }

  @autobind
  handleClickCloseFrom() {
    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    });
  }

  render(): Element {
    return (
      <div className="fc-product-state">
        <div className="fc-product-state__header">
          <div className="fc-product-state__text">
            State
          </div>
          <div className="fc-product-state__icon">
            <a onClick={this.handleClickCalendar}><i className="icon-calendar" /></a>
          </div>
        </div>
        {this.activeDropdown}
        {this.activeFromPicker}
        {this.activeToPicker}
      </div>
    );
  }
}
