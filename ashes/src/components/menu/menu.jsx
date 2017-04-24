import React, { PropTypes } from 'react';
import Transition from 'react-transition-group/CSSTransitionGroup';

const Menu = props => {
  const { position, isOpen, animate, children } = props;

  const transitionProps = {
    component: 'div',
    className: 'fc-menu',
    transitionName: `dd-transition-${position}`,
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
  position: PropTypes.oneOf(['left', 'center', 'right']),
  isOpen: PropTypes.bool,
  animate: PropTypes.bool,
};

Menu.defaultProps = {
  position: 'left',
  isOpen: false,
  animate: true,
};

export default Menu;
