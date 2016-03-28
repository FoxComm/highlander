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


const prefixed = prefix('fc-customer-group-dynamic__criterion');

const Criterion = ({field, operator, value}) => {
  const criterion = _.find(criterions, criterion => criterion.field === field);
  const {Label} = operator in criterion.input ? criterion.input[operator] : criterion.input.default;

  const fieldLabel = _.find(criterions, criterion => criterion.field === field).label;

  const availableOperators = criterion.operators
    ? _.pick(criterion.type.operators, criterion.operators)
    : criterion.type.operators;
  const operatorLabel = availableOperators[operator];

  const valueLabel = React.createElement(Label, {
    criterion,
    value,
    prefixed: prefix(prefixed()),
  });

  return (
    <div className={classNames('fc-grid', prefixed())}>
      <div className={prefixed('field')}>{fieldLabel}</div>
      <div className={prefixed('operator')}>{operatorLabel}</div>
      {valueLabel}
    </div>
  );
};

Criterion.propTypes = {
  field: PropTypes.string,
  operator: PropTypes.string,
  value: PropTypes.any,
};

export default Criterion;
