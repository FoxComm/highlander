//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import FormField from '../../forms/formfield';
import classNames from 'classnames';

//data
import criterions from '../../../paragons/customer-groups/criterions';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import { Dropdown, DropdownItem } from '../../dropdown';


const prefixed = prefix('fc-customer-group-builder');

const fields = criterions.map(({field,label}) => [field, label]);

const Criterion = ({field, operator, value, changeField, changeOperator, changeValue, remove}) => {
  const criterion = _.find(criterions, {field : field});

  return (
    <div className={classNames('fc-grid', prefixed('criterion'))}>
      <Dropdown items={fields}
                className={prefixed('field')}
                placeholder='- Select criteria -'
                value={field}
                onChange={changeField} />
      {renderOperator(criterion, operator, changeOperator)}
      {renderValue(criterion, operator, value, changeValue)}
      <i onClick={remove} className={classNames(prefixed('remove-criterion'), 'icon-close')} />
    </div>
  );
};

const renderOperator = (criterion, operator, changeOperator) => {
  if (!criterion) {
    return null;
  }

  const availableOperators = criterion.operators
    ? _.pick(criterion.type.operators, criterion.operators)
    : criterion.type.operators;

  const operators = _.map(availableOperators, (label, operator) => [operator, label]);

  return (
    <Dropdown items={operators}
              className={prefixed('operator')}
              placeholder='- Select operator -'
              value={operator}
              onChange={changeOperator} />
  );
};

const renderValue = (criterion, operator, value, changeValue) => {
  if (!criterion || !operator) {
    return null;
  }

  const {Input} = _.get(criterion.widget, operator, criterion.widget.default);

  return React.createElement(Input, {
    criterion,
    value,
    changeValue,
    prefixed: prefix('fc-customer-group-builder'),
  });
};

Criterion.propTypes = {
  field: PropTypes.string,
  operator: PropTypes.string,
  value: PropTypes.any,
  changeField: PropTypes.func.isRequired,
  changeOperator: PropTypes.func.isRequired,
  changeValue: PropTypes.func.isRequired,
  remove: PropTypes.func.isRequired,
};

export default Criterion;
