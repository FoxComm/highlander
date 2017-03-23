/* @flow */

import React from 'react';
import Icon from 'ui/icon';

import styles from './action-link.css';

type Props = {
  action: Function,
  title: string,
  icon?: ?Object,
  children: any,
};

const ActionLink = (props: Props) => {
  const { action, title, icon, ...rest } = props;
  const actionIcon = icon
    ?
    <Icon
      className={icon.className}
      name={icon.name}
    />
    :
    null
  ;

  return (
    <span styleName="action-link" onClick={action} {...rest}>
      {actionIcon}
      {title}
      {props.children}
    </span>
  );
};

export default ActionLink;
