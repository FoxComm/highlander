// libs
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import _ from 'lodash';

// components
import Icon from 'components/core/icon';

const TabView = props => {
  const klass = classnames('fc-tab', props.className, {
    '_draggable': props.draggable,
    '_selected': props.selected
  });

  const icon = props.draggable
    ? <Icon className='fc-tab__icon' name="drag-drop" />
    : null;

  return (
    <li className={klass} onClick={props.onClick}>
      {icon}
      {props.children}
    </li>
  );
};

TabView.propTypes = {
  className: PropTypes.string,
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
