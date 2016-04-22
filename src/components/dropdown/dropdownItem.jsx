
/* @flow */

import React, { PropTypes, Element } from 'react';

type ItemProps = {
  onSelect: Function,
  value: number|string|bool,
  children: Element,
};

const DropdownItem = (props: ItemProps) => {
  const {value, children, onSelect} = props;

  const handleClick = event => {
    event.preventDefault();
    onSelect(value, children);
  };

  return (
    <li className="fc-dropdown__item" key={value} onClick={handleClick}>
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
