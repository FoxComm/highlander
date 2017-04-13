//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';
//data
import operators from 'paragons/promotions-query-builder/operators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Dropdown } from 'components/dropdown';
import QueryBuilder from './query-builder';

const SELECT_CRITERIA = [
  [operators.and, 'all'],
  [operators.or, 'any']
];

const prefixed = prefix('fc-customer-group-edit');

class ElasticQueryGenerator extends React.Component {

  constructor(props) {
    super(props);
    this.state = {
      mainCondition: _.get(SELECT_CRITERIA, '0.0'),
      conditions: [],
    }
  }

  @autobind
  setMainCondition(value) {
    this.setState({
      mainCondition: value,
    })
  }

  @autobind
  setConditions(value) {
    this.setState({
      conditions: value,
    })
  }

  get mainCondition() {

    return (
      <div className={prefixed('match-div')}>
        <span className={prefixed('match-span')}>Items match</span>
        <span className={prefixed('match-dropdown')}>
          <Dropdown
            name="matchCriteria"
            value={this.state.mainCondition}
            onChange={value => this.setMainCondition(value)}
            items={SELECT_CRITERIA}
          />
        </span>
        <span className={prefixed('form-name')}>of the following criteria:</span>
      </div>
    );
  }

  get queryBuilder() {
    const { group, actions } = this.props;

    return (
      <div>
        {this.mainCondition}
        <QueryBuilder
          mainCondition={this.state.mainCondition}
          conditions={this.state.conditions}
          setConditions={this.setConditions}
        />
      </div>
    );
  }

  render() {
    return (
      <div>
        {this.queryBuilder}
      </div>
    );
  }
}

export default ElasticQueryGenerator;
