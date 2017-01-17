/* @flow */

// libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

// components
import { IncrementButton, DecrementButton } from '../common/buttons';

type Props = {
  value: number|string,
  min: number,
  max: number,
  step: number,
  decreaseAction: Function,
  increaseAction: Function,
  className: string,
  disabled: boolean,
  counterId: string,
};

const Counter = (props: Props) => {
  const { decreaseAction, increaseAction, disabled, value, min, max, ...rest } = props;

  return (
    <div id={props.counterId} className="fc-input-group fc-counter">
      <div className="fc-input-prepend">
        <DecrementButton
          type="button"
          disabled={disabled || value === min}
          onClick={decreaseAction}
          className={ classNames('fc-btn-counter', 'decrement-btn') }
        />
      </div>
      <input
        id="adjust-quantity-input"
        type="number"
        value={value}
        className={classNames('fc-counter__input', props.className, {disabled})}
        disabled={disabled}
        {...rest} />
      <div className="fc-input-append">
        <IncrementButton
          type="button"
          disabled={disabled || value === max}
          onClick={increaseAction}
          className={ classNames('fc-btn-counter', 'increment-btn') }
        />
      </div>
    </div>
  );
};

Counter.defaultProps = {
  value: 1,
  step: 1,
  min: 1,
  disabled: false,
};

export default Counter;
