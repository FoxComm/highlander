'use strict';

import React from 'react';

function composeClassName(props, defaultPart) {
  let additionalClass = props.className ? props.className : '';
  return `${defaultPart} ${additionalClass}`.trim();
}

const GenericCheckbox = (props) => {
  return (
    <div className={ props.className }>
      <input type="checkbox"
             defaultChecked={ props.defaultChecked }
             id={ props.id }
             name={ props.name } />
      <label htmlFor={props.id}></label>
    </div>
  );
}

const SliderCheckbox = (props) => {
  let composedClassName = composeClassName(props, 'fc-slide-checkbox');
  return (
    <GenericCheckbox {...props} className={ composedClassName } />
  );
};

const Checkbox = (props) => {
  let composedClassName = composeClassName(props, 'fc-checkbox');
  return (
    <GenericCheckbox {...props} className={ composedClassName } />
  );
}

export {
  SliderCheckbox,
  Checkbox
};
