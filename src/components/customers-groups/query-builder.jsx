//libs
import React, { PropTypes } from 'react';
import { autobind } from 'core-decorators';

//data
import criterions from '../../modules/customer-groups/criterions';

//components
import Criterion from './criterion';
import { AddButton } from '../common/buttons';

export default class QueryBuilder extends React.Component {

  static props = {
    conditions: PropTypes.arrayOf(PropTypes.object).isRequired,
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
      updateCondition([field, operator, value]);
    };
    const changeOperator = (operator) => {
      updateCondition([field, operator, value]);
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
      <div className='fc-group-builder'>
        <div className='fc-grid fc-group-builder-criterions'>
          {conditions.map(this.renderCriterion)}
        </div>
        <div className='fc-group-builder-add-criterion' onClick={this.addCondition}>
          <AddButton type="button" onClick={this.addCondition} /><span>Add criteria</span>
        </div>
      </div>
    );
  }
}
