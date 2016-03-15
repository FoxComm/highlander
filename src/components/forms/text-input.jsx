/**
 * @flow
 */

import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';

type Props = {
  className?: string,
  onChange: (value: string) => void,
  placeholder?: string,
  value: ?string,
};

export default class TextInput extends Component<void, Props, void> {
  static propTypes = {
    className: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    value: PropTypes.string,
  };

  render(): Element {
    const { className, onChange, placeholder, value, ...rest } = this.props;
    const inputClass = classNames('fc-text-input', className);

    return (
      <input
        type="text"
        className={inputClass}
        onChange={({ target }) => onChange(target.value)}
        placeholder={placeholder}
        value={value}
        {...rest} />
    );
  }
}
