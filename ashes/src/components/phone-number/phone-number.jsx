
import React, { PropTypes } from 'react';

export default class PhoneNumber extends React.Component {

  static propTypes = {
    children: PropTypes.string
  };

  formatPhoneNumber(value) {
    const numbers = value.replace(/[^\d]/g, '');

    if (numbers.length === 10) {
      return `(${numbers.slice(0, 3)}) ${numbers.slice(3, 6)}-${numbers.slice(6, 10)}`;
    }
    return value;
  }

  get phoneNumber() {
    return this.formatPhoneNumber(this.props.children);
  }

  render() {
    return (
      <span className="phone">{this.phoneNumber}</span>
    );
  }
}
