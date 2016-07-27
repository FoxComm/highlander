/* @flow */

import React, { Element } from 'react';

type Props = {
  status: string,
  text: string,
  isOptional: bool,
};

const PanelHeader = (props: Props): Element => {
  const { status, text, isOptional } = props;

  const icon = (
    <div className={`fc-orders-panel-header__icon _${status}`}>
      <i className={`icon-${status}`} />
    </div>
  );

  const optional = isOptional
    ? <span className="fc-orders-panel-header__optional">(optional)</span>
    : null;

  return (
    <div className="fc-orders-panel-header">
      {icon}
      {text}
      {optional}
    </div>
  );
};

PanelHeader.defaultProps = {
  status: 'success',
  isOptional: false,
};

export default PanelHeader;
