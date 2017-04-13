//libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

//data
import criterions, { getCriterion, getOperators, getWidget } from 'paragons/promotions-query-builder/criterions';
import operators from 'paragons/promotions-query-builder/operators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Dropdown, DropdownItem } from 'components/dropdown';

const prefixed = prefix('fc-customer-group-builder');
const fields = criterions.map(({ field,label }) => [ field, label ]);

class Criterion extends Component {

  shouldComponentUpdate(nextProps, nextState) {
    const { field, operator, value, conditionsLength, mainCondition } = this.props;

    return nextProps.field != field ||
            nextProps.conditionsLength != conditionsLength ||
            nextProps.operator != operator ||
            nextProps.mainCondition != mainCondition ||
            nextProps.value != value;
  }

  @autobind
  renderAndOrLabel() {
    if (this.props.conditionsLength == 1) return null;
    if(this.props.mainCondition == operators.and) {
      if (this.props.index == 0) return(<span className={prefixed('prefix')}></span>);
      return(<span className={prefixed('prefix')}>and</span>);
    } else if(this.props.mainCondition == operators.or) {
      if (this.props.index == 0) return(<span className={prefixed('prefix')}></span>);
      return(<span className={prefixed('prefix')}>or</span>);
    }
  }

  render() {
    const { field, operator, value, changeField, changeOperator, changeValue, remove } = this.props;
    const criterion = getCriterion(field);

    return (
      <div className={prefixed('criterion')}>
        {this.renderAndOrLabel()}
        <Dropdown
          items={fields}
          className={prefixed('field')}
          placeholder='- Select criteria -'
          value={field}
          onChange={changeField}
        />
        {renderOperator(criterion, operator, changeOperator)}
        {renderValue(criterion, operator, value, changeValue)}
        <i onClick={remove} className={classNames(prefixed('remove-criterion'), 'icon-close')} />
      </div>
    );
  }
};

const renderOperator = (criterion, operator, changeOperator) => {
  if (!criterion) {
    return null;
  }

  const operators = _.map(getOperators(criterion), (label, operator) => [operator, label]);

  return (
    <Dropdown
      items={operators}
      className={prefixed('operator')}
      placeholder='- Select operator -'
      value={operator}
      onChange={changeOperator}
    />
  );
};

const renderValue = (criterion, operator, value, changeValue) => {
  if (!criterion || !operator) {
    return null;
  }

  const {Input, getDefault} = getWidget(criterion, operator);

  return React.createElement(Input, {
    criterion,
    value,
    changeValue,
    className: 'fc-customer-group-builder',
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
