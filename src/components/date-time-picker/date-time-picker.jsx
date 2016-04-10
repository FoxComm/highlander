/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import moment from 'moment';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import { Dropdown, DropdownItem } from '../dropdown';
import DatePicker from '../datepicker/datepicker';
import TextInput from '../forms/text-input';

type Props = {
  dateTime: ?string,
  onCancel: () => void,
  onChange: (dateTime: string) => void,
};

/**
 * emptyDate is a placeholder when a date hasn't been specified. It will always
 * return the current date at 9:00am local time.
 */
function emptyDate() {
  return moment();
}

export default class DateTimePicker extends Component<void, Props, void> {
  get localTime(): Object {
    // Convert from UTC to local.
    return moment.utc(this.props.dateTime).local();
  }

  get date(): ?Object {
    if (this.props.dateTime) {
      const local = this.localTime.toObject();
      return new Date(local.years, local.months, local.date);
    }
  }

  get hour(): number {
    if (this.props.dateTime) {
      return this.localTime.hours();
    } else {
      return 0;
    }
  }

  get renderedHour(): string {
    if (this.hour > 12) {
      return `${this.hour - 12}`;
    } else if (this.hour == 0) {
      return '12';
    } else {
      return `${this.hour}`;
    }
  }

  get minute(): string {
    if (this.props.dateTime) {
      const minutes = this.localTime.minutes();
      return minutes > 9 ? minutes : `0${minutes}`;
    } else {
      return '00';
    }
  }

  get ampm(): string {
    return this.hour < 12 ? 'am' : 'pm';
  }

  @autobind
  handleChangeDate(newDateStr: string) {
    const newDate = moment(newDateStr);
    let currentDate = this.props.dateTime ? this.localTime : emptyDate();
    currentDate.year(newDate.year()).dayOfYear(newDate.dayOfYear());
    this.props.onChange(currentDate.toISOString());
  }

  render(): Element {
    return (
      <div className="fc-date-time-picker">
        <DatePicker
          className="fc-date-time-picker__date"
          date={this.date}
          onChange={this.handleChangeDate} />
        <TextInput
          className="fc-date-time-picker__hour"
          value={this.renderedHour}
          onChange={_.noop} />
        <div className="fc-date-time-picker__separator">:</div>
        <TextInput
          className="fc-date-time-picker__minute"
          value={this.minute}
          onChange={_.noop} />
        <TextInput
          className="fc-date-time-picker__ampm"
          value={this.ampm}
          onChange={_.noop} />
        <a
          className="fc-date-time-picker__close"
          onClick={this.props.onCancel}>
          <i className="icon-close" />
        </a>
      </div>
    );
  }
}
