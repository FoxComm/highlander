// @flow

// libs
import React, { PropTypes } from 'react';
import _ from 'lodash';

// styles
import styles from './trend-button.css';

// types
type Trend = {
  index: number,
  style: string,
}

type Props = {
  trendType: Trend,
  message: string,
  value: number,
}

export const TrendType = {
  gain: {
    index: 1,
    style: 'gain',
  },
  loss: {
    index: 2,
    style: 'loss',
  },
  steady: {
    index: 3,
    style: 'steady',
  },
};

const TrendButton = (props: Props) => {

  const { trendType, message, value } = props;
  const contentBody = `${value}${message}`;

  let arrow = {};
  arrow.direction = _.find([TrendType.gain, TrendType.steady], trendType)
    ? 'up' : 'down';
  arrow.color = _.find([TrendType.steady], trendType)
    ? 'black' : 'white';

  return(
    <div styleName={`trend-button-container-${trendType.style}`}>
      <p styleName={`trend-button-content-${trendType.style}`}>
        <img
          styleName={`trend-button-arrow-${arrow.direction}`}
          src={`/admin/images/arrow-${arrow.color}.svg`}
        />
        {contentBody}
      </p>
    </div>
  );
};

TrendButton.propTypes = {
  trendType: PropTypes.shape({
    index: PropTypes.number,
    style: PropTypes.string,
  }),
  message: PropTypes.string,
  value: PropTypes.number,
};

TrendButton.defaultProps = {
  value: 0,
  message: '% from average',
};

export default TrendButton;
