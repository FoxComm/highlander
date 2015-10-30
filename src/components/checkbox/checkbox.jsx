'use strict';

import React from 'react';
import classNames from 'classnames';

const GenericCheckbox = (props) => {
  return (
    <div className={ props.className }>
      <input type="checkbox"
             defaultChecked={ props.defaultChecked }
             id={ props.id }
             name={ props.name } />
      <label htmlFor={props.id}><span></span></label>
    </div>
  );
};

const SliderCheckbox = (props) => {
  return (
    <GenericCheckbox {...props}
                     className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

const Checkbox = (props) => {
  return (
    <GenericCheckbox {...props}
                     className={ classNames('fc-checkbox', props.className) } />
  );
};

export {
  SliderCheckbox,
  Checkbox
};
