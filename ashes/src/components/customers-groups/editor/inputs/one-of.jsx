//libs
import React from 'react';
import classNames from 'classnames';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Button } from 'components/common/buttons';
import propTypes from '../widgets/propTypes';


export const Input = ({Input: Widget}) => ({criterion, value, className, changeValue}) => {
  const values = value || [null];
  const prefixed = prefix(prefix(className)('vertical-select'));

  return (
    <div className={classNames('fc-grid', prefixed())}>
      {values.map((item, index) => renderInputItem({
        Widget,
        criterion,
        values,
        index,
        className,
        changeValue,
        prefixed,
      }))}
    </div>
  );
};
Input.propTypes = propTypes;

const renderInputItem = ({Widget, criterion, values, index, className, changeValue, prefixed}) => {
  const value = values[index];

  const add = () => {
    changeValue([
      ...values,
      null,
    ]);
  };
  const change = (value) => {
    changeValue([
      ...values.slice(0, index),
      value,
      ...values.slice(index + 1),
    ]);
  };
  const remove = () => {
    changeValue([
      ...values.slice(0, index),
      ...values.slice(index + 1),
    ]);
  };

  return (
    <div className={prefixed('container')} key={index}>
      <div className={prefixed('item')}>
        {React.createElement(Widget, ({
          criterion,
          value: values[index],
          className,
          changeValue: change,
        }))}
      </div>
      {renderNodeOrAdd(prefixed, index < values.length - 1, add)}
      <i className={classNames(prefixed('remove'), 'icon-close')} onClick={remove} />
    </div>
  );
};

const renderNodeOrAdd = (prefixed, isNode, add) => {
  if (isNode) {
    return <div className={prefixed('or')}>or</div>;
  }

  return <div className={classNames(prefixed('add'), 'icon-add')} onClick={add} />;
};

export const getDefault = Widget => criterion => [Widget.getDefault(criterion)];

export const isValid = Widget => (value, criterion) => value.every(part => Widget.isValid(part, criterion));
