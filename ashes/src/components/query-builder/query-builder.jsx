//libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

//data
import criterions, { getCriterion, getWidget } from 'paragons/promotions-query-builder/criterions';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { AddButton } from 'components/common/buttons';
import Criterion from './criterion-edit';


const prefixed = prefix('fc-customer-group-builder');

export default class QueryBuilder extends React.Component {

  static propTypes = {
    conditions: PropTypes.arrayOf(PropTypes.array).isRequired,
    setConditions: PropTypes.func.isRequired,
    mainCondition: PropTypes.string.isRequired,
  };

  updateCondition(index, value) {
    const {conditions, setConditions} = this.props;

    setConditions([
      ...conditions.slice(0, index),
      value,
      ...conditions.slice(index + 1),
    ]);
  }

  @autobind
  changeField(index, field) {
    this.updateCondition(index, [field, null, null]);
  }

  @autobind
  changeValue(index, field, operator, value) {
    this.updateCondition(index, [field, operator, value]);
  }

  @autobind
  changeOperator(index, field, operator) {
    const criterion = getCriterion(field);
    const { getDefault } = getWidget(criterion, operator);

    this.updateCondition(index, [field, operator, getDefault(criterion)]);
  }

  @autobind
  remove(index) {
    const {conditions, setConditions} = this.props;

    setConditions([
      ...conditions.slice(0, index),
      ...conditions.slice(index + 1),
    ]);
  }

  @autobind
  renderCriterion([field, operator, value], index) {
    const {conditions, setConditions} = this.props;

    return (
      <Criterion
        key={`${field}.${operator}.${index}`}
        index={index}
        field={field}
        operator={operator}
        mainCondition={this.props.mainCondition}
        conditionsLength={this.props.conditions.length}
        value={value}
        changeField={field => this.changeField(index, field)}
        changeOperator={op => this.changeOperator(index, field, op)}
        changeValue={v => this.changeValue(index, field, operator, v)}
        remove={() => this.remove(index)}
      />
    );
  }

  @autobind
  addCondition(event) {
    const {conditions, setConditions} = this.props;
    event.stopPropagation();
    setConditions([
      ...conditions,
      [null, null, null]
    ]);
  }

  render() {
    const {conditions} = this.props;
    return (
      <div className={prefixed()}>
        <div className={prefixed('criterions')}>
          {conditions.map(this.renderCriterion)}
        </div>
        <div className={prefixed('add-criterion')} onClick={this.addCondition}>
          <AddButton type="button" onClick={this.addCondition} />
          <span>Add criteria</span>
        </div>
      </div>
    );
  }
}
