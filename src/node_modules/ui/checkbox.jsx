
/* @flow */

import React from 'react';
import styles from './css/checkbox.css';

import type { HTMLElement } from 'types';

type CheckboxProps = {
  id: string|number;
  children?: HTMLElement|string;
  className?: string;
};

const Checkbox = (props: CheckboxProps) => {
  const { children, className, ...rest } = props;
  return (
    <div className={className}>
      <input type="checkbox" styleName="checkbox" {...rest} />
      <label htmlFor={props.id}>{children}</label>
    </div>
  );
};

export default Checkbox;
