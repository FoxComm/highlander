/* @flow */

// libs
import React, { Component, Element } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// styles
import s from './text-input.css';

type Props = {
  value: string,
  className?: string,
  onChange?: (value: string) => void,
  placeholder?: string | Element<*>,
  autoFocus?: boolean,
  disabled?: boolean,
};

type State = {
  value: string,
};

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

  render(): Element<*> {
    const { className, placeholder, onChange, ...rest } = this.props;
    const inputClass = classNames(s.input, className, '__cssmodules');

    return (
      <input
        type="text"
        className={inputClass}
        onChange={({ target }) => this.handleChange(target.value)}
        placeholder={placeholder}
        {...rest}
        value={onChange ? this.props.value : this.state.value} />
    );
  }
}
