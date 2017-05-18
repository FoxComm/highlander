//libs
import _ from 'lodash';
import React, { Component, PropTypes } from 'react';
import classNames from 'classnames';
import { autobind } from 'core-decorators';

//data
import criterions, { getCriterion, getOperators, getWidget } from 'paragons/query-builder/promotions-criterions';
import operators from 'paragons/query-builder/operators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Dropdown, DropdownItem } from 'components/dropdown';

type Props = {
  criterions: Array<Object>,
  getCriterion: Function,
  getOperators: Function,
  getWidget: Function,
  index: number,
  field: string,
  operator: string,
  mainCondition: string,
  conditionsLength: number,
  value: any,
  changeField: Function,
  changeOperator: Function,
  changeValue: Function,
  remove: Function,
  omitDeleteIcon: boolean,
};

const prefixed = prefix('fc-query-builder');
const fields = criterions.map(({ field,label }) => [ field, label ]);

class Criterion extends Component {
  props: Props;

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

    if (this.props.mainCondition == operators.and) {
      if (this.props.index == 0) return (<span className={prefixed('prefix')}></span>);
      return (<span className={prefixed('prefix')}>and</span>);
    } else if (this.props.mainCondition == operators.or) {
      if (this.props.index == 0) return (<span className={prefixed('prefix')}></span>);
      return (<span className={prefixed('prefix')}>or</span>);
    }
  }

  render() {
    const {
            field,
            operator,
            value,
            changeField,
            changeOperator,
            changeValue,
            remove,
            criterions,
            getCriterion,
            getOperators,
            getWidget,
            omitDeleteIcon,
          } = this.props;
    const criterion = getCriterion(field);
    const fields = criterions.map(({ field,label }) => [ field, label ]);
    const delIcon = (<i onClick={remove} className={classNames(prefixed('remove-criterion'), 'icon-close')} />);
    const deleteButton = omitDeleteIcon ? null : delIcon;
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
        {renderOperator(criterion, operator, changeOperator, getOperators)}
        {renderValue(criterion, operator, value, changeValue, getWidget)}
        {deleteButton}
      </div>
    );
  }
}

const renderOperator = (criterion, operator, changeOperator, getOperators) => {
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

const renderValue = (criterion, operator, value, changeValue, getWidget) => {
  if (!criterion || !operator) {
    return null;
  }

  const {Input, getDefault} = getWidget(criterion, operator);

  return React.createElement(Input, {
    criterion,
    value,
    changeValue,
    className: 'fc-query-builder',
  });
};

export default Criterion;
