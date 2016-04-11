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

type State = {
  hour: string,
  minutes: string,
  isMorning: bool,
};

/**
 * emptyDate is a placeholder when a date hasn't been specified. It will always
 * return the current date at 9:00am local time.
 */
function emptyDate() {
  return moment();
}

function setStateFromProps(props: Props): State {
  if (props.dateTime) {
    const localTime = moment.utc(props.dateTime).local();
    const hour = localTime.hours() % 12;
    const hourStr = hour < 10 ? `0${hour}` : `${hour}`;
    const minutes = localTime.minutes();
    const minutesStr = minutes < 10 ? `0${minutes}` : `${minutes}`;
    
    return { hour: hourStr, minutes: minutesStr, isMorning: hour < 12 };
  } else {
    return { hour: '9', minutes: '0', isMorning: true };
  }
}

export default class DateTimePicker extends Component<void, Props, State> {
  state: State;

  constructor(props: Props, ...args: any) {
    super(props, ...args);
    this.state = setStateFromProps(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(setStateFromProps(nextProps));
  }

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

  @autobind
  handleChangeDate(newDateStr: string) {
    const newDate = moment(newDateStr);
    let currentDate = this.props.dateTime ? this.localTime : emptyDate();
    currentDate.year(newDate.year()).dayOfYear(newDate.dayOfYear());
    this.props.onChange(currentDate.toISOString());
  }

  @autobind
  handleChangeHour(hour: string) {
    if (hour == '') {
      this.setState({ hour });
      return;
    }

    const hourInt = parseInt(hour);
    if (hourInt && hourInt >= 0 && hourInt <= 23) {
      let normalizedHour = hourInt;
      if (!this.state.isMorning && hourInt < 12) {
        normalizedHour = hourInt + 12;
      } else if (this.state.isMorning && hourInt == 12) {
        normalizedHour = 0;
      }

      let currentDate = this.props.dateTime ? this.localTime : emptyDate();
      currentDate.hour(normalizedHour);
      this.props.onChange(currentDate.toISOString());
    }
  }

  @autobind
  handleChangeMinutes(minutes: string) {
    if (minutes == '') {
      this.setState({ minutes });
      return;
    }

    const minutesInt = parseInt(minutes);
    if (minutesInt && minutesInt >= 0 && minutesInt <= 59) {
      let currentDate = this.props.dateTime ? this.localTime : emptyDate();
      currentDate.minutes(minutesInt);
      this.props.onChange(currentDate.toISOString());
    }
  }

  render(): Element {
    return (
      <div className="fc-date-time-picker">
        <DatePicker
          className="fc-date-time-picker__date"
          date={this.date}
          onChange={this.handleChangeDate} />
        <div className="fc-date-time-picker__time">
          <TextInput
            className="fc-date-time-picker__hour"
            value={this.state.hour}
            onChange={this.handleChangeHour} />
          <div className="fc-date-time-picker__separator">:</div>
          <TextInput
            className="fc-date-time-picker__minute"
            value={this.state.minutes}
            onChange={this.handleChangeMinutes} />
          <TextInput
            className="fc-date-time-picker__ampm"
            value={this.state.isMorning ? 'am' : 'pm'}
            onChange={_.noop} />
          <a
            className="fc-date-time-picker__close"
            onClick={this.props.onCancel}>
            <i className="icon-close" />
          </a>
        </div>
      </div>
    );
  }
}
