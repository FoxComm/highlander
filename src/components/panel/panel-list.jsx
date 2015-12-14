import React, { PropTypes } from 'react';

export const PanelList = props => {
  const classList = `${props.className ? props.className : ''} fc-panel-list`;
  return (
    <div className={classList}>
      {props.children}
    </div>
  );
};

PanelList.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string
};

export const PanelListItem = props => {
  return (
    <div className="fc-panel-list-panel">
      <div className="fc-panel-list-header">
        {props.title}
      </div>
      <div className="fc-panel-list-content">
        {props.content && props.content.props.children}
        {props.children}
      </div>
    </div>
  );
};

PanelListItem.propTypes = {
  children: PropTypes.node,
  content: PropTypes.node,
  title: PropTypes.node
};
