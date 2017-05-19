/* @flow */

// libs
import classNames from 'classnames';
import React from 'react';

type Props = {
  id: string,
  className?: string,
  children?: any,
  inline?: boolean,
  docked?: string,
  halfChecked?: boolean,
  checked?: boolean,
}

const DefaultCheckbox = (props: Props)=> {
  const { className, children, id, ...rest } = props;

  const label = children ? <span className="fc-checkbox__label">{children}</span> : null;

  return (
    <div className={ className }>
      <input type="checkbox" id={id} {...rest} />
      <label htmlFor={id}>
        {label}
      </label>
    </div>
  );
};

const SliderCheckbox = (props: Props) => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

const Checkbox = ({inline, docked, ...props}: Props) => {
  const className = classNames(
    'fc-checkbox',
    {'_inline': inline},
    {'_docked-left': docked && docked === 'left'},
    {'_docked-right': docked && docked === 'right'},
    props.className,
  );

  return (
    <DefaultCheckbox {...props}
      className={ className } />
  );
};

Checkbox.defaultProps = {
  inline: false,
  docked: 'left'
};


const HalfCheckbox = (props: Props)=> {
  const { halfChecked, ...rest } = props;
  const className = classNames(
    {'_half-checked': props.checked && halfChecked},
    props.className,
  );

  return (
    <Checkbox {...rest} className={ className } />
  );
};

HalfCheckbox.defaultProps = {
  halfChecked: false,
  checked: false,
};


export {
  SliderCheckbox,
  Checkbox,
  HalfCheckbox,
};
