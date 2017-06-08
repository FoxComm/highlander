/* @flow */

// libs
import omit from 'lodash/omit';
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import s from './text-input.css';

type Props = {
  /** Input value itself */
  value: string,
  /** Input default value for uncontrolled */
  defaultValue: string,
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

  state: State = {
    value: this.props.value || this.props.defaultValue || ''
  };

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.value !== this.props.value) {
      this.setState({ value: nextProps.value });
    }
  }

  @autobind
  handleChange(e: SyntheticInputEvent) {
    const { value, name } = e.target;
    if (this.props.onChange) {
      this.props.onChange(value, name, e);
    } else {
      this.setState({ value });
    }
  }

  render(): Element<any> {
    const { className, placeholder, ...rest } = this.props;
    const inputClass = classNames(s.input, className, '__cssmodules');

    const inputProps = omit(rest, ['value', 'onChange', 'defaultValue']);

    return (
      <input
        type="text"
        className={inputClass}
        placeholder={placeholder}
        value={this.state.value}
        onChange={this.handleChange}
        {...inputProps}
      />
    );
  }
}