/* @flow */

// libs
import noop from 'lodash/noop';
import classNames from 'classnames';
import React, { Element } from 'react';

// styles
import s from './dropdown-item.css';

type ItemProps = {
  onSelect?: Function,
  value: number | string | boolean,
  children?: Element<any>,
  className?: string,
  isHidden?: boolean,
};

const DropdownItem = ({ children, className = '', isHidden = false, value, onSelect = noop }: ItemProps) => {
  const handleClick = event => {
    debugger;
    event.preventDefault();
    onSelect(value, children);
  };

  const classnames = classNames(className, {
    [s.item]: true,
    [s.hidden]: isHidden,
  });

  return (
    <li className={classnames} key={value} onClick={handleClick}>
      {children}
    </li>
  );
};

export default DropdownItem;
