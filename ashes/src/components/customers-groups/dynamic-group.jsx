/* @flow weak */

//libs
import _ from 'lodash';
import React, { PropTypes, Component, Element } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind, debounce } from 'core-decorators';
import moment from 'moment';
import classNames from 'classnames';

//data
import criterions from 'paragons/customer-groups/criterions';
import operators from 'paragons/customer-groups/operators';
import requestAdapter from 'modules/customer-groups/utils/request-adapter';
import * as groupActions from 'modules/customer-groups/details/group';
import { actions as customersListActions } from 'modules/customer-groups/details/customers-list';

//helpers
import { transitionTo } from 'browserHistory';
import { prefix } from 'lib/text-utils';

//components
import { SelectableSearchList, makeTotalCounter } from 'components/list-page';
import { PanelList, PanelListItem } from 'components/panel/panel-list';
import { PrimaryButton } from 'components/common/buttons';
import MultiSelectRow from 'components/table/multi-select-row';
import ContentBox from 'components/content-box/content-box';
import Currency from 'components/common/currency';
import Criterion from './editor/criterion-view';

type State = {
  criteriaOpen: boolean,
};

type Props = {
  customersList: Object,
  statsLoading: boolean,
  group: TCustomerGroup,
  groupActions: {
    fetchGroupStats: Function,
  },
  customersListActions: {
    resetSearch: Function,
    setExtraFilters: Function,
    fetch: Function,
  },
};

const prefixed = prefix('fc-customer-group');

const mapStateToProps = state => ({
  customersList: _.get(state, 'customerGroups.details.customers'),
  statsLoading: _.get(state, 'asyncActions.fetchStatsCustomerGroup.inProgress', false),
});

const mapDispatchToProps = dispatch => ({
  groupActions: bindActionCreators(groupActions, dispatch),
  customersListActions: bindActionCreators(customersListActions, dispatch),
});

const tableColumns = [
  { field: 'name', text: 'Name' },
  { field: 'email', text: 'Email' },
  { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
];

const TotalCounter = makeTotalCounter(state => state.customerGroups.details.customers, customersListActions);

const StatsValue = ({ value, currency, preprocess = _.identity }) => {
  if (!_.isNumber(value)) {
    return <span>—</span>;
  }

  const v = preprocess(value);

  return currency ? <Currency value={v} /> : <span>{v}</span>;
};

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroup extends Component {

  props: Props;

  static defaultProps = {
    customersList: [],
  };

  state: State = {
    criteriaOpen: true,
  };

  componentDidMount() {
    this.refreshGroupData();
  }

  componentWillReceiveProps({ group }: Props) {
    if (group.id !== this.props.group.id) {
      this.refreshGroupData();
    }
  }

  refreshGroupData() {
    const { customersListActions, groupActions, group } = this.props;

    customersListActions.resetSearch();

    customersListActions.setExtraFilters([
      requestAdapter(group.id, criterions, group.mainCondition, group.conditions).toRequest().query,
    ]);

    customersListActions.fetch();

    groupActions.fetchGroupStats();
  }

  @autobind
  goToEdit() {
    transitionTo('edit-customer-group', { groupId: this.props.group.id });
  }

  get header() {
    const { group } = this.props;

    return (
      <header className={prefixed('header')}>
        <div className={prefixed('title')}>
          <h1 className="fc-title">
            {group.name}&nbsp;
            <span className={prefixed('count')}>
              <TotalCounter />
            </span>
          </h1>
          {group.conditions && <PrimaryButton onClick={this.goToEdit}>Edit Group</PrimaryButton>}
        </div>
        <div className={prefixed('about')}>
          <div>
            <span className={prefixed('about__key')}>Type:&nbsp;</span>
            <span className={prefixed('about__value')}>{_.capitalize(group.type)}</span>
          </div>
          <div>
            <span className={prefixed('about__key')}>Created:&nbsp;</span>
            <span className={prefixed('about__value')}>{moment(group.createdAt).format('DD/MM/YYYY HH:mm')}</span>
          </div>
        </div>
      </header>
    );
  }

  @autobind
  renderCriterion([field, operator, value]: Array<Object>, index?: number): Element {
    return (
      <Criterion
        key={index}
        field={field}
        operator={operator}
        value={value}
      />
    );
  }

  get criteria(): ?Element {
    const { mainCondition, conditions, type } = this.props.group;
    const main = mainCondition === operators.and ? 'all' : 'any';

    const conditionBlock = _.map(conditions, c => this.renderCriterion(c));

    if (type != 'manual' && conditions) return null;

    return (
      <ContentBox title="Criteria"
                  className={prefixed('criteria')}
                  bodyClassName={classNames({'_open': this.state.criteriaOpen})}
                  actionBlock={this.criteriaToggle}>
        <span className={prefixed('main')}>
          Customers match
          &nbsp;<span className={prefixed('inline-label')}>{main}</span>&nbsp;
          of the following criteria:
        </span>
        {conditionBlock}
      </ContentBox>
    );
  }

  get criteriaToggle(): Element {
    const { criteriaOpen } = this.state;
    const icon = criteriaOpen ? 'icon-chevron-up' : 'icon-chevron-down';

    return (
      <i className={icon} onClick={() => this.setState({criteriaOpen: !criteriaOpen})} />
    );
  }


  get stats() {
    // $FlowFixMe
    const { statsLoading, group: { stats } } = this.props;

    if (stats == null) return null;

    return (
      <PanelList className={classNames(prefixed('stats'), { _loading: statsLoading })}>
        <PanelListItem title="Total Orders">
          <StatsValue value={stats.ordersCount} />
        </PanelListItem>
        <PanelListItem title="Total Sales">
          <StatsValue value={stats.totalSales} currency />
        </PanelListItem>
        <PanelListItem title="Avg. Order Size">
          <StatsValue value={stats.averageOrderSize} preprocess={Math.round} />
        </PanelListItem>
        <PanelListItem title="Avg. Order Value">
          <StatsValue value={stats.averageOrderSum} currency />
        </PanelListItem>
      </PanelList>
    );
  }

  @debounce(200)
  updateSearch() {
    this.props.customersListActions.fetch();
  }

  get renderRow(): Function {
    return (row, index, columns, params) => (
      <MultiSelectRow
        key={index}
        columns={columns}
        linkTo="customer"
        linkParams={{customerId: row.id}}
        row={row}
        setCellContents={(customer, field) => _.get(customer, field)}
        params={params}
      />
    );
  }

  get table(): Element {
    const { customersList, customersListActions } = this.props;

    return (
      <SelectableSearchList
        entity="customerGroups.details.customers"
        emptyMessage="No customers found."
        list={customersList}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchActions={customersListActions}
        searchOptions={{singleSearch: true}}
      />
    );
  }

  render() {
    return (
      <div className={classNames(prefixed(), 'fc-list-page')}>
        <div className={classNames(prefixed('details'))}>
          <article>
            {this.header}
            {this.criteria}
            {this.stats}
          </article>
        </div>
        {this.table}
      </div>
    );
  }
}
