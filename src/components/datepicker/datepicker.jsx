import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import { Button } from '../common/buttons';
import AppendInput from '../forms/append-input';

const weeks = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

const suppressClick = event => {
  event.preventDefault();
  event.stopPropagation();
};

export default class DatePicker extends React.Component {
  static propTypes = {
    className: PropTypes.string,
    onClick: PropTypes.func,
    showInput: PropTypes.bool,
    showPicker: PropTypes.bool,
    month: PropTypes.number,
    year: PropTypes.number
  };

  static defaultProps = {
    onClick: _.noop,
    showInput: true,
    showPicker: false,
    month: new Date().getMonth() + 1,
    year: new Date().getFullYear()
  };

  constructor(props, context) {
    super(props, context);

    // Assume that the month coming from the called is an index based on 1,
    // because this is how it will appear in the UI. For our purposes, turn it
    // into a 0-based index as that's how JS expects it.
    this.state = {
      selectedDate: null,
      showPicker: props.showPicker,
      month: props.month - 1,
      year: props.year
    };
  }

  get inputBox() {
    if (this.props.showInput) {
      const date = this.state.selectedDate || '';
      const prettyDate = this.state.selectedDate
        ? this.state.selectedDate.toLocaleString('en-us', {
          month: '2-digit',
          day: '2-digit',
          year: 'numeric',
        }) : '';

      return (
        <div className="fc-datepicker__control">
          <AppendInput
            icon="calendar"
            inputName="someDate"
            inputValue={date}
            inputValuePretty={prettyDate}
            onBlur={this.blurred}
            onFocus={this.focused}
            placeholder="mm/dd/yyyy" />
          {this.state.showPicker && <div className="fc-datepicker__arrow-up"></div>}
        </div>
      );
    }
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
      ...this.state,
      showPicker: false
    });
  }

  @autobind
  focused(event) {
    this.setState({
      ...this.state,
      showPicker: true
    });
  }

  @autobind
  goBackMonth(event) {
    suppressClick(event);

    const newDate = new Date(this.state.year, this.state.month - 1, 1);
    this.setState({
      ...this.state,
      month: newDate.getMonth(),
      year: newDate.getFullYear()
    });
  }

  @autobind
  goForwardMonth(event) {
    suppressClick(event);

    const newDate = new Date(this.state.year, this.state.month + 1, 1);
    this.setState({
      ...this.state,
      month: newDate.getMonth(),
      year: newDate.getFullYear()
    });
  }

  @autobind
  selectDate(date) {
    this.props.onClick(date);

    this.setState({
      ...this.state,
      selectedDate: date
    });
  }

  @autobind
  renderMonth(date, isFirst = false) {
    const monthName = date.toLocaleString('en-us', { month: 'long', year: 'numeric' });

    const backAction = (
      <a
        onClick={this.goBackMonth}
        onMouseDown={suppressClick}
        onMouseUp={suppressClick}>
        <i className="icon-chevron-left" />
      </a>
    );

    const forwardAction = (
      <a
        onClick={this.goForwardMonth}
        onMouseDown={suppressClick}
        onMouseUp={suppressClick}>
        <i className="icon-chevron-right" />
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
          {weeks.map(w => <div className="fc-datepicker__week">{w}</div>)}
        </div>
        <div className="fc-datepicker__days">
          {this.renderDays(date)}
        </div>
      </div>
    );
  }

  @autobind
  renderDays(date) {
    const startDay = 1 - date.getDay(); // Day is 1-based in JS Date.
    const selectedTime = !_.isNull(this.state.selectedDate)
      ? this.state.selectedDate.getTime()
      : '';

    return _.range(startDay, startDay + 35).map(day => {
      const dt = new Date(date.getFullYear(), date.getMonth(), day);
      const klass = classNames('fc-datepicker__day', {
        '_last-month': date.getMonth() > dt.getMonth(),
        '_next-month': date.getMonth() < dt.getMonth(),
        '_selected': _.isEqual(dt.getTime(), selectedTime)
      });

      return (
        <div
          className={klass}
          onClick={() => this.selectDate(dt)}
          onMouseDown={suppressClick}
          onMouseUp={suppressClick}>
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
