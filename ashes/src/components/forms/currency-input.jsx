import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { pick, flow, reduce, entries } from 'lodash/fp';
import formatCurrency, { stringToCurrency } from '../../lib/format-currency';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';
import classNames from 'classnames';

import PrependInput from './prepend-input';

export default class CurrencyInput extends React.Component {

  static propTypes = {
    min: PropTypes.number,
    value: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    step: PropTypes.number,
    onChange: PropTypes.func,
    currency: PropTypes.string,
    fractionBase: PropTypes.number,
    defaultValue: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    className: PropTypes.string,
    inputClass: PropTypes.string,
    inputName: PropTypes.string,
  };

  static defaultProps = {
    value: 0,
    min: 0,
    step: 1,
    fractionBase: 2,
    currency: 'USD',
    inputName: 'currencyInput',
  };

  constructor(props, ...args) {
    super(props, ...args);
    this.state = {
      focus: false,
      editing: props.value || 0
    };
  }

  @autobind
  onChange(e) {
    let value = e.target.value;
    // don't pass non numbers
    let number = Number(value);
    if (isNaN(number) || !isFinite(number)) {
      e.preventDefault();
      e.stopPropagation();
      return;
    }

    this.setState({
      editing: value
    });

    this.fireOnChange(value);
  }

  get value() {
    return this.props.value || this.props.defaultValue;
  }

  @autobind
  onInputFocus() {
    this.setState({
      focus: true,
      editing: formatCurrency(this.value, {
        bigNumber: true,
        groupDigits: false,
        fractionBase: this.props.fractionBase
      })
    });
  }

  @autobind
  fireOnChange(value) {
    if (this.props.onChange) {
      this.props.onChange(stringToCurrency(value, {...this.props}));
    }
  }

  @autobind
  onInputBlur() {
    this.setState({
      focus: false
    });
  }

  get valueProps() {
    let inputValue;
    if (this.state.focus) {
      inputValue = this.state.editing;
    } else {
      inputValue = formatCurrency(this.value, {bigNumber: true, fractionBase: this.props.fractionBase});
    }
    const keys = ['value', 'defaultValue'];

    return flow(
      pick(keys),
      entries,
      reduce((r, [k, v]) => assoc(r, k, inputValue), {})
    )(this.props);
  }

  render() {
    const classnames = classNames(this.props.inputClass, {
      'fc-input-group': true,
      '_focused': this.state.focus
    });

    return (
      <PrependInput
        inputName={this.props.inputName}
        className={this.props.className}
        inputClass={classnames}
        icon="usd"
        onChange={this.onChange}
        onFocus={this.onInputFocus}
        onBlur={this.onInputBlur}
        {...this.valueProps}
      />
    );
  }
}
