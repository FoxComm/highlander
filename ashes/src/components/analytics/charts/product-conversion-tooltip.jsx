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

const DeltaFlyout = ({x, y, offsetX, offsetY, strokeColor, fillColor}) => {

  const badgeStyle = (x, y, offsetX, offsetY, strokeColor, fillColor) => {
    const trnslate = `translate(${x + offsetX}, ${y + offsetY}) scale(0.425)`;
    return (
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
        <g transform={trnslate}>
          <polygon stroke={strokeColor} fill={fillColor} points="0 1.17774105e-16 2.75821033e-15 45.2614326 66.25 45.2607422 80.8210449 22.6307163 66.2856445 0"></polygon>
          <path d="M2.07990973e-15,34.5 L2.75821033e-15,45.6307163 L66.25,45.6300259 L80.5832702,23.3692837 L0,23.3692837 L0,34.5 Z" stroke={strokeColor} fill="#F7F7F7"></path>
        </g>
      </g>
    );
  };

  return badgeStyle(x, y, offsetX, offsetY, strokeColor, fillColor);
};

const ConversionLabelComponent = (props) => {
  const { x, y, datum, textStyle } = props;
  const textPosition = { x: x - 7, y: y + 2 };

  const arrowDirection = (parseFloat(datum.conversion) > 0) ? 'rotate(90.0)' : 'rotate(-90.0)';
  // public/admin/images/arrow-black.svg
  const arrowCopy = (
    <svg width="11px" height="10px" viewBox="0 0 11 10" x={textPosition.x - 6} y={textPosition.y - 4.5}>
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd" transform="scale(0.45)">
        <g transform="translate(-271.000000, -457.000000)" fill="#000000">
          <g transform="translate(261.000000, 450.000000)">
            <g transform={`translate(15.000000, 12.000000) ${arrowDirection} translate(-15.000000, -12.000000) translate(10.000000, 6.500000)`}>
              <path d="M9.83251172,4.78074136 C9.83251172,4.32606946 9.53192543,3.92488837 9.05098735,3.92488837 L4.34848175,3.92488837 L6.30563252,1.96578736 C6.46594521,1.80531492 6.55946094,1.58466532 6.55946094,1.35732937 C6.55946094,1.12999341 6.46594521,0.909343812 6.30563252,0.748871375 L5.80465536,0.254081359 C5.64434267,0.0936089218 5.43059241,3.55271368e-15 5.20348277,3.55271368e-15 C4.97637312,3.55271368e-15 4.75594317,0.0936089218 4.59563048,0.254081359 L0.247148732,4.60020987 C0.0935157365,4.76068231 -2.13162821e-14,4.98133191 -2.13162821e-14,5.20866786 C-2.13162821e-14,5.43600382 0.0935157365,5.65665342 0.247148732,5.81043951 L4.59563048,10.1699407 C4.75594317,10.3237268 4.97637312,10.4173357 5.20348277,10.4173357 C5.43059241,10.4173357 5.65102236,10.3237268 5.80465536,10.1699407 L6.30563252,9.661778 C6.46594521,9.50799192 6.55946094,9.28734232 6.55946094,9.06000636 C6.55946094,8.83267041 6.46594521,8.61202081 6.30563252,8.45823472 L4.34848175,6.49244736 L9.05098735,6.49244736 C9.53192543,6.49244736 9.83251172,6.09126627 9.83251172,5.63659436 L9.83251172,4.78074136 Z" id="Icons/Arrow-left"></path>
            </g>
          </g>
        </g>
      </g>
    </svg>
  );

  return (
    <g>
      {arrowCopy}
      <text x={textPosition.x} y={textPosition.y} style={textStyle}>
        {Math.abs(datum.conversion)}%
      </text>
    </g>
  );
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

const ConversionLabelComponent = (props) => {
  const { x, y, datum, textStyle } = props;
  const textPosition = { x: x - 7, y: y + 2};

  const arrowDirection = (parseFloat(datum.conversion) > 0) ? "rotate(90.0)" : "rotate(-90.0)";
  // public/admin/images/arrow-black.svg
  const arrowCopy = (
    <svg width="11px" height="10px" viewBox="0 0 11 10" x={textPosition.x - 6} y={textPosition.y - 4}>
      <g stroke="none" strokeWidth="1" fill="none" fillRule="evenodd" transform="scale(0.45)">
        <g transform="translate(-271.000000, -457.000000)" fill="#000000">
          <g transform="translate(261.000000, 450.000000)">
            <g transform={`translate(15.000000, 12.000000) ${arrowDirection} translate(-15.000000, -12.000000) translate(10.000000, 6.500000)`}>
              <path d="M9.83251172,4.78074136 C9.83251172,4.32606946 9.53192543,3.92488837 9.05098735,3.92488837 L4.34848175,3.92488837 L6.30563252,1.96578736 C6.46594521,1.80531492 6.55946094,1.58466532 6.55946094,1.35732937 C6.55946094,1.12999341 6.46594521,0.909343812 6.30563252,0.748871375 L5.80465536,0.254081359 C5.64434267,0.0936089218 5.43059241,3.55271368e-15 5.20348277,3.55271368e-15 C4.97637312,3.55271368e-15 4.75594317,0.0936089218 4.59563048,0.254081359 L0.247148732,4.60020987 C0.0935157365,4.76068231 -2.13162821e-14,4.98133191 -2.13162821e-14,5.20866786 C-2.13162821e-14,5.43600382 0.0935157365,5.65665342 0.247148732,5.81043951 L4.59563048,10.1699407 C4.75594317,10.3237268 4.97637312,10.4173357 5.20348277,10.4173357 C5.43059241,10.4173357 5.65102236,10.3237268 5.80465536,10.1699407 L6.30563252,9.661778 C6.46594521,9.50799192 6.55946094,9.28734232 6.55946094,9.06000636 C6.55946094,8.83267041 6.46594521,8.61202081 6.30563252,8.45823472 L4.34848175,6.49244736 L9.05098735,6.49244736 C9.53192543,6.49244736 9.83251172,6.09126627 9.83251172,5.63659436 L9.83251172,4.78074136 Z" id="Icons/Arrow-left"></path>
            </g>
          </g>
        </g>
      </g>
    </svg>
  );

  return (
    <g>
      {arrowCopy}
      <text x={textPosition.x} y={textPosition.y} style={textStyle}>
        {Math.abs(datum.conversion)}%
      </text>
    </g>
  );
};

export default class ProductConversionToolTip extends React.Component {

  static defaultEvents = VictoryTooltip.defaultEvents;

  static defaultProps = {
    dy: -10,
  };

  get tooltips() {
    const {
      dy,
      datum,
      toolTipColor,
      getDelta, getConversion,
      deltaToolTipCoordinates, deltaToolTipStyles,
      conversionToolTipOffsetCoordinates, conversionToolTipStyles
    } = this.props;

    const toolTipFlyoutStyle = {
      opacity: 0,
    };
    const toolTipStyle = {
      fontSize: 8,
      fill: toolTipColor,
    };

    const deltaToolTipStyle = {
      fill: deltaToolTipStyles.textColor,
      fontSize: deltaToolTipStyles.fontSize,
    };

    const conversionToolTipStyle = {
      fill: conversionToolTipStyles.textColor,
      fontSize: conversionToolTipStyles.fontSize,
    };

    const deltaValue = getDelta(datum);
    const conversionValue = getConversion(datum);

    const deltaToolTipPosition = {
      dx: deltaToolTipCoordinates.x,
      y: deltaToolTipCoordinates.y,
    };

    const deltaToolTip = !_.isNil(deltaValue) ?
      (
        <g>
          <VictoryTooltip
            {...this.props}
            cornerRadius={0}
            flyoutComponent={
              <DeltaFlyout
                offsetX={deltaToolTipCoordinates.offsetX}
                offsetY={deltaToolTipCoordinates.offsetY}
                fillColor={deltaToolTipStyles.fill}
                strokeColor={deltaToolTipStyles.borderColor} />
            }
            style={deltaToolTipStyle}
            pointerLength={7}
            pointerWidth={19}
            dx={deltaToolTipPosition.dx}
            y={deltaToolTipPosition.y}
            text={`${deltaValue} %`}
            orientation="left"
            labelComponent={
              <VictoryLabel 
                dx={deltaToolTipCoordinates.deltaTextDx}
                dy={deltaToolTipCoordinates.deltaTextDy} />
            }
            active={true}
            renderInPortal={false}
          />
          <VictoryTooltip
            {...this.props}
            flyoutStyle={toolTipFlyoutStyle}
            style={conversionToolTipStyle}
            dx={deltaToolTipPosition.dx + conversionToolTipOffsetCoordinates.dx}
            y={deltaToolTipPosition.y + conversionToolTipOffsetCoordinates.y}
            text={`${conversionValue}%`}
            labelComponent={
              <ConversionLabelComponent
                textStyle={conversionToolTipStyle}
              />
            }
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
          dy={dy}
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
