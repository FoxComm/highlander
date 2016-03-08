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


const Checkbox = ({inline, docked, ...props}) => {
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

Checkbox.propTypes = {
  className: PropTypes.string,
  inline: PropTypes.bool,
};

Checkbox.defaultProps = {
  inline: false,
  docked: PropTypes.oneOf([
    'left',
    'right',
  ]),
};


const HalfCheckbox = props => {
  const className = classNames(
    {'_half-checked': props.checked && props.halfChecked},
    props.className,
  );

  return (
    <Checkbox {...props}
      className={ className } />
  );
};


HalfCheckbox.propTypes = {
  className: PropTypes.string,
  halfChecked: PropTypes.bool,
  checked: PropTypes.bool,
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
