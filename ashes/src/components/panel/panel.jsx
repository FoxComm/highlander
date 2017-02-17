/* @flow */

import classNames from 'classnames';
import React, { PropTypes } from 'react';

type Props = {
  title?: Element<*>|string;
  featured?: boolean;
  className?: string;
  children?: Element<*>;
};

const Panel = ({ title, featured, className, children }: Props) => (
  <div className={classNames('fc-panel', className)}>
    <div className="fc-panel-header">
      {title}
    </div>
    <div className={classNames('fc-panel-content', { 'fc-panel-content-featured': featured })}>
      {children}
    </div>
  </div>
);

export default Panel;
