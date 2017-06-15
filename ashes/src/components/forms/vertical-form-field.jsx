/* @flow */

import React from 'react';
import styles from './vertical-form-field.css';

type Props = {
  children?: any,
  controlId: string,
  label: string,
  required?: bool,
};

const VerticalFormField = ({
  children,
  controlId,
  label,
  required = false,
}: Props) => {

  const upChildren = React.Children.map(children, (child) => {
    return React.cloneElement(child, {
      id: controlId,
      required,
    });
  });

  return (
    <div styleName="field">
      <label styleName="label" htmlFor={controlId}>
        {label}
        {required && (
           <span styleName="required">&nbsp;*</span>
        )}
      </label>
      <div styleName="input-container">
        {upChildren}
      </div>
    </div>
  );
};

export default VerticalFormField;
