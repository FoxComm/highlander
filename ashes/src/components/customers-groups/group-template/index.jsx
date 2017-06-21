/* @flow */

// libs
import React from 'react';

// components
import Icon from 'components/core/icon';

// styles
import styles from './styles.css';

type Props = {
  id?: number,
  name: string,
  icon?: string,
  onClick: (id?: number) => any,
};

export default ({ id, name, onClick, icon = '' }: Props) =>
  <button className={styles.template} onClick={() => onClick(id)}>
    {icon && <Icon className={styles.icon} name={icon} />}
    {name}
  </button>;
