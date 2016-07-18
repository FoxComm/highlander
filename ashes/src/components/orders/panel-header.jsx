
/* @flow */

import React, { Element } from 'react';

type Props = {
  text: string,
  isOptional: bool,
};

const PanelHeader = (props: Props): Element => {
  const { text, isOptional } = props;
  const optional = isOptional
    ? <span className="fc-orders-panel-header__optional">(optional)</span>
    : null;

  return (
    <div className="fc-orders-panel-header">
      {text}
      {optional}
    </div>
  );
};

PanelHeader.defaultProps = {
  isOptional: false,
};

export default PanelHeader;
