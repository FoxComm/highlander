import React, { PropTypes } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { assoc, get } from 'sprout-data';
import Dropdown from '../dropdown/dropdown';
import CurrencyInput from '../forms/currency-input';
import DatePicker from '../datepicker/datepicker';
import SelectVertical from './select-vertical';
import FormField from '../forms/formfield';
import * as GroupBuilderActions from '../../modules/groups/builder';

function mapDispatchToProps(dispatch, props) {
  return _.transform(GroupBuilderActions, (result, action, key) => {
    result[key] = (...args) => {
      return dispatch(action(props.id, ...args));
    };
  });
}

@connect((state, props) => state.groups.builder.criterions[props.id], mapDispatchToProps)
export default class Criterion extends React.Component {

  static propTypes = {
    id: PropTypes.string.isRequired,
    terms: PropTypes.object.isRequired, // {value -> title}
    selectedTerm: PropTypes.string,
    value: PropTypes.object.isRequired, // {type: :widget-type, other fields, like :options}
    operators: PropTypes.object, // {value -> title}
    selectedOperator: PropTypes.string,
    changeTerm: PropTypes.func.isRequired,
    changeOperator: PropTypes.func.isRequired,
    removeCriterion: PropTypes.func.isRequired,
    changeValue: PropTypes.func
  };

  get operator() {
    if (!this.props.value.type) {
      return;
    }
    return (
      <Dropdown
        className='fc-group-builder-crit-op'
        items={ this.props.operators }
        value={ this.props.selectedOperator }
        onChange={ this.props.changeOperator }
      />
    );
  }

  get value() {
    if (!this.props.value.type) {
      return;
    }

    switch(this.props.value.type) {
      case 'date':
        return <DatePicker onChange={({target}) => this.props.changeValue(target.value)}/>;
      case 'number':
        return (<FormField><input onChange={({target}) => this.props.changeValue(target.value)}
                                  type="number"/></FormField>);
      case 'currency':
        return <CurrencyInput onChange={this.props.changeValue} value={this.props.value.value}/>;
      case 'bool':
        return <Dropdown onChange={this.props.changeValue}
                         value={this.props.value.value} items={{t: 'Yes', f: 'No'}}/>;
      case 'enum':
        return <SelectVertical options={this.props.value.options}
          onChange={this.props.changeValue}/>;
    }
  }

  render() {
    return (
      <div className='fc-grid fc-group-builder-criterion'>
        <Dropdown
          items={ this.props.terms }
          className='fc-group-builder-crit-term'
          placeholder='- Select criteria -'
          value={this.props.selectedTerm}
          onChange={ this.props.changeTerm }
        />
        {this.operator}
        <span className='fc-group-builder-crit-val'>{this.value}</span>
        <i onClick={this.props.removeCriterion} className='fc-group-builder-remove-crit icon-close'/>
      </div>
    );
  }
}