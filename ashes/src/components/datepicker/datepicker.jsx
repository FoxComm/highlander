/* @flow weak */

// libs
import React from 'react';
import { autobind } from 'core-decorators';
import moment from 'moment';
import _ from 'lodash';
import classNames from 'classnames';

// components
import AppendInput from '../forms/append-input';
import Icon from 'components/core/icon';

const weeks = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

const suppressClick = event => {
  event.preventDefault();
  event.stopPropagation();
};

type Props = {
  className?: string,
  date: Date,
  onChange: (date: Date) => any,
  onClick: (date: Date) => any,
  showInput?: boolean,
  showPicker?: boolean,
  inputFormat: string,
};

type DateState = {
  month: number,
  year: number,
};

type State = DateState & {
  showPicker: boolean,
};

export default class DatePicker extends React.Component {
  props: Props;
  state: State;

  static defaultProps = {
    date: null,
    onChange: _.noop,
    onClick: _.noop,
    showInput: true,
    showPicker: false,
    inputFormat: 'MM/DD/YYYY',
  };

  constructor(props: Props, context) {
    super(props, context);

    const date = props.date || new Date();
    this.state = {
      showPicker: !!props.showPicker,
      month: date.getMonth(),
      year: date.getFullYear(),
    };
  }

  get selectedDate(): ?Date {
    return this.props.date;
  }

  get inputBox() {
    const { showInput, inputFormat } = this.props;
    if (!showInput) {
      return null;
    }

    const { showPicker } = this.state;
    const value = this.selectedDate ? this.selectedDate.toString() : '';
    const prettyDate = this.selectedDate ? moment(this.selectedDate).format(inputFormat) : '';

    return (
      <div className="fc-datepicker__control">
        <AppendInput
          icon="calendar"
          inputName="someDate"
          value={value}
          inputValuePretty={prettyDate}
          onBlur={this.blurred}
          onChange={this.changed}
          onFocus={this.focused}
          placeholder={inputFormat.toLowerCase()}
        />
        {showPicker && <div className="fc-datepicker__arrow-up" />}
      </div>
    );
  }

  get picker() {
    const { showPicker, month, year } = this.state;

    if (showPicker) {
      const firstMonth = new Date(year, month, 1);
      const secondMonth = new Date(year, month + 1, 1);

      return (
        <div className="fc-datepicker__dropdown-container">
          {this.renderMonth(firstMonth, true)}
          {this.renderMonth(secondMonth)}
        </div>
      );
    }
  }

  @autobind
  blurred(event) {
    this.setState({
      showPicker: false,
    });
  }

  @autobind
  changed({ target }) {
    const date = moment(target.value, this.props.inputFormat);
    if (date.isValid()) {
      this.selectDate(date.toDate());
    }
  }

  @autobind
  focused(event) {
    this.setState({
      showPicker: true,
    });
  }

  @autobind
  goBackMonth(event) {
    suppressClick(event);

    const newDate = new Date(this.state.year, this.state.month - 1, 1);
    this.setState({
      month: newDate.getMonth(),
      year: newDate.getFullYear(),
    });
  }

  @autobind
  goForwardMonth(event) {
    suppressClick(event);

    const newDate = new Date(this.state.year, this.state.month + 1, 1);
    this.setState({
      month: newDate.getMonth(),
      year: newDate.getFullYear(),
    });
  }

  @autobind
  selectDate(date: Date) {
    const action = () => {
      this.props.onClick(date);
      this.props.onChange(date);
    };

    this.setState(
      {
        showPicker: false,
      },
      action
    );
  }

  @autobind
  renderMonth(date, isFirst = false) {
    const monthName = date.toLocaleString('en-us', { month: 'long', year: 'numeric' });

    const backAction = (
      <a onClick={this.goBackMonth} onMouseDown={suppressClick} onMouseUp={suppressClick}>
        <Icon name="chevron-left" />
      </a>
    );

    const forwardAction = (
      <a onClick={this.goForwardMonth} onMouseDown={suppressClick} onMouseUp={suppressClick}>
        <Icon className="chevron-right" />
      </a>
    );

    return (
      <div className="fc-datepicker__month">
        <div className="fc-datepicker__month-header">
          <div className="fc-datepicker__month-action-back">
            {isFirst && backAction}
          </div>
          <div className="fc-datepicker__month-name">
            {monthName}
          </div>
          <div className="fc-datepicker__month-action-forward">
            {!isFirst && forwardAction}
          </div>
        </div>
        <div className="fc-datepicker__weeks-header">
          {weeks.map((w, idx) => <div className="fc-datepicker__week" key={`${w}-${idx}`}>{w}</div>)}
        </div>
        <div className="fc-datepicker__days">
          {this.renderDays(date)}
        </div>
      </div>
    );
  }

  @autobind
  renderDays(date) {
    const currentDate = new Date();
    const startDay = 1 - date.getDay(); // Day is 1-based in JS Date.
    const selectedTime = this.selectedDate ? this.selectedDate.getTime() : '';

    return _.range(startDay, startDay + 35).map(day => {
      const dt = new Date(date.getFullYear(), date.getMonth(), day);
      const klass = classNames('fc-datepicker__day', {
        '_last-month': date.getMonth() > dt.getMonth(),
        '_next-month': date.getMonth() < dt.getMonth(),
        _current: currentDate.getMonth() == dt.getMonth() && currentDate.getDate() == dt.getDate(),
        _selected: _.isEqual(dt.getTime(), selectedTime),
      });

      return (
        <div className={klass} onMouseDown={() => this.selectDate(dt)} key={day}>
          {dt.getDate()}
        </div>
      );
    });
  }

  render() {
    const className = classNames('fc-datepicker', this.props.className);
    return (
      <div className={className}>
        {this.inputBox}
        {this.picker}
      </div>
    );
  }
}
