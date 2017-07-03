/* @flow */

// libs
import React from 'react';

// components
import Icon from 'components/core/icon';

// styles
import styles from './panel-header.css';

type Props = {
  text: string,
  isOptional?: boolean,
  showStatus?: boolean,
  status?: string,
};

const PanelHeader = ({ isOptional = false, showStatus = false, status = '', text }: Props) => {
  const optional = isOptional ? <span styleName="optional">(optional)</span> : null;

  let icon = null;
  if (showStatus) {
    const styleName = `icon-${status}`;
    const iconStyleName = `icon-background-${status}`;
    icon = (
      <div styleName={styleName}>
        <Icon styleName={iconStyleName} name={status} />
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
