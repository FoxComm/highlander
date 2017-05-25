/* @flow */

// libs
import React from 'react';
import classNames from 'classnames';

// styles
import s from './radio-button.css'

type Props = {
  /** Button content (label) */
  children?: Element<any>
}

const RadioButton = (props: Props) => {
  const { children, ...rest } = props;

  return (
    <div className={ classNames(s.radio, props.className) }>
      <input type="radio" {...rest} />
      {children}
    </div>
  );
};

export default RadioButton;
