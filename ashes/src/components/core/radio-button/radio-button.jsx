/* @flow */

// libs
import invariant from 'invariant';
import classNames from 'classnames';
import React from 'react';

// styles
import s from './radio-button.css';

type Props = {
  /** ID for input and label */
  id: string,
  /** RadioButton label */
  label: string,
  /** Additional className */
  className?: string,
};

/**
 * RadioButton component is a simple radio-button with optional content
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @function RadioButton
 */

const RadioButton = (props: Props) => {
  const { id, label, className, ...rest } = props;

  invariant(!!id, 'id prop should be provided for RadioButton');

  return (
    <div className={classNames(s.radio, className)}>
      <input type="radio" id={id} {...rest} />
      <label htmlFor={id} className={s.label}>{label}</label>
    </div>
  );
};

export default RadioButton;
