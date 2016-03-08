import React, { PropTypes } from 'react';
import _ from 'lodash';
import { connect } from 'react-redux';
import { assoc, get } from 'sprout-data';
import Dropdown from '../dropdown/dropdown';
import { SliderCheckbox } from '../checkbox/checkbox';
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
    terms: Dropdown.itemsType, // [value, title]
    term: PropTypes.string,
    value: PropTypes.object.isRequired, // {type: :widget-type, other fields, like :options}
    operators: PropTypes.object, // {value -> title}
    operator: PropTypes.string,
    changeTerm: PropTypes.func.isRequired,
    changeOperator: PropTypes.func.isRequired,
    removeCriterion: PropTypes.func.isRequired,
    changeValue: PropTypes.func
  };

  get operator() {
    const {value, operators, operator, changeOperator } = this.props;
    if (!value.type) return;

    return (
      <Dropdown
        className='fc-group-builder-crit-op'
        items={ _.map(operators, (title, operator) => [operator, title]) }
        value={ operator }
        onChange={ changeOperator }
      />
    );
  }

  get value() {
    if (!this.props.value.type) {
      return;
    }

    switch(this.props.value.type) {
      case 'date':
        return <DatePicker onClick={this.props.changeValue}/>;
      case 'number':
        return (<FormField><input onChange={({target}) => this.props.changeValue(target.value)}
                                  type="number"/></FormField>);
      case 'currency':
        return <CurrencyInput onChange={this.props.changeValue} value={this.props.value.value}/>;
      case 'bool':
      case 'bool_inverted':
        return (<SliderCheckbox onChange={({target}) => this.props.changeValue(target.checked)}
                                checked={this.props.value.value}/>);
      case 'enum':
        return (<SelectVertical options={this.props.value.options}
          {...this.props.value.props}
          onChange={this.props.changeValue}/>);
    }
  }

  render() {
    return (
      <div className='fc-grid fc-group-builder-criterion'>
        <Dropdown
          items={ this.props.terms }
          className='fc-group-builder-crit-term'
          placeholder='- Select criteria -'
          value={this.props.term}
          onChange={ this.props.changeTerm }
        />
        {this.operator}
        <span className='fc-group-builder-crit-val'>{this.value}</span>
        <i onClick={this.props.removeCriterion} className='fc-group-builder-remove-crit icon-close'/>
      </div>
    );
  }
}
