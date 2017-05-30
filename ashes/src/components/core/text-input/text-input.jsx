/* @flow */

// libs
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import s from './text-input.css';

type Props = {
  /** Input value itself */
  value: string,
  /** Additional className */
  className?: string,
  /** Action performed on input's value change */
  onChange?: (value: string) => void,
  /** Placeholder value */
  placeholder?: string | Element<any>,
  /** If true - enables autofocus */
  autoFocus?: boolean,
  /** If true - disables input */
  disabled?: boolean,
};

type State = {
  value: string,
};

/**
 * TextInput is a wrapper over DOM's input
 *
 * @class TextInput
 */

export default class TextInput extends Component {
  props: Props;

  // static defaultProps = {
  //   value: ''
  // };

  state: State = {
    value: this.props.value || this.props.defaultValue || ""
  };

  componentWillReceiveProps(nextProps) {
    console.log(nextProps)
    if (nextProps.value !== this.props.value) {
      this.setState({ value: nextProps.value })
    }
  }

  componentWillUpdate(nextProps: Props) {
    if (this.state.value != nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  }

  @autobind
  handleChange(e: SyntheticEvent) {
    const { target: {value, name} } = e;
    if (this.props.onChange) {
      this.props.onChange(value, name, e);
    } else {
      this.setState({ value });
    }
  }

  render(): Element<any> {
    const { className, placeholder, onChange, defaultValue, ...rest } = this.props;
    const inputClass = classNames(s.input, className, '__cssmodules');

    return (
      <input
        type="text"
        className={inputClass}
        onChange={this.handleChange}
        placeholder={placeholder}
        value={this.state.value}
        {...rest}
      />
    );
  }
}
