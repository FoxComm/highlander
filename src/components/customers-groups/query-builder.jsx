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
    const props = this.props;

    const selectedValues = _.pluck(_.values(props.criterions), 'term');
    const availableValues = _.difference(_.keys(props.termOptions), selectedValues);
    const items = availableValues.map(term => [term, props.termOptions[term]]);

    function buildCriterion(key) {
      const selectedVal = get(props.criterions, [key, 'term']);
      let curItems = items;
      if (selectedVal) {
        curItems = [...items, [selectedVal, props.termOptions[selectedVal]]];
      }

      return <Criterion key={key} id={key} terms={curItems} term={selectedVal}/>;
    }

    return (
      <div className='fc-grid fc-group-builder-criterions'>
        {_.keys(this.props.criterions).map(buildCriterion)}
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
