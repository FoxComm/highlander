import React, { PropTypes } from 'react';
import classnames from 'classnames';
import _ from 'lodash';

const TabView = props => {
  const klass = classnames(
    'fc-tab',
     { 'fc-tab-draggable': props.draggable },
     { 'fc-tab-selected': props.selected });

   const icon = props.draggable
     ? <i className='fc-tab-icon icon-drag-drop'></i>
     : null;

  return (
    <li className={klass} onClick={props.onClick}>
      {icon}
      {props.children}
    </li>
  );
};

TabView.propTypes = {
  selector: PropTypes.string,
  children: PropTypes.node,
  draggable: PropTypes.bool,
  selected: PropTypes.bool,
  onClick: PropTypes.func
};

TabView.defaultProps = {
  draggable: true,
  onClick: _.noop,
  selected: false
};

export default TabView;
