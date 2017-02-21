/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';
import { isActive, setFromTo } from 'paragons/common';
import { trackEvent } from 'lib/analytics';

import DateTimePicker from '../date-time-picker/date-time-picker';
import StateDropdown from '../object-page/state-dropdown';
import type { StateChangeEvent } from '../object-page/state-dropdown';


type Props = {
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  title: string,
  parent?: string,
};

type State = {
  showActiveFromPicker: boolean,
  showActiveToPicker: boolean,
};

export default class ObjectScheduler extends Component {
  props: Props;
  state: State = this.fieldsStateFromProps(this.props);

  fieldsStateFromProps(props: Props): State {
    const activeFrom = this.getAttribute(props.attributes, 'activeFrom');
    const activeTo = this.getAttribute(props.attributes, 'activeTo');

    return {
      showActiveFromPicker: !!activeFrom,
      showActiveToPicker: !!activeTo
    };
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(this.fieldsStateFromProps(nextProps));
  }

  getAttribute(attrs: Attributes, name: string): any {
    return _.get(attrs, [name, 'v']);
  }

  get activeFrom(): ?string {
    return this.getAttribute(this.props.attributes, 'activeFrom');
  }

  get activeTo(): ?string {
    return this.getAttribute(this.props.attributes, 'activeTo');
  }

  get activeFromPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const activePhrase = `${this.props.title} will be active on:`;
      return (
        <div className="fc-product-state__picker">
          <div className="fc-product-state__picker-header">{activePhrase}</div>
          <div className="fc-product-state__picker-label">
            Start
          </div>
          <DateTimePicker
            pickerCloseBtnId="remove-start-date-btn"
            dateTime={this.activeFrom}
            onChange={this.updateActiveFrom}
            onCancel={this.handleCancelFrom} />
        </div>
      );
    }
  }

  get activeToPicker(): ?Element {
    if (this.state.showActiveFromPicker) {
      const picker = this.state.showActiveToPicker
        ? (
          <DateTimePicker
            pickerCloseBtnId="remove-end-date-btn"
            dateTime={this.activeTo}
            onChange={this.updateActiveTo}
            onCancel={this.handleCancelTo} />
        )
        : <a id="add-end-date-btn" onClick={this.handleShowActiveTo}><i className="icon-add" /></a>;

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

  get isActive(): bool {
    return isActive(this.activeFrom, this.activeTo);
  }

  @autobind
  updateActiveTo(value: ?string) {
    if (this.isPeriodValid(this.activeFrom, value)) {
      this.updateAttribute('activeTo', value);
    }
  }

  @autobind
  updateActiveFrom(value: ?string) {
    if (this.isPeriodValid(value, this.activeTo)) {
      this.updateAttribute('activeFrom', value);
    }
  }

  isPeriodValid(activeFrom: ?string, activeTo: ?string): boolean {
    if (!activeTo || !activeFrom) return true;

    const activeFromTime = moment(activeFrom).toDate().getTime();
    const activeToTime = moment(activeTo).toDate().getTime();

    return activeToTime >= activeFromTime;
  }

  updateAttribute(label: string, value: ?string) {
    const attribute = {
      [label]: {
        t: 'datetime',
        v: value,
      },
    };

    const newAttrs = { ...this.props.attributes, ...attribute };
    this.props.onChange(newAttrs);
  }

  setFromTo(activeFrom: ?string, activeTo: ?string): Attributes {
    const { attributes } = this.props;

    return setFromTo(attributes, activeFrom, activeTo);
  }

  @autobind
  handleActiveChange(event: StateChangeEvent) {
    this.props.onChange(event.attributes);
  }

  trackEvent(...args: any[]) {
    trackEvent(`Scheduler(${this.props.parent || ''})`, ...args);
  }

  @autobind
  handleCancelFrom() {
    this.trackEvent('click_cancel_from_picker');
    const attributes = this.setFromTo(null, null);

    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    }, () => this.props.onChange(attributes));
  }

  @autobind
  handleCancelTo() {
    this.trackEvent('click_cancel_to_picker');
    this.setState({
      showActiveToPicker: false,
    }, () => this.updateActiveTo(null));
  }

  @autobind
  handleShowActiveTo() {
    this.trackEvent('show_active_to_picker');
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: true,
    });
  }


  get activeDropdown(): Element {
    const isDisabled = !this.isActive && this.state.showActiveFromPicker;
    return (
      <StateDropdown
        id="state-dd"
        dropdownValueId="state-dd--value"
        className="fc-product-state__active-state"
        disabled={isDisabled}
        attributes={this.props.attributes}
        onChange={this.handleActiveChange}
      />
    );
  }

  @autobind
  handleClickCalendar() {
    if (this.state.showActiveFromPicker) {
      this.trackEvent('hide_date_picker');
      this.setState({
        showActiveFromPicker: false,
        showActiveToPicker: false,
      });
    } else {
      this.trackEvent('show_date_picker');
      this.setState({
        showActiveFromPicker: true,
        showActiveToPicker: !_.isNull(this.activeTo) && !_.isUndefined(this.activeTo),
      });
    }
  }

  shouldComponentUpdate(nextProps: Props, nextState: State): boolean {
    const prevActiveFrom = this.activeFrom;
    const prevActiveTo = this.activeTo;

    const nextActiveFrom = this.getAttribute(nextProps.attributes, 'activeFrom');
    const nextActiveTo = this.getAttribute(nextProps.attributes, 'activeTo');

    return prevActiveFrom !== nextActiveFrom || prevActiveTo !== nextActiveTo || !_.eq(this.state, nextState);
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
