//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';

//data
import { getCriterion, getOperators, getWidget } from 'paragons/customer-groups/criterions';

//helpers
import { prefix } from 'lib/text-utils';

const prefixed = prefix('fc-customer-group__criterion');

const Criterion = ({field, operator, value}) => {
  const criterion = getCriterion(field);
  const {Label} = getWidget(criterion, operator);
  const operators = getOperators(criterion);
  const operatorLabel = operators[operator];

  const valueLabel = React.createElement(Label, {
    criterion,
    value,
    className: 'fc-customer-group__criterion',
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
