// @flow

// libs
import React from 'react';
import _ from 'lodash';

// components
import { VictoryTooltip, VictoryLabel } from 'victory';

type Props = {
  barWidth: number,
  getDelta: Function,
  datum: Object,
};

export default class ProductConversionToolTip extends React.Component {

  static defaultEvents = VictoryTooltip.defaultEvents;

  props: Props;

  get tooltips() {
    const { barWidth, getDelta, datum } = this.props;

    const toolTipFlyoutStyle = {
      opacity: 0,
    };
    const toolTipStyle = {
      fontSize: 6,
    };

    const deltaToolTipFlyoutStyle = {
      stroke: '#363636',
      fill: '#3A4350',
    };
    const deltaToolTipStyle = {
      fill: '#FFFFFF',
      fontSize: 6,
    };
    const deltaValue = getDelta(datum);

    const deltaToolTip = (!_.isNil(deltaValue) && deltaValue > 0) ?
      (<VictoryTooltip
        {...this.props}
        cornerRadius={0}
        flyoutStyle={deltaToolTipFlyoutStyle}
        style={deltaToolTipStyle}
        pointerLength={7}
        pointerWidth={19}
        dx={barWidth * 1.5}
        y={180}
        text={`${deltaValue} %`}
        orientation="left"
        labelComponent = {
          <VictoryLabel />
        }
        active={true}
        renderInPortal={false}
      />)
      : false;

    return (
      <g>
        <VictoryTooltip
          {...this.props}
          flyoutStyle={toolTipFlyoutStyle}
          style={toolTipStyle}
          pointerLength={0}
          dy={-10}
          active={true}
          renderInPortal={false}
        />
        {deltaToolTip}
      </g>
    );
  }

  render() {
    return this.tooltips;
  }
}
