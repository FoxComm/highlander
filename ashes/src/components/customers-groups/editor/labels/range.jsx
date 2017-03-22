//libs
import React from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Label = ({Label: Widget}) => ({criterion, value, className}) => {
  const prefixed = prefix(className);
  const values = value || [null, null];

  return (
    <div className={prefixed('range')}>
      {Widget({criterion, value: values[0], className})}
      <span className={classNames(prefixed('range__separator'), 'icon-minus', 'fc-align-center')}/>
      {Widget({criterion, value: values[1], className})}
    </div>
  );
};

Label.propTypes = propTypes;
