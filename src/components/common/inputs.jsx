
import React from 'react';
import inputStyles from './css/input.css';
import inputWithLabelStyles from './css/input-with-label.css';
import cssModules from 'react-css-modules';

export const TextInput = cssModules(props => {
  return <input styleName="text-input" type="text" {...props} />;
}, inputStyles);

export const TextInputWithLabel = cssModules(props => {
  const {label, ...rest} = props;

  return (
    <div styleName="block">
      <TextInput styleName="text-input" {...rest} />
      <span styleName="label">{label}</span>
    </div>
  );
}, inputWithLabelStyles);
