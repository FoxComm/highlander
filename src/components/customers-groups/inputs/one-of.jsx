//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//components
import { Button } from '../../common/buttons';


const Input = Widget => ({criterion, value, prefixed, changeValue}) => {
  const values = _.isEmpty(value) ? [null] : value;
  return (
    <div className={classNames('fc-grid', prefixed('vertical-select'))}>
      {values.map((item, index) => renderItem({
        Widget,
        criterion,
        values,
        index,
        prefixed,
        changeValue,
      }))}
    </div>
  );
};

const renderItem = ({Widget, criterion, values, index, prefixed, changeValue}) => {
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
    <div className={prefixed('vertical-select__container')} key={index}>
      <div className={prefixed('vertical-select__item')}>
        {Widget({criterion, value: values[index], prefixed, changeValue: change})}
      </div>
      {renderNodeOrAdd(prefixed, index < values.length - 1, add)}
      <i className={classNames(prefixed('vertical-select__remove'), 'icon-close')} onClick={remove} />
    </div>
  );
};

const renderNodeOrAdd = (prefixed, isNode, add) => {
  if (isNode) {
    return <div className={prefixed('vertical-select__or')}>or</div>;
  }

  return <Button className={classNames(prefixed('vertical-select__add'), 'icon-add')} onClick={add} />;
};

Input.propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.arrayOf(PropTypes.any),
  changeValue: PropTypes.func.isRequired,
};

export default Input;
