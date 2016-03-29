//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//components
import propTypes from '../widgets/propTypes';


export const Label = ({Label: Widget}) => ({criterion, value, prefixed}) => {
  const values = value || [null, null];

  return (
    <div className={prefixed('range')}>
      {Widget({criterion, value: values[0], prefixed})}
      <span className={classNames(prefixed('range__separator'), 'icon-minus', 'fc-align-center')}/>
      {Widget({criterion, value: values[1], prefixed})}
    </div>
  );
};
Label.propTypes = propTypes;
