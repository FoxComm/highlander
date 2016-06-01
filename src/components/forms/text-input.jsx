/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

type Props = {
  className?: string,
  onChange: (value: string) => void,
  placeholder?: string,
  value: ?string,
};

export default class TextInput extends Component<void, Props, void> {
  static propTypes = {
    className: PropTypes.string,
    onChange: PropTypes.func,
    placeholder: PropTypes.string,
    value: PropTypes.string,
  };

  handleChange(value) {
    if (this.props.onChange) {
      this.props.onChange(value)
    } else {
      this.setState({value});
    }
  };

  state = {
    value: this.props.value
  };

  render(): Element {
    const { className, placeholder, ...rest } = this.props;
    const inputClass = classNames('fc-text-input', className);

    return (
      <input
        type="text"
        className={inputClass}
        onChange={({ target }) => this.handleChange(target.value)}
        placeholder={placeholder}
        value={this.state.value}
        {...rest} />
    );
  }
}
