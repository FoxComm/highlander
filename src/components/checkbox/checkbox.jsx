'use strict';

import React from 'react';

function composeClassName(props, defaultPart) {
  const additionalClass = props.className ? props.className : '';
  return `${defaultPart} ${additionalClass}`.trim();
}

const GenericCheckbox = (props) => {
  return (
    <div className={ props.className }>
      <input type="checkbox"
             defaultChecked={ props.defaultChecked }
             id={ props.id }
             name={ props.name } />
      <label htmlFor={props.id}><span><span></span></span></label>
    </div>
  );
};

const SliderCheckbox = (props) => {
  const composedClassName = composeClassName(props, 'fc-slide-checkbox');
  return (
    <GenericCheckbox {...props} className={ composedClassName } />
  );
};

const Checkbox = (props) => {
  const composedClassName = composeClassName(props, 'fc-checkbox');
  return (
    <GenericCheckbox {...props} className={ composedClassName } />
  );
};

export {
  SliderCheckbox,
  Checkbox
};
