/**
 * @flow
 */

import React, { Component, Element } from 'react';

type Props = {
  hexCode: string,
};

export default class SwatchDisplay extends Component<void, Props, void> {
  props: Props;

  render() {
    const hexCode = this.props.hexCode.toUpperCase();
    const colorStyle = {
      background: `#${hexCode}`,
    };

    return (
      <div className="fc-swatch-display">
        <div className="fc-swatch-display__code"># {hexCode}</div>
        <div className="fc-swatch-display__color" style={colorStyle}></div>
      </div>
    );
  }
}
