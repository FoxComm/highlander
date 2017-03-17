// @flow

import React, { Element, Component, PropTypes } from 'react';
import s from './text-input.css';
import classNames from 'classnames';

type Props = {
  className?: string,
  labelClass?: string,
  errorClass?: string,
  // could be any combination of characters l, r, b, t or one of "middle-v", "middle-h", "top", "bottom", "left", "right"
  pos?: string,
  error?: boolean|string,
  type?: string,
  placeholder?: string,
  label?: ?string|Element,
  // modificators
  hasCard?: boolean,
  hasSymbol?: boolean,
}

class TextInput extends Component {
  props: Props;

  static contextTypes = {
    error: PropTypes.string,
  };

  get contextError(): ?string {
    return this.context.error;
  }

  calcPositions(position: ?string): Array<string> {
    if (!position) return [];

    switch (position) {
      case 'middle-v':
        return ['t', 'b'];
      case 'middle-h':
        return ['r', 'l'];
      case 'top':
        return ['t'];
      case 'bottom':
        return ['b'];
      case 'right':
        return ['r'];
      case 'left':
        return ['l'];
      default:
        return position.split('');
    }
  }

  get label() {
    const { props } = this;
    if (props.label) {
      if (typeof props.label === 'string') {
        return <span className={classNames(props.labelClass, s.labelText)}>{props.label}</span>;
      }
      return <span className={classNames(props.labelClass, s.labelElement)}>{props.label}</span>;
    }
  }

  render() {
    const { props } = this;

    const positions = this.calcPositions(props.pos);
    const posClassNames = positions.map(side => s[`pos-${side}`]);

    const {className, labelClass, errorClass, type = 'text', ...rest} = props;
    const error = props.error || this.contextError;

    const showSmallPlaceholder = !!props.value && props.placeholder;
    const showErrorText = error && typeof error === 'string';

    const inputClass = classNames(s.textInput, className, posClassNames, {
      [s.error]: !!error,
    });

    const blockClass = classNames(s.block, posClassNames, {
      [s.error]: !error,
      [s.hasCard]: props.hasCard,
      [s.hasSymbol]: props.hasSymbol,
      [s.hasTopMessages]: showSmallPlaceholder || showErrorText,
    });

    const content = props.children || <input className={inputClass} type={type} {...rest} />;

    const errorClassName = classNames(s.errorMessage, errorClass);

    return (
      <div className={blockClass}>
        {showSmallPlaceholder && <span className={s.placeholder}>{props.placeholder}</span>}
        {showErrorText && <span className={errorClassName}>{error}</span>}
        {content}
        {this.label}
      </div>
    );
  }
}

export default TextInput;
