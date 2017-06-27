// @flow

// libs
import React from 'react';

// components
import { VictoryTooltip } from 'victory';

export default class TotalRevenueToolTip extends React.Component {
  static defaultEvents = VictoryTooltip.defaultEvents;

  render() {
    const toolTipCornerRadius = () => {
      return 0;
    };
    const toolTipFlyoutStyle = {
      stroke: '#25334a',
      fill: '#25334a',
    };
    const toolTipStyle = {
      fill: '#ffffff',
      fontSize: 6,
    };

    return (
      <g>
        <VictoryTooltip
          {...this.props}
          pointerLength={2}
          pointerWidth={4}
          cornerRadius={toolTipCornerRadius}
          style={toolTipStyle}
          flyoutStyle={toolTipFlyoutStyle}
          orientation="right"
          dx={4}
          dy={-10}
          renderInPortal={false}
        />
      </g>
    );
  }
}
