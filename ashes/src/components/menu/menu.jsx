import React from 'react';
import PropTypes from 'prop-types';
import Transition from 'react-transition-group/CSSTransitionGroup';

const Menu = props => {
  const { position, isOpen, children } = props;

  const transitionProps = {
    component: 'div',
    className: 'fc-menu',
    transitionName: `dd-transition-${position}`,
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
};

Menu.defaultProps = {
  position: 'left',
  isOpen: false,
};

export default Menu;
