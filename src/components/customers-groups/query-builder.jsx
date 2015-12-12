import React, { PropTypes } from 'react';
import _ from 'lodash';
// components
import { AddButton } from '../common/buttons';
import Criterion from './criterion';
import Currency from '../common/currency';
// stuff
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { assoc, dissoc, get } from 'sprout-data';

export default class QueryBuilder extends React.Component {

  static propTypes = {
    termOptions: PropTypes.object.isRequired, // {value -> title} for terms
    criterions: PropTypes.object.isRequired,
    initBuilder: PropTypes.func.isRequired,
    addCriterion: PropTypes.func.isRequired
  };

  componentDidMount() {
    this.props.initBuilder();
  }

  get criterions() {
    const selectedValues = _.pluck(_.values(this.props.criterions), 'selectedTerm');
    const availableValues = _.difference(_.keys(this.props.termOptions), selectedValues);
    const items = availableValues.reduce((r, term) => assoc(r, term, this.props.termOptions[term]), {});

    function buildCriterion(key) {
      const selectedVal = get(this.props.criterions, [key, 'selectedTerm']);
      let curItems = items;
      if (selectedVal) {
        curItems = assoc(curItems, selectedVal, this.props.termOptions[selectedVal]);
      }

      return <Criterion key={key} id={key} terms={curItems} selectedTerm={selectedVal}/>;
    }

    return (
      <div className='fc-grid fc-group-builder-criterions'>
        {_.keys(this.props.criterions).map(buildCriterion.bind(this))}
      </div>
    );
  };

  @autobind
  addCriterion(e) {
    e.preventDefault();
    this.props.addCriterion();
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
