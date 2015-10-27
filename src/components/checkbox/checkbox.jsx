'use strict';

import React from 'react';

const SliderCheckbox = (props) => {
  let additionalClass = props.className ? props.className : '';
  let containerClassName = `fc-slide-checkbox ${additionalClass}`.trim();
  return (
    <div className={containerClassName}>
      <input type="checkbox" defaultChecked={ props.defaultChecked } id={props.id} />
      <label htmlFor={props.id}></label>
    </div>
  );
};

export {
  SliderCheckbox
};
