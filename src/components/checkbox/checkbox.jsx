'use strict';

import React from 'react';
import classNames from 'classnames';

const DefaultCheckbox = (props) => {
  const {className, ...rest} = props;
  return (
    <div className={ className }>
      <input type="checkbox" {...rest} />
      <label htmlFor={props.id}><span></span></label>
    </div>
  );
};

const SliderCheckbox = (props) => {
  return (
    <DefaultCheckbox {...props}
                     className={ classNames('fc-slide-checkbox', props.className) } />
  );
};

const Checkbox = (props) => {
  return (
    <DefaultCheckbox {...props}
                     className={ classNames('fc-checkbox', props.className) } />
  );
};

export {
  SliderCheckbox,
  Checkbox
};
