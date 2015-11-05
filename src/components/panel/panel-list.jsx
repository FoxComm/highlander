'use strict';

import React from 'react';

export const PanelList = props => {
  const classList = `${props.className ? props.className : ''} fc-panel-list`;
  return (
    <div className={classList}>
      {props.children}
    </div>
  );
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
