/* @flow */

import React from 'react';
import classNames from 'classnames';

import styles from './sort-pill.css';

type Props = {
  field: string,
  direction: number,
  isActive: boolean,
  onClick?: Function,
};

const SortPill = (props: Props) => {
  const icon = props.direction === -1 ? '▲' : '▼';
  const classes = classNames(styles.pill, {
    [styles['_is-active']]: props.isActive,
    [styles['_is-inactive']]: !props.isActive,
  });

  return (
    <div className={classes} onClick={props.onClick}>
      {props.field}
      &nbsp;
      {props.isActive && <span styleName="pill-chevron">{icon}</span>}
    </div>
  );
};

SortPill.defaultProps = {
  isActive: false,
};

export default SortPill;
