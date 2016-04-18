import React, { PropTypes } from 'react';
import Transition from 'react-addons-css-transition-group';

const Menu = props => {
  const { position, isOpen, animate, children } = props;

  const transitionProps = {
    component: 'div',
    className: 'fc-menu',
    transitionName: `menu-transition-${position}`,
    transitionEnter: animate,
    transitionLeave: animate,
    transitionEnterTimeout: 300,
    transitionLeaveTimeout: 300,
  };

  return (
    <Transition {...transitionProps}>
      {isOpen &&
      <ul className='fc-menu-items'>{children}</ul>
      }
    </Transition>

  );
};

Menu.propTypes = {
  children: PropTypes.node,
  position: PropTypes.oneOf(['left', 'right', 'center']),
  isOpen: PropTypes.bool,
  animate: PropTypes.bool,
};

Menu.defaultProps = {
  position: 'left',
  isOpen: false,
  animate: true,
};

export default Menu;
