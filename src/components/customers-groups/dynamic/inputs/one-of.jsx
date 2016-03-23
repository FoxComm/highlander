//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//components
import { Button } from '../../../common/buttons';


const propTypes = {
  criterion: PropTypes.shape({
    field: PropTypes.string.isRequired,
  }).isRequired,
  prefixed: PropTypes.func.isRequired,
  value: PropTypes.arrayOf(PropTypes.any),
  changeValue: PropTypes.func.isRequired,
};

const Input = ({Input: Widget}) => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null];
  return (
    <div className={classNames('fc-grid', prefixed('vertical-select'))}>
      {values.map((item, index) => renderInputItem({
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
Input.propTypes = propTypes;

const renderInputItem = ({Widget, criterion, values, index, prefixed, changeValue}) => {
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
        {React.createElement(Widget, ({criterion, value: values[index], prefixed, changeValue: change}))}
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

const Label = ({Label: Widget}) => ({criterion, value, prefixed, changeValue}) => {
  const values = value || [null];
  return (
    <div className={classNames('fc-grid', prefixed('vertical-select'))}>
      {values.map((item, index) => renderLabelItem({
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
Label.propTypes = propTypes;

const renderLabelItem = ({Widget, criterion, values, index, prefixed}) => {
  return (
    <div className={prefixed('vertical-select__container')} key={index}>
      <div className={prefixed('vertical-select__item')}>
        {React.createElement(Widget, ({criterion, value: values[index], prefixed}))}
      </div>
      {index < values.length - 1 ? <div className={prefixed('vertical-select__or')}>or</div> : null}
    </div>
  );
};

export default function(Widget) {
  return {
    Input: Input(Widget),
    Label: Label(Widget)
  };
}
