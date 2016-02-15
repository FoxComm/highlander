
import React, { PropTypes } from 'react';
import styles from './css/buttons.css';
import cssModules from 'react-css-modules';

const Button = props => <button styleName="button" {...props}>{props.children}</button>;

Button.propTypes = {
  children: PropTypes.node,
};

export default cssModules(Button, styles);
