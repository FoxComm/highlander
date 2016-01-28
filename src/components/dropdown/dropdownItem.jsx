import React, { PropTypes } from 'react';

const DropdownItem = props => {
  return (
    <li className="fc-dropdown__item" key={props.value} onClick={props.onClick}>
      {props.children}
    </li>
  );
};

DropdownItem.propTypes = {
  onClick: PropTypes.func,
  value: PropTypes.string,
  children: PropTypes.node
};

export default DropdownItem;
