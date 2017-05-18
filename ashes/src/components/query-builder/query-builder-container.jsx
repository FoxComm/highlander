//libs
import React, { PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { Request, query } from 'elastic/request';

//data
import operators from 'paragons/query-builder/operators';

//helpers
import { prefix } from 'lib/text-utils';

//components
import { Dropdown } from 'components/dropdown';
import QueryBuilder from './query-builder';


type Props = {
  omitMainCondition?: boolean,
  itemName: string,
  criterions: Array<Object>,
  getCriterion: Function,
  getOperators: Function,
  getWidget: Function,
  mainCondition: string,
  conditions: Array<Array>,
  setMainCondition: Function,
  setElasticQuery: Function,
  setConditions: Function,
};

const requestAdapter = (criterions, mainCondition, conditions) => {
  const request = new Request(criterions);
  request.query = mainCondition === operators.and ? new query.ConditionAnd() : new query.ConditionOr();

  _.each(conditions, ([name, operator, value]) => {
    if (value != null) {
      const field = (new query.Field(name)).add(operator, value);
      request.query.add(field);
    }
  });

  return request;
};

const SELECT_CRITERIA = [
  [operators.and, 'all'],
  [operators.or, 'any']
];

const prefixed = prefix('fc-query-builder-edit');

class QueryBuilderContainer extends React.Component {
  props: Props;

  constructor(props) {
    super(props);
  }

  @autobind
  setConditions(newConditions) {
    const {criterions, mainCondition} = this.props;
    this.props.setConditions(newConditions);
    this.props.setElasticQuery(requestAdapter(criterions, mainCondition, newConditions).toRequest());
  }

  get mainCondition() {
    if (this.props.omitMainCondition) return (<hr className={prefixed('separator-hr')}/>);

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
          itemName={this.props.itemName}
          omitAddButton = {this.props.omitAddButton || false}
          criterions={this.props.criterions}
          getCriterion={this.props.getCriterion}
          getOperators={this.props.getOperators}
          getWidget={this.props.getWidget}
          mainCondition={this.props.mainCondition}
          conditions={this.props.conditions}
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

export default QueryBuilderContainer;
