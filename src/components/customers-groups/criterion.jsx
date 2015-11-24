import React, { PropTypes } from 'react';
import _ from 'lodash';
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { AddButton } from '../common/buttons';
import { Link } from '../link';
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { assoc } from 'sprout-data';


export default class Criterion extends React.Component {
  static propTypes = {
    terms: PropTypes.array.isRequired,
    criterions: PropTypes.array.isRequired,
    onTermChange: PropTypes.func,
    initialTerm: PropTypes.any
  };

  typeOperators = {
    bool: {
      is: null
    },
    date: {
      'is after': null,
      'is before': null
    },
    number: {
      'is greater than': null,
      'is less than': null
    },
    enum: {
      'is one of': null,
      'is not one of': null
    }
  };

  constructor(props, ...args) {
    super(props, ...args);
    // type,
    // suggestions: PropTypes.array from static

    let items = {};
    if (props.initialTerm) {
      items[props.initialTerm] = props.initialTerm;
    }

    this.state = {
      term: props.initialTerm,
      initial: items,
      current: _.chain(props.criterions)
        .pick(({term}) => term == props.initialTerm)
        .values().first().value()
    };
  }

  @autobind
  onTermChange(newTerm) {
    if (this.props.onTermChange) {
      this.props.onTermChange(this.state.term, newTerm);
    }
    this.setState({
      term: newTerm,
      initial: assoc({}, newTerm, newTerm),
      current: _.chain(this.props.criterions)
        .pick(({term}) => term == newTerm)
        .values().first().value()
    });
  }

  get operator() {
    if (!this.state.current) {
      return;
    }
    const operators = this.typeOperators[this.state.current.type];
    if (!operators) {
      console.error(`operator ${this.state.current.type} isn't supported`);
      return;
    }
    const items = _.chain(operators).keys().reduce((p, k) => assoc(p, k, k), {}).value();
    return (
      <Dropdown
        items={items}
        value={_.first(_.keys(operators))}
      />
    );
  }

  get value() {
    if (!this.state.current) {
      return;
    }
    return (
      <input type='text'/>
    );
  }

  render() {
    const value = () => {
      return (
        <div>{val}</div>
      );
    };
    return (
      <div className='fc-group-builder-criterion'>
        <Dropdown
          items={ this.props.terms.reduce( (prev, term) => assoc(prev, term, term), this.state.initial) }
          placeholder='- Select criteria -'
          value={this.state.term}
          onChange={ this.onTermChange }
        />
        {this.operator}
        {this.value}
      </div>
    );
  }
}