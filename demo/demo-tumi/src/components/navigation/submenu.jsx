// @flow

import React from 'react';
import classnames from 'classnames';
import styles from './navigation.css';

type Props = {
  className?: string,
  open?: boolean,
  search?: boolean,
}

const Submenu = (props: Props) => {
  const className = classnames(styles.submenu, {
    [styles.open]: props.open,
    [styles.search]: props.search,
  });
  return (
    <div className={className}>{props.children}</div>
  );
};

export default Submenu;
