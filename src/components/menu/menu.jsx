import React, { PropTypes } from 'react';

const Menu = props => {
  return (
    <div className='fc-menu'>
      <ul className='fc-menu-items'>
        {props.children}
      </ul>
    </div>
  );
};

Menu.propTypes = {
  children: PropTypes.node
};

export default Menu;
