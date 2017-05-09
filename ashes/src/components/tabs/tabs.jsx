import classNames from 'classnames';
import React from 'react';
import PropTypes from 'prop-types';

const TabListView = props => {
  const cls = classNames('fc-tab-list', { '_loading': props.isLoading });

  return (
    <div className={cls}>
      <ul className="fc-tab-list__current-tabs">
        {props.children}
      </ul>
      <div className="fc-tab-list__buffer"></div>
    </div>
  );
};

TabListView.propTypes = {
  isLoading: PropTypes.bool,
  children: PropTypes.node,
};

export default TabListView;
