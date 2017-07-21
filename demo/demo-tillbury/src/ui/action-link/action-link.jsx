/* @flow */

import React from 'react';
import Icon from 'ui/icon';

import styles from './action-link.css';

type Props = {
  action: Function,
  title?: string,
  icon?: ?Object,
  children?: any,
  hoverAction?: boolean,
};

const ActionLink = (props: Props) => {
  const { action, hoverAction, title, icon, ...rest } = props;
  const actionIcon = icon ? <Icon {...icon} /> : null;

  return (
    <span styleName="action-link" onClick={action} onMouseLeave={hoverAction} onMouseEnter={hoverAction} {...rest}>
      {actionIcon}
      {title}
      {props.children}
    </span>
  );
};

export default ActionLink;
