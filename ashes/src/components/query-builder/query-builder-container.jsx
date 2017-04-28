//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';

//data
import operators from 'paragons/query-builder/operators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Dropdown } from 'components/dropdown';
import QueryBuilder from './query-builder';

const SELECT_CRITERIA = [
  [operators.and, 'all'],
  [operators.or, 'any']
];

const prefixed = prefix('fc-query-builder-edit');

class QueryBuilderContainer extends React.Component {

  constructor(props) {
    super(props);
  }

  get mainCondition() {

    return (
      <div className={prefixed('match-div')}>
        <span className={prefixed('match-span')}>Items match</span>
        <span className={prefixed('match-dropdown')}>
          <Dropdown
            name="matchCriteria"
            value={this.props.mainCondition}
            onChange={value => this.props.setMainCondition(value)}
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
          criterions={this.props.criterions}
          getCriterion={this.props.getCriterion}
          getOperators={this.props.getOperators}
          getWidget={this.props.getWidget}
          mainCondition={this.props.mainCondition}
          conditions={this.props.conditions}
          setConditions={this.props.setConditions}
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

export default QueryBuilderContainer;
