'use strict';

import React, { PropTypes } from 'react';

const DropdownItem = props => {
  return (
    <div className="fc-dropdown-item" key={props.value} onClick={props.onClick}>
      {props.children}
    </div>
  );
};

DropdownItem.propTypes = {
  onClick: PropTypes.func,
  value: PropTypes.string,
  children: PropTypes.node
};

export default DropdownItem;