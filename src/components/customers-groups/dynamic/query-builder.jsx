//libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

//data
import criterions from '../../../paragons/customer-groups/criterions';

//helpers
import { prefix } from '../../../lib/text-utils';

//components
import Criterion from './criterion-edit';
import { AddButton } from '../../common/buttons';


const prefixed = prefix('fc-customer-group-builder');

export default class QueryBuilder extends React.Component {

  static propTypes = {
    conditions: PropTypes.arrayOf(PropTypes.array).isRequired,
    setConditions: PropTypes.func.isRequired,
  };

  @autobind
  renderCriterion([field, operator, value], index) {
    const {conditions, setConditions} = this.props;

    const updateCondition = (value) => {
      setConditions([
        ...conditions.slice(0, index),
        value,
        ...conditions.slice(index + 1),
      ]);
    };
    const changeField = (field) => {
      updateCondition([field, null, null]);
    };
    const changeOperator = (operator) => {
      updateCondition([field, operator, null]);
    };
    const changeValue = (value) => {
      updateCondition([field, operator, value]);
    };
    const remove = () => {
      setConditions([
        ...conditions.slice(0, index),
        ...conditions.slice(index + 1),
      ]);
    };

    return (
      <Criterion key={index}
                 field={field}
                 operator={operator}
                 value={value}
                 changeField={changeField}
                 changeOperator={changeOperator}
                 changeValue={changeValue}
                 remove={remove} />
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
      <div className={prefixed('')}>
        <div className={prefixed('__criterions')}>
          {conditions.map(this.renderCriterion)}
        </div>
        <div className={prefixed('__add-criterion')} onClick={this.addCondition}>
          <AddButton type="button" onClick={this.addCondition} /><span>Add criteria</span>
        </div>
      </div>
    );
  }
}
