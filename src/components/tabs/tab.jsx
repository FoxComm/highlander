import React, { PropTypes } from 'react';
import classnames from 'classnames';

const TabView = props => {
  const klass = classnames(
    'fc-tab',
     { 'fc-tab-draggable': props.draggable },
     { 'fc-tab-selected': props.selected });

   const icon = props.draggable
     ? <i className='fc-tab-icon icon-drag-drop'></i>
     : null;

  return (
    <li className={klass}>
      {icon}
      {props.children}
    </li>
  );
};

TabView.propTypes = {
  selector: PropTypes.string,
  children: PropTypes.node,
  draggable: PropTypes.bool,
  selected: PropTypes.bool
};

TabView.defaultProps = {
  draggable: true,
  selected: false
};

export default TabView;
