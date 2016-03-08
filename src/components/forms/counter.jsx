
// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { IncrementButton, DecrementButton } from '../common/buttons';

const Counter = props => {
  const {decreaseAction, increaseAction, disabled, ...rest} = props;

  return (
    <div className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton disabled={disabled} onClick={decreaseAction} className="fc-btn-counter" />
      </div>
      <input
        type="number"
        className={classNames('fc-counter__input', props.className, {disabled})}
        disabled={disabled}
        {...rest} />
      <div className="fc-input-append">
        <IncrementButton disabled={disabled} onClick={increaseAction} className="fc-btn-counter" />
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
  className: PropTypes.string,
};

Counter.defaultProps = {
  value: 1,
  step: 1,
  min: 1,
  max: 100
};

export default Counter;
