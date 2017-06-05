/* @flow */

// libs
import invariant from 'invariant';
import classNames from 'classnames';
import React from 'react';

// styles
import s from './checkbox.css';

type BaseProps = {
  /** ID for input */
  id: string,
  /** Label text */
  label: string,
  /** className */
  className?: string,
};

type CheckboxProps = BaseProps & {
  /** If checkbox is the only child of table cell and should cover the cell(e.g. `MultiSelectRow`) */
  inCell?: boolean,
};

type PartialProps = CheckboxProps & {
  /** If true - sets ("-") in checkbox  */
  halfChecked?: boolean,
}

const BaseCheckbox = (props: BaseProps) => {
  const { className, label, id, ...rest } = props;

  invariant(!!id, 'id prop should be provided for Checkbox');

  return (
    <div className={className}>
      <input type="checkbox" id={id} {...rest} />
      <label htmlFor={id} className={s.label}>{label}</label>
    </div>
  );
};

/**
 * `Checkbox`, `PartialCheckbox`, `BigCheckbox` and `SliderCheckbox` -
 * are simple components build on the top of generic `BaseCheckbox`
 * and serve to show true/false data
 *
 * [Mockups](https://zpl.io/Z39JBU)
 *
 * @function Checkbox
 */
export const Checkbox = ({ className, inCell = false, ...rest }: CheckboxProps) => {
  const cls = classNames(s.checkbox, {
    [s.inCell]: inCell,
  }, className);

  return (
    <BaseCheckbox className={cls} {...rest} />
  );
};

export const PartialCheckbox = ({ halfChecked, className = '', ...rest }: PartialProps) => {
  const cls = classNames(s.halfCheckbox, {
    [s.halfChecked]: (rest.checked || rest.defaultChecked) && halfChecked,
  }, className);

  return (
    <BaseCheckbox className={cls} {...rest} />
  );
};

export const BigCheckbox = ({ className = '', ...rest }: BaseProps) => {
  const cls = classNames(s.bigCheckbox, className);

  return (
    <BaseCheckbox className={cls} {...rest} />
  );
};

export const SliderCheckbox = ({ className = '', ...rest }: BaseProps) => {
  const cls = classNames(s.slideCheckbox, className);

  return (
    <BaseCheckbox className={cls} {...rest} />
  );
};
