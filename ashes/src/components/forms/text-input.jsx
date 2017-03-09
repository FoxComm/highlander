/* @flow */

import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

type Props = {
  className?: string,
  onChange: (value: string) => void,
  placeholder?: string,
  value: ?string,
  autoFocus?: boolean
};

type State = {
  value: ?string,
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
      this.setState({value});
    }
  };

  render() {
    const { className, placeholder, onChange, ...rest } = this.props;
    const inputClass = classNames('fc-text-input', className);

    return (
      <input
        type="text"
        className={inputClass}
        onChange={({ target }) => this.handleChange(target.value)}
        placeholder={placeholder}
        {...rest}
        value={this.state.value} />
    );
  }
}
