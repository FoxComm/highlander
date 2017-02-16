/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import moment from 'moment';
import { autobind } from 'core-decorators';
import _ from 'lodash';

import { Dropdown, DropdownItem } from '../dropdown';
import DatePicker from '../datepicker/datepicker';
import DateTimeCounter from './counter';
import TextInput from '../forms/text-input';

type Props = {
  dateTime: ?string,
  onCancel: () => void,
  onChange: (dateTime: string) => void,
  pickerCloseBtnId: string,
};

type State = {
  hour: string,
  minutes: string,
  ampm: string,
};

function formatTimeDigits(digits: number): string {
  return digits < 10 ? `0${digits}` : digits.toString();
}

function formatDate(date: Date): string {
  return date.toISOString();
}

function setStateFromProps(props: Props): State {
  if (props.dateTime) {
    const localTime = moment.utc(props.dateTime).local();
    const hour = localTime.hours() == 0 ? 12 : localTime.hours() % 12;
    const minutes = localTime.minutes();

    return {
      hour: formatTimeDigits(hour),
      minutes: formatTimeDigits(minutes),
      ampm: localTime.hours() < 12 ? 'am' : 'pm',
    };
  } else {
    return { hour: '09', minutes: '00', ampm: 'am' };
  }
}

export default class DateTimePicker extends Component<void, Props, State> {
  state: State;
  props: Props;

  constructor(props: Props, ...args: any) {
    super(props, ...args);
    this.state = setStateFromProps(this.props);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState(setStateFromProps(nextProps));
  }

  get localTime(): ?Object {
    if (this.props.dateTime) {
      return moment.utc(this.props.dateTime).local();
    }
  }

  get date(): ?Object {
    if (this.localTime) {
      const local = this.localTime.toObject();
      return new Date(local.years, local.months, local.date);
    }
  }

  get currentDate(): Object {
    if (this.localTime) {
      return this.localTime;
    } else {
      return moment().hour(9).minutes(0).seconds(0);
    }
  }

  @autobind
  handleChangeDate(date: Date) {
    const newDate = moment(date);
    if (newDate.isValid()) {
      let currentDate = this.currentDate;
      currentDate.year(newDate.year()).dayOfYear(newDate.dayOfYear());
      this.triggerChange(currentDate);
    }
  }

  @autobind
  handleChangeHour(hour: string) {
    if (!_.isNull(hour.match(/^[0-9]*$/))) {
      this.setState({ hour });
    }
  }

  @autobind
  handleChangeMinutes(minutes: string) {
    if (!_.isNull(minutes.match(/^[0-9]*$/))) {
      this.setState({ minutes });
    }
  }

  triggerChange(date: Date) {
    this.props.onChange(formatDate(date));
  }

  @autobind
  handleAddHour() {
    let currentDate = this.currentDate;
    currentDate.add(1, 'h');
    this.triggerChange(currentDate);
  }

  @autobind
  handleSubtractHour() {
    let currentDate = this.currentDate;
    currentDate.subtract(1, 'h');
    this.triggerChange(currentDate);
  }

  @autobind
  handleAddMinutes() {
    let currentDate = this.currentDate;
    currentDate.add(5, 'm');
    this.triggerChange(currentDate);
  }

  @autobind
  handleSubtractMinutes() {
    let currentDate = this.currentDate;
    currentDate.subtract(5, 'm');
    this.triggerChange(currentDate);
  }

  @autobind
  handleChangeAmPm(ampm: string) {
    this.setState({ ampm });
  }

  @autobind
  handleAmPmToggle() {
    const ampm = this.state.ampm.toLowerCase() === 'am' ? 'pm' : 'am';
    this.setState({ ampm }, this.updateDateTime);
  }

  @autobind
  updateDateTime() {
    // Verify that hours are correct.
    const hour = parseInt(this.state.hour);
    const minutes = parseInt(this.state.minutes);
    const ampm = this.state.ampm.toLowerCase();

    if (hour < 0 || hour > 23 || minutes < 0 || minutes > 59) {
      this.setState(setStateFromProps(this.props));
    } else if (ampm !== 'am' || ampm !== 'pm') {
      this.setState(setStateFromProps(this.props));
    }

    let normalizedHours = hour;
    if (ampm === 'am' && hour >= 12) {
      normalizedHours -= 12;
    } else if (ampm === 'pm' && hour < 12) {
      normalizedHours += 12;
    }

    let currentDate = this.currentDate;
    currentDate.hour(normalizedHours).minutes(minutes);
    this.triggerChange(currentDate);
  }

  render() {
    return (
      <div className="fc-date-time-picker">
        <DatePicker
          className="fc-date-time-picker__date"
          date={this.date}
          onChange={this.handleChangeDate} />
        <div className="fc-date-time-picker__time">
          <DateTimeCounter onClickUp={this.handleAddHour} onClickDown={this.handleSubtractHour}>
            <TextInput
              className="fc-date-time-picker__hour"
              value={this.state.hour}
              onChange={this.handleChangeHour}
              onBlur={this.updateDateTime} />
          </DateTimeCounter>
          <div className="fc-date-time-picker__separator">:</div>
          <DateTimeCounter onClickUp={this.handleAddMinutes} onClickDown={this.handleSubtractMinutes}>
            <TextInput
              className="fc-date-time-picker__minute"
              value={this.state.minutes}
              onChange={this.handleChangeMinutes}
              onBlur={this.updateDateTime} />
          </DateTimeCounter>
          <DateTimeCounter onClickUp={this.handleAmPmToggle} onClickDown={this.handleAmPmToggle}>
            <TextInput
              className="fc-date-time-picker__ampm"
              value={this.state.ampm}
              onChange={this.handleChangeAmPm}
              onBlur={this.updateDateTime} />
          </DateTimeCounter>
          <a
            id={this.props.pickerCloseBtnId}
            className="fc-date-time-picker__close"
            onClick={this.props.onCancel}>
            <i className="icon-close" />
          </a>
        </div>
      </div>
    );
  }
}
