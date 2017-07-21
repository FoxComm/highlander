/* @flow */

// libs
import React, { Element } from 'react';

// styles
import styles from './radiobutton.css';

type Props = {
  id: string|number,
  children?: Element<*>|string,
  className?: string,
};

const Radiobutton = (props: Props) => {
  const { children, className, ...rest } = props;
  return (
    <div styleName="radiobutton" className={className}>
      <input styleName="input" type="radio" {...rest} />
      <label styleName="label" htmlFor={props.id}>{children}</label>
    </div>
  );
};

export default Radiobutton;
