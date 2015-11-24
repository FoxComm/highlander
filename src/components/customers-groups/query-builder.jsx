import React, { PropTypes } from 'react';
import _ from 'lodash';
// components
import SectionTitle from '../section-title/section-title';
import FormField from '../forms/formfield.jsx';
import Form from '../forms/form.jsx';
import Dropdown from '../dropdown/dropdown';
import DropdownItem from '../dropdown/dropdownItem';
import { AddButton } from '../common/buttons';
import { Link } from '../link';
import Criterion from './criterion';
// stuff
import { transitionTo } from '../../route-helpers';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { assoc, dissoc } from 'sprout-data';

const append = function(collection, item) {
  return assoc(collection, collection.length, item);
}

export default class QueryBuilder extends React.Component {

  constructor(props, ...args) {
    super(props, ...args);
    this.terms = _.pluck(props.criterions, 'term');

    this.state = {
      counter: 2,
      current: {1: null}
    };
  }

  static propTypes = {
    criterions: PropTypes.array.isRequired
  };

  termChanged(key, _prev, current) {
    this.setState({
      current: assoc(this.state.current, key, current)
    });
  }

  removeCriteria(key) {
    this.setState({current: dissoc(this.state.current, key)});
  }

  @autobind
  buildCriterion(key) {
    return (
      <div className='fc-group-builder-criterion-container' key={key}>
        <Criterion
          terms={_.difference(this.terms, _.values(this.state.current))}
          initialTerm={ this.state.current[key] }
          onTermChange={this.termChanged.bind(this, key)}
          criterions={this.props.criterions}
        />
        <i onClick={this.removeCriteria.bind(this, key)} className='fc-group-builder-remove-crit icon-close'/>
      </div>
    );
  }

  get criterions() {
    return (
      <div className='fc-group-builder-criterions'>
        {_.keys(this.state.current).map(this.buildCriterion)}
      </div>
    );
  };

  @autobind
  addCriterion(e) {
    e.preventDefault();
    if (_.contains(_.values(this.state.current), null)) {
      return;
    }
    if (this.terms.length == _.size(this.state.current)) return;

    let term = null;
    if (this.terms.length == _.size(this.state.current) + 1) {
      term = _.first(_.difference(this.terms, _.values(this.state.current)));
    }

    const key = this.state.counter;
    this.setState({
      counter: key + 1,
      current: assoc(this.state.current, key, term)
    });
  }

  render () {
    return (
      <div className='fc-group-builder'>
        {this.criterions}
        <div className='fc-group-builder-add-criterion' onClick={this.addCriterion}>
          <AddButton/><span>Add criteria</span>
        </div>
      </div>
    );
  }
}
