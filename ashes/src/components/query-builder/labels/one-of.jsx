//libs
import React from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from 'lib/text-utils';

//components
import propTypes from '../widgets/propTypes';

export const Label = ({Label: Widget}) => ({criterion, value, className}) => {
  const values = value || [null];
  const prefixed = prefix(prefix(className)('vertical-select'));

  return (
    <div className={classNames('fc-grid', prefixed())}>
      {values.map((item, index) => renderLabelItem({
        Widget,
        criterion,
        values,
        index,
        className,
        prefixed,
      }))}
    </div>
  );
};
Label.propTypes = propTypes;

const renderLabelItem = ({Widget, criterion, values, index, className, prefixed}) => {
  return (
    <div className={prefixed('container')} key={index}>
      <div className={prefixed('item')}>
        {React.createElement(Widget, ({
          criterion,
          value: values[index],
          className,
        }))}
      </div>
      {index < values.length - 1 ? <div className={prefixed('or')}>or</div> : null}
    </div>
  );
};
