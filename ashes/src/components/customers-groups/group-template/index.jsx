/* @flow */

import classNames from 'classnames';
import React from 'react';

import styles from './styles.css';

type Props = {
  id?: number,
  name: string,
  icon?: string,
  onClick: (id?: number) => any,
};

export default ({ id, name, onClick, icon = '' }: Props) => (
  <button className={styles.template} onClick={() => onClick(id)}>
    {icon && <i className={classNames(styles.icon, `icon-${icon}`)} />}
    {name}
  </button>
);
