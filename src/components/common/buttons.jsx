
import React, { PropTypes } from 'react';
import styles from './css/buttons.css';
import cssModules from 'react-css-modules';
import Icon from 'react-evil-icons';

const Button = props => {
  let icon = null;
  if (props.icon) {
    icon = <Icon name={props.icon} size="" />;
  }

  return (
    <button styleName="button" {...props}>
      {icon}
      {props.children}
    </button>
  );
};

Button.propTypes = {
  children: PropTypes.node,
  icon: PropTypes.string,
};

export default cssModules(Button, styles);
