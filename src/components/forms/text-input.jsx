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

type State = {
  value: ?string,
};

export default class TextInput extends Component<void, Props, State> {
  static propTypes = {
    className: PropTypes.string,
    onChange: PropTypes.func,
    placeholder: PropTypes.string,
    value: PropTypes.string,
  };

  handleChange(value: string) {
    if (this.props.onChange) {
      this.props.onChange(value);
    } else {
      this.setState({value});
    }
  };

  state: State = {
    value: this.props.value || ''
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
        {...rest}
        value={this.state.value} />
    );
  }
}
