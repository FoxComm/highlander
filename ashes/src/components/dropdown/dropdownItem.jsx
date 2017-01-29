/* @flow */

import React, { PropTypes, Element } from 'react';
import classNames from 'classnames';

type ItemProps = {
  onSelect?: Function,
  value: number|string|bool,
  children?: Element,
  className?: string,
  isHidden?: bool,
};

const DropdownItem = ({
  children, 
  className, 
  isHidden = false, 
  value, 
  onSelect = () => {},
}: ItemProps) => {
  const handleClick = event => {
    event.preventDefault();
    onSelect(value, children);
  };

  const classnames = classNames(className, {
    'fc-dropdown__item': true,
    'fc-dropdown__item-hidden': isHidden,
  });

  return (
    <li className={classnames} key={value} onClick={handleClick}>
      {children}
    </li>
  );
};

export default DropdownItem;
