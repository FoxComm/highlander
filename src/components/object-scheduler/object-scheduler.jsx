/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';
import { isActive } from 'paragons/common';
import { trackEvent } from 'lib/tracker';

import { Dropdown, DropdownItem } from '../dropdown';
import DateTimePicker from '../date-time-picker/date-time-picker';

type Attribute = { t: string, v: any };
type Attributes = { [key:string]: Attribute };

type Props = {
  attributes: Attributes,
  onChange: (attributes: Attributes) => void,
  title: string,
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
            dateTime={this.activeTo}
            onChange={this.updateActiveTo}
            onCancel={this.handleCancelTo} />
        )
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

    return {
      ...attributes,
      activeFrom: {
        v: activeFrom,
        t: 'datetime'
      },
      activeTo: {
        v: activeTo,
        t: 'datetime'
      },
    };
  }

  @autobind
  handleActiveChange(value: string) {
    const now = moment().utc().format('YYYY-MM-DDTHH:mm:ss.SSSZ');
    const activeFrom = value == 'active' ? now : null;

    const attributes = this.setFromTo(activeFrom, null);
    this.props.onChange(attributes);
  }

  @autobind
  handleCancelFrom() {
    trackEvent('Scheduler', 'click_cancel_from_picker');
    const attributes = this.setFromTo(null, null);

    this.setState({
      showActiveFromPicker: false,
      showActiveToPicker: false,
    }, () => this.props.onChange(attributes));
  }

  @autobind
  handleCancelTo() {
    trackEvent('Scheduler', 'click_cancel_to_picker');
    this.setState({
      showActiveToPicker: false,
    }, () => this.updateActiveTo(null));
  }

  @autobind
  handleShowActiveTo() {
    trackEvent('Scheduler', 'show_active_to_picker');
    this.setState({
      showActiveFromPicker: true,
      showActiveToPicker: true,
    });
  }


  get activeDropdown(): Element {
    const activeState = this.isActive ? 'active' : 'inactive';
    const isDisabled = this.state.showActiveFromPicker;
    return (
      <Dropdown
        className="fc-product-state__active-state"
        disabled={isDisabled}
        value={activeState}
        onChange={this.handleActiveChange}>
        <DropdownItem value="active">Active</DropdownItem>
        <DropdownItem value="inactive">Inactive</DropdownItem>
      </Dropdown>
    );
  }

  @autobind
  handleClickCalendar() {
    if (this.state.showActiveFromPicker) {
      trackEvent('Scheduler', 'hide_date_picker');
      this.setState({
        showActiveFromPicker: false,
        showActiveToPicker: false,
      });
    } else {
      trackEvent('Scheduler', 'show_date_picker');
      this.setState({
        showActiveFromPicker: true,
        showActiveToPicker: !_.isNull(this.activeTo) && !_.isUndefined(this.activeTo),
      });
    }
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
