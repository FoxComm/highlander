
import React from 'react';
import styles from './css/input.css';

export const TextInput = props => {
  // deprecated: use ui/forms/text-input instead
  return <input styleName="text-input" type="text" {...props} />;
};

export const TextInputWithLabel = props => {
  const {label, children, ...rest} = props;

  const content = children || <TextInput styleName="text-input" {...rest} />;

  return (
    <div styleName="input-with-label">
      {content}
      {label && <span styleName="label">{label}</span>}
    </div>
  );
};
