
/* @flow */

import React, { Element } from 'react';

type Props = {
  isCart: bool,
  status: string,
  text: string,
  isOptional: bool,
};

const PanelHeader = (props: Props): Element => {
  const { isCart, status, text, isOptional } = props;
  let icon = null;

  if (isCart) {
    icon = (
      <div className={`fc-orders-panel-header__icon _${status}`}>
        <i className={`icon-${status}`} />
      </div>
    );
  }

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
  isCart: false,
  status: 'success',
  isOptional: false,
};

export default PanelHeader;
