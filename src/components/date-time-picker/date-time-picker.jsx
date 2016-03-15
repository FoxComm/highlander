/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import _ from 'lodash';

import { Dropdown, DropdownItem } from '../dropdown';
import TextInput from '../forms/text-input';

type Props = {
  onCancel: () => void,
};

export default class DateTimePicker extends Component<void, Props, void> {
  render(): Element {
    return (
      <div className="fc-date-time-picker">
        <TextInput
          className="fc-date-time-picker__date"
          placeholder="mm/dd/yy"
          value=""
          onChange={_.noop} />
        <TextInput
          className="fc-date-time-picker__hour"
          value="12"
          onChange={_.noop} />
        <div className="fc-date-time-picker__separator">:</div>
        <TextInput
          className="fc-date-time-picker__minute"
          value="00"
          onChange={_.noop} />
        <TextInput
          className="fc-date-time-picker__ampm"
          value="am"
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
