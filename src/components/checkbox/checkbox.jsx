import React, { PropTypes } from 'react';
import classNames from 'classnames';

const DefaultCheckbox = props => {
  const {className, ...rest} = props;
  return (
    <div className={ className }>
      <input type="checkbox" {...rest} />
      <label htmlFor={props.id}><span></span></label>
    </div>
  );
};

DefaultCheckbox.propTypes = {
  id: PropTypes.string,
  className: PropTypes.string,
};

const SliderCheckbox = props => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

SliderCheckbox.propTypes = {
  className: PropTypes.string,
};

const Checkbox = props => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames('fc-checkbox', props.className) } />
  );
};

Checkbox.propTypes = {
  className: PropTypes.string,
};

const HalfCheckbox = props => {
  const className = classNames(
    'fc-checkbox',
    {'_half-checked': props.checked && props.halfChecked},
    props.className,
  );

  return (
    <DefaultCheckbox {...props}
      className={ className } />
  );
};

HalfCheckbox.propTypes = {
  className: PropTypes.string,
  halfChecked: PropTypes.bool,
};

HalfCheckbox.defaultProps = {
  halfChecked: false,
};

export {
  SliderCheckbox,
  Checkbox,
  HalfCheckbox,
};
