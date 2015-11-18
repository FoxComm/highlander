import React, { PropTypes } from 'react';
import classNames from 'classnames';

const DefaultCheckbox = props => {
  const {className, ...rest} = props;
  console.log(props);
  return (
    <div className={ className }>
      <input type="checkbox" {...rest} />
      <label htmlFor={props.id}><span></span></label>
    </div>
  );
};

DefaultCheckbox.propTypes = {
  id: PropTypes.string
};

const SliderCheckbox = props => {
  return (
    <DefaultCheckbox {...props}
                     className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

SliderCheckbox.propTypes = {
  className: PropTypes.string
};

const Checkbox = props => {
  return (
    <DefaultCheckbox {...props}
                     className={ classNames('fc-checkbox', props.className) } />
  );
};

Checkbox.propTypes = {
  className: PropTypes.string
};

export {
  SliderCheckbox,
  Checkbox
};
