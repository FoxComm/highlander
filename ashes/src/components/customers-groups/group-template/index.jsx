/* @flow */

import classNames from 'classnames';
import React from 'react';

import styles from './styles.css';

type Props = {
  id: number;
  name: string;
  onClick: (id: number) => Promise;
};

export default (props: Props) => (
  <button className={styles.template} onClick={() => props.onClick(props.id)}>
    {props.icon && <i className={classNames(styles.icon, `icon-${props.icon}`)} />}
    {props.name}
  </button>
);
