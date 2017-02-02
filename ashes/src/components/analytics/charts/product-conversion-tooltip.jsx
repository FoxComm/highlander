// @flow

// libs
import React from 'react';
import _ from 'lodash';

// components
import { VictoryTooltip, VictoryLabel } from 'victory';

type Props = {
  barWidth: number,
  getDelta: Function,
  getConversion: Function,
  datum?: any,
};

const DeltaFlyout = ({x, y}) => {

  const badgeStyle = (x, y) => {
    const trnslate = `translate(${x - 54}, ${y + 4}) scale(0.425)`;
    return (
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform={trnslate}>
          <polygon
            stroke="#363636"
            fill="#3A4350"
            points="0 1.17774105e-16 2.75821033e-15 45.2614326 66.25 45.2607422 80.8210449 22.6307163 66.2856445 0">
          </polygon>
          <path
            d="M2.07990973e-15,34.5 L2.75821033e-15,45.6307163 L66.25,45.6300259 L80.5832702,23.3692837 L0,23.3692837 L0,34.5 Z"
            stroke="#3A4350"
            fill="#F7F7F7">
          </path>
        </g>
      </g>
    );
  };

  return badgeStyle(x, y);
};

export default class ProductConversionToolTip extends React.Component {

  static defaultEvents = VictoryTooltip.defaultEvents;

  props: Props;

  get tooltips() {
    const { barWidth, getDelta, datum, getConversion } = this.props;

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
    const conversionToolTipStyle = {
      fill: '#3A4350',
      fontSize: 6,
    };

    const deltaValue = getDelta(datum);
    const conversionValue = getConversion(datum);

    const deltaToolTipPosition = {
      dx: barWidth * 1.3,
      y: 180,
    };

    const deltaToolTip = (deltaValue > 0) ?
      (
        <g>
          <VictoryTooltip
            {...this.props}
            cornerRadius={0}
            flyoutStyle={deltaToolTipFlyoutStyle}
            flyoutComponent={<DeltaFlyout />}
            style={deltaToolTipStyle}
            pointerLength={7}
            pointerWidth={19}
            dx={deltaToolTipPosition.dx}
            y={deltaToolTipPosition.y}
            text={`${deltaValue}%`}
            orientation="left"
            labelComponent={
              <VictoryLabel 
                dy={1.4} 
                dx={2} 
              />
            }
            active={true}
            renderInPortal={false}
            />
          <VictoryTooltip
            {...this.props}
            flyoutStyle={toolTipFlyoutStyle}
            style={conversionToolTipStyle}
            dx={deltaToolTipPosition.dx - 56}
            y={deltaToolTipPosition.y + 36}
            text={`${conversionValue}%`}
            active={true}
            renderInPortal={false}
            />
        </g>
      )
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
