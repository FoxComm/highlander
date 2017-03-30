/* @flow */

// libs
import React, { Element } from 'react';

// styles
import styles from './checkbox.css';

type CheckboxProps = {
  id: string|number,
  children?: Element<*>|string,
  className?: string,
};

const Checkbox = (props: CheckboxProps) => {
  const { children, className, ...rest } = props;
  return (
    <div className={className}>
      <input type="checkbox" styleName="checkbox" {...rest} />
      <label htmlFor={props.id}>
        <span styleName="box" />
        {children}
      </label>
    </div>
  );
};

export default Checkbox;
