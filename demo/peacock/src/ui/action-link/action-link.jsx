/* @flow */

import React from 'react';
import Icon from 'ui/icon';

import type { HTMLElement } from 'types';

import styles from './action-link.css';

type Props = {
  action: Function,
  title: string,
  icon?: Object,
}

const ActionLink = (props: Props) => {
  const { action, title, icon, ...rest } = props
  const actionIcon = icon
    ?
    <Icon
      className={icon.className}
      name={icon.name}
    />
    :
    null
  ;

  return(
    <span styleName="action-link" onClick={action} {...rest}>
      {actionIcon}
      {title}
    </span>
  );
};

export default ActionLink;
