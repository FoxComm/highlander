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
      stroke: '#3a434f',
      fill: '#3a434f',
    };
    const toolTipStyle = {
      fill: '#FFFFFF',
      fontSize: 6
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
