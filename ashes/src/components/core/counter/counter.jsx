/* @flow */

// libs
import React, { Component } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

// components
import { IncrementButton, DecrementButton } from 'components/core/button';

// styles
import s from './counter.css';

type Props = {
  value: number,
  min: number,
  max: number,
  step: number,
  className?: string,
  disabled?: boolean,
  counterId?: string,
  onChange: (value: number) => void
};

class Counter extends Component {
  props: Props;

  static defaultProps: $Shape<Props> = {
    value: 0,
    min: 0,
    max: 100,
    step: 1,
    disabled: false,
  };

  @autobind
  setValue(value: number) {
    const { min, max, onChange } = this.props;

    if (!value || value < min) {
      return onChange(min);
    }

    if (value > max) {
      return onChange(max);
    }

    return onChange(value);
  }

  @autobind
  handleChange({ target }: { target: HTMLInputElement }) {
    return this.setValue(parseInt(target.value, 10));
  }

  render() {
    const {
      value,
      min,
      max,
      step,
      disabled,
      counterId,
      className,
      onChange,
      ...rest,
    } = this.props;

    return (
      <div id={counterId} className={ classNames(s.counter, className)}>
        <DecrementButton
          type="button"
          disabled={disabled || value === min}
          onClick={() => this.setValue(value - step)}
          className={s.controls}
        />
        <input
          type="number"
          value={value}
          disabled={disabled}
          className={classNames(s.input, '__cssmodules')}
          onChange={this.handleChange}
          {...rest}
        />
        <IncrementButton
          type="button"
          disabled={disabled || value === max}
          onClick={() => this.setValue(value + step)}
          className={s.controls}
        />
      </div>
    );
  }
}

export default Counter;
