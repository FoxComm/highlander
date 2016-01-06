import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

import _ from 'lodash';
import classNames from 'classnames';

import { Button } from '../common/buttons';
import AppendInput from '../forms/append-input';

const weeks = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

export default class DatePicker extends React.Component {
  static propTypes = {
    showInput: PropTypes.bool,
    showPicker: PropTypes.bool,
    month: PropTypes.number,
    year: PropTypes.number
  };

  static defaultProps = {
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
      return (
        <AppendInput
          icon="calendar"
          inputName="someDate"
          inputValue={this.state.selectedDate} />
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
  goBackMonth() {
    const newDate = new Date(this.state.year, this.state.month - 1, 1);
    this.setState({
      ...this.state,
      month: newDate.getMonth(),
      year: newDate.getFullYear()
    });
  }

  @autobind
  goForwardMonth() {
    const newDate = new Date(this.state.year, this.state.month + 1, 1);
    this.setState({
      ...this.state,
      month: newDate.getMonth(),
      year: newDate.getFullYear()
    });
  }

  @autobind
  selectDate(date) {
    this.setState({ 
      ...this.state,
      selectedDate: date 
    });
  }

  @autobind
  renderMonth(date, isFirst = false) {
    const monthName = date.toLocaleString('en-us', { month: 'long', year: 'numeric' });

    return (
      <div className="fc-datepicker__month">
        <div className="fc-datepicker__month-header">
          <a className="fc-datepicker__month-action-back" onClick={this.goBackMonth}>
            {isFirst && <i className="icon-chevron-left" />} 
          </a>
          <div className="fc-datepicker__month-name">
            {monthName}
          </div>
          <a className="fc-datepicker__month-action-forward" onClick={this.goForwardMonth}>
            {!isFirst && <i className="icon-chevron-right" />} 
          </a>
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
        <div className={klass} onClick={() => this.selectDate(dt)}>
          {dt.getDate()}
        </div>
      );
    });
  }

  render() {
    return (
      <div className="fc-datepicker">
        {this.inputBox}
        {this.picker}
      </div>
    );
  }
}
