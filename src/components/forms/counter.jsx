import React, { PropTypes } from 'react';
import { IncrementButton, DecrementButton } from '../common/buttons';

const Counter = props => {
  const {decreaseAction, increaseAction, ...rest} = props;

  return (
    <div className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton onClick={decreaseAction} className="fc-btn-counter" />
      </div>
      <input
        type="number"
        {...rest} />
      <div className="fc-input-append">
        <IncrementButton onClick={increaseAction} className="fc-btn-counter" />
      </div>
    </div>
  );
};

Counter.propTypes = {
  value: PropTypes.number,
  min: PropTypes.number,
  max: PropTypes.number,
  step: PropTypes.number,
  decreaseAction: PropTypes.func,
  increaseAction: PropTypes.func,
};

Counter.defaultProps = {
  value: 1,
  step: 1,
  min: 1,
  max: 100
};

export default Counter;
