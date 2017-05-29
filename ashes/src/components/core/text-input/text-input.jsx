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

  static defaultProps = {
    value: ''
  };

  state: State = {
    value: this.props.value
  };

  componentWillUpdate(nextProps: Props) {
    if (this.state.value != nextProps.value) {
      this.setState({ value: nextProps.value });
    }
  }

  @autobind
  handleChange(value: string) {
    if (this.props.onChange) {
      this.props.onChange(value);
    } else {
      this.setState({ value });
    }
  }

  render(): Element<any> {
    const { className, placeholder, onChange, ...rest } = this.props;
    const inputClass = classNames(s.input, className, '__cssmodules');

    return (
      <input
        type="text"
        className={inputClass}
        onChange={({ target }) => this.handleChange(target.value)}
        placeholder={placeholder}
        value={onChange ? this.props.value : this.state.value}
        {...rest}
      />
    );
  }
}
