//libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind, debounce } from 'core-decorators';
import moment from 'moment';
import classNames from 'classnames';

//data
import criterions from 'paragons/customer-groups/criterions';
import operators from 'paragons/customer-groups/operators';
import requestAdapter from 'modules/customer-groups/request-adapter';
import * as groupActions from 'modules/customer-groups/group';
import { actions as customersListActions } from 'modules/customer-groups/customers-list';

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

const StatsValue = ({ value, currency }) => {
  if (!_.isNumber(value)) {
    return <span>—</span>;
  }

  return currency ? <Currency value={value} /> : <span>{value}</span>;
};

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroup extends Component {

  static propTypes = {
    customersList: PropTypes.object,
    statsLoading: PropTypes.bool,
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array),
      stats: PropTypes.shape({
        ordersCount: PropTypes.number,
        totalSales: PropTypes.number,
        averageOrderSize: PropTypes.number,
        averageOrderSum: PropTypes.number,
      }),
    }),
    groupActions: PropTypes.shape({
      fetchGroupStats: PropTypes.func.isRequired,
    }).isRequired,
    customersListActions: PropTypes.shape({
      fetch: PropTypes.func.isRequired,
    }).isRequired,
  };

  state = {
    criteriaOpen: true,
  };

  componentDidMount() {
    this.refreshGroupData(this.props.group);
  }

  componentWillReceiveProps({ group }) {
    if (group.id !== this.props.group.id) {
      this.refreshGroupData(group);
    }
  }

  refreshGroupData({ mainCondition, conditions }) {
    const { customersListActions, groupActions } = this.props;

    customersListActions.resetSearch();

    customersListActions.setExtraFilters([
      requestAdapter(criterions, mainCondition, conditions).toRequest().query,
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
          <PrimaryButton onClick={this.goToEdit}>Edit Group</PrimaryButton>
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
  renderCriterion([field, operator, value], index) {
    return (
      <Criterion key={index}
                 field={field}
                 operator={operator}
                 value={value} />
    );
  }

  get criteria() {
    const { mainCondition, conditions } = this.props.group;
    const main = mainCondition === operators.and ? 'all' : 'any';

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
        {conditions.map(this.renderCriterion)}
      </ContentBox>
    );
  }

  get criteriaToggle() {
    const { criteriaOpen } = this.state;
    const icon = criteriaOpen ? 'icon-chevron-up' : 'icon-chevron-down';

    return (
      <i className={icon} onClick={() => this.setState({criteriaOpen: !criteriaOpen})} />
    );
  }


  get stats() {
    const { statsLoading, group: { stats } } = this.props;

    return (
      <PanelList className={classNames(prefixed('stats'), { _loading: statsLoading })}>
        <PanelListItem title="Total Orders">
          <StatsValue value={stats.ordersCount} />
        </PanelListItem>
        <PanelListItem title="Total Sales">
          <StatsValue value={stats.totalSales} currency />
        </PanelListItem>
        <PanelListItem title="Avg. Order Size">
          <StatsValue value={stats.averageOrderSize} />
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

  get renderRow() {
    return (row, index, columns, params) => (
      <MultiSelectRow
        key={index}
        columns={columns}
        linkTo="customer"
        linkParams={{customerId: row.id}}
        row={row}
        setCellContents={(customer, field) => _.get(customer, field)}
        params={params} />
    );
  }

  get table() {
    const { customersList, customersListActions } = this.props;

    return (
      <SelectableSearchList
        entity="customerGroups.details.customers"
        emptyMessage="No customers found."
        list={customersList}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchActions={customersListActions}
        searchOptions={{singleSearch: true}} />
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
