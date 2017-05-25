/* @flow */

// libs
import React from 'react';

type Props = {
  /** Button content (label) */
  children?: Element<any>
}

const RadioButton = (props: Props) => {
  const { children, ...rest } = props;

  return (
    <div className="fc-form-field fc-radio">
      <input type="radio" {...rest} />
      {children}
    </div>
  );
};

export default RadioButton;
