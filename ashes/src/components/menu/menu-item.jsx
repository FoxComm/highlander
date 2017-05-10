import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

const MenuItem = props => {
  const { className, clickAction, ...rest } = props;
  const klass = classNames('fc-menu-item', className);

  const click = event => {
    event.preventDefault();
    event.stopPropagation();
    clickAction(event);
  };

  const preventClickSelection = event => {
    event.preventDefault();
    event.stopPropagation();
  };

  return (
    <li
      className={klass}
      onClick={click}
      onMouseDown={preventClickSelection}
      onMouseUp={preventClickSelection}
      {...rest}
    >
      {props.children}
    </li>
  );
};

MenuItem.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  isActive: PropTypes.bool,
  isFirst: PropTypes.bool,
  clickAction: PropTypes.func,
};

export default MenuItem;
