//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from '../../../../lib/text-utils';

//components
import propTypes from '../widgets/propTypes';


export const Label = ({Label: Widget}) => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null];
  const widgetPrefixed = prefixed;
  prefixed = prefix(prefixed('vertical-select'));

  return (
    <div className={classNames('fc-grid', prefixed())}>
      {values.map((item, index) => renderLabelItem({
        Widget,
        criterion,
        values,
        index,
        prefixed,
        widgetPrefixed,
        changeValue,
      }))}
    </div>
  );
};
Label.propTypes = propTypes;

const renderLabelItem = ({Widget, criterion, values, index, widgetPrefixed, prefixed}) => {
  return (
    <div className={prefixed('container')} key={index}>
      <div className={prefixed('item')}>
        {React.createElement(Widget, ({
          criterion,
          value: values[index],
          prefixed: widgetPrefixed
        }))}
      </div>
      {index < values.length - 1 ? <div className={prefixed('or')}>or</div> : null}
    </div>
  );
};
