/* @flow */

import classNames from 'classnames';
import React, { Element } from 'react';

type PanelListProps = {
  className?: string;
  children?: Element;
}

export const PanelList = ({ className, children }: PanelListProps) => (
  <div className={classNames('fc-panel-list', className)}>
    {children}
  </div>
);

type PanelListItemProps = {
  title: Element;
  children: Element;
}

export const PanelListItem = ({ title, children }: PanelListItemProps) => (
  <div className="fc-panel-list-panel">
    <div className="fc-panel-list-header">
      {title}
    </div>
    <div className="fc-panel-list-content">
      {children}
    </div>
  </div>
);
