/* @flow */

import React, { PropTypes, Element } from 'react';
import classNames from 'classnames';

type ItemProps = {
  onSelect: Function,
  value: number|string|bool,
  children: Element,
  className: string,
  isHidden: bool,
};

const DropdownItem = (props: ItemProps) => {
  const {value, children, onSelect} = props;

  const handleClick = event => {
    event.preventDefault();
    onSelect(value, children);
  };

  const classnames = classNames(props.className, {
    'fc-dropdown__item': true,
    'fc-dropdown__item-hidden': props.isHidden,
  });

  return (
    <li className={classnames} key={value} onClick={handleClick}>
      {children}
    </li>
  );
};

DropdownItem.propTypes = {
  onSelect: PropTypes.func,
  value: PropTypes.oneOfType([
    PropTypes.bool,
    PropTypes.string,
    PropTypes.number,
  ]),
  children: PropTypes.node,
};

DropdownItem.defaultProps = {
  onSelect: () => {},
};

export default DropdownItem;
