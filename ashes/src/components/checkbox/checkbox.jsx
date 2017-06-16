
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

const DefaultCheckbox = props => {
  const { className, children, id, ...rest } = props;

  const label = children != null ? <span className="fc-checkbox__label">{children}</span> : null;

  return (
    <div className={ className }>
      <input type="checkbox" id={id} {...rest} />
      <label htmlFor={id}>
        {label}
      </label>
    </div>
  );
};

DefaultCheckbox.propTypes = {
  id: PropTypes.string.isRequired,
  className: PropTypes.string,
  children: PropTypes.node,
};


const SliderCheckbox = props => {
  return (
    <DefaultCheckbox {...props}
      className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

SliderCheckbox.propTypes = {
  id: PropTypes.string.isRequired,
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
  id: PropTypes.string.isRequired,
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
  const { halfChecked, ...rest } = props;
  const className = classNames(
    {'_half-checked': props.checked && halfChecked},
    props.className,
  );

  return (
    <Checkbox {...rest} className={ className } />
  );
};


HalfCheckbox.propTypes = {
  id: PropTypes.string.isRequired,
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
