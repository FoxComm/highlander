//@flow

//libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';
import _ from 'lodash';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { AddButton } from 'components/core/button';
import Criterion from './criterion-edit';

const prefixed = prefix('fc-query-builder');

// types
type Props = {
  criterions: Array<Object>,
  getCriterion: Function,
  getOperators: Function,
  getWidget: Function,
  conditions: Array<Array>,
  setConditions: Function,
  mainCondition: string,
  omitAddButton: boolean,
  itemName: string,
};

export default class QueryBuilder extends React.Component {
  props: Props;

  updateCondition(index, value) {
    const {conditions, setConditions, criterions, mainCondition, setElasticQuery} = this.props;

    const newConditions =[
      ...conditions.slice(0, index),
      value,
      ...conditions.slice(index + 1),
    ];

    setConditions(newConditions);
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
    const criterion = this.props.getCriterion(field);
    const { getDefault } = this.props.getWidget(criterion, operator);

    this.updateCondition(index, [field, operator, getDefault(criterion)]);
  }

  @autobind
  remove(index) {
    const {conditions, setConditions, criterions, mainCondition, setElasticQuery} = this.props;
    const newConditions =[
      ...conditions.slice(0, index),
      ...conditions.slice(index + 1),
    ];
    setConditions(newConditions);
  }

  @autobind
  renderCriterion([field, operator, value], index) {
    const {conditions, setConditions} = this.props;

    return (
      <Criterion
        omitDeleteIcon={this.props.omitAddButton}
        criterions={this.props.criterions}
        getCriterion={this.props.getCriterion}
        getOperators={this.props.getOperators}
        getWidget={this.props.getWidget}
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
    const {conditions,itemName} = this.props;
    const name = itemName || 'criterion';
    let button;
    if (this.props.omitAddButton) {
      button = null;
    } else {
      button = (<div className={prefixed('add-criterion')} onClick={this.addCondition}>
                  <AddButton type="button" onClick={this.addCondition} />
                  <span>Add {name}</span>
                </div>);
    }
    return (
      <div className={prefixed()}>
        <div className={prefixed('criterions')}>
          {conditions.map(this.renderCriterion)}
        </div>
        {button}
      </div>
    );
  }
}
