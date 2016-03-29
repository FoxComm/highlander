//libs
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import FormField from '../../forms/formfield';
import classNames from 'classnames';

//data
import { getCriterion, getOperators, getWidget } from '../../../paragons/customer-groups/criterions';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import { Dropdown, DropdownItem } from '../../dropdown';


const prefixed = prefix('fc-customer-group-dynamic__criterion');

const Criterion = ({field, operator, value}) => {
  const criterion = getCriterion(field);
  const {Label} = getWidget(criterion, operator);
  const operators = getOperators(criterion);
  const operatorLabel = operators[operator];

  const valueLabel = React.createElement(Label, {
    criterion,
    value,
    className: 'fc-customer-group-dynamic__criterion',
  });

  return (
    <div className={classNames('fc-grid', prefixed())}>
      <div className={prefixed('field')}>{criterion.label}</div>
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
