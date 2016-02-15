
import React from 'react';
import styles from './css/input.css';
import cssModules from 'react-css-modules';

export const TextInput = cssModules(props => {
  return <input styleName="text-input" type="text" {...props} />;
}, styles);
