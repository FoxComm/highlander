import React, { PropTypes } from 'react';
import classNames from 'classnames';

const MenuItem = props => {
  const { className, ...rest } = props;
  const klass = classNames('fc-menu-item', className);

  return (
    <li className={klass} {...rest}>
      {props.children}
    </li>
  );
};

MenuItem.propTypes = {
  className: PropTypes.string,
  children: PropTypes.node.isRequired,
  isActive: PropTypes.bool,
  isFirst: PropTypes.bool
};

export default MenuItem;
