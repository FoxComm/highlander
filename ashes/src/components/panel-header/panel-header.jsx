
/* @flow */

import React, { Element } from 'react';
import styles from './panel-header.css';

type Props = {
  text: string,
  isOptional?: bool,
  showStatus?: bool,
  status?: string,
};

const PanelHeader = ({isOptional = false, showStatus = false, status, text}: Props): Element => {
  const optional = isOptional
    ? <span styleName="optional">(optional)</span>
    : null;

  let icon = null;
  if (showStatus) {
    const styleName = `icon-${status}`;
    const iconStyleName = `icon-background-${status}`;
    icon = (
      <div styleName={styleName}>
        <i styleName={iconStyleName} className={styleName} />
      </div>
    );
  }

  return (
    <div styleName="header">
      {icon}
      <div styleName="text">
        {text}
        {optional}
      </div>
    </div>
  );
};

export default PanelHeader;
