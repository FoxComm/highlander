//libs
import _ from 'lodash';
import React, { PropTypes, Component } from 'react';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind, debounce } from 'core-decorators';
import moment from 'moment';
import classNames from 'classnames';

//data
import criterions from '../../../paragons/customer-groups/criterions';
import operators from '../../../paragons/customer-groups/operators';
import queryAdapter from '../../../modules/customer-groups/query-adapter';
import { actions as groupActions } from '../../../modules/customer-groups/dynamic/group';
import { actions as listActions } from '../../../modules/customer-groups/dynamic/list';

//helpers
import { transitionTo } from 'browserHistory';
import { prefix } from '../../../lib/text-utils';

//components
import { PrimaryButton } from '../../common/buttons';
import ContentBox from '../../content-box/content-box';
import { PanelList, PanelListItem } from '../../panel/panel-list';
import Currency from '../../common/currency';
import Criterion from './criterion-view';
import PrependIconInput from '../../icon-input/prepend-icon-input';
import { SelectableSearchList, makeTotalCounter } from '../../list-page';
import MultiSelectRow from '../../table/multi-select-row';


const prefixed = prefix('fc-customer-group-dynamic');

const mapStateToProps = state => ({list: state.customerGroups.dynamic.list});
const mapDispatchToProps = dispatch => ({
  groupActions: bindActionCreators(groupActions, dispatch),
  listActions: bindActionCreators(listActions, dispatch),
});

const tableColumns = [
  {field: 'name', text: 'Name'},
  {field: 'email', text: 'Email'},
  {field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime'}
];

const TotalCounter = makeTotalCounter(state => state.customerGroups.dynamic.list, listActions);

@connect(mapStateToProps, mapDispatchToProps)
export default class DynamicGroup extends Component {

  static propTypes = {
    list: PropTypes.object,
    group: PropTypes.shape({
      id: PropTypes.number,
      name: PropTypes.string,
      mainCondition: PropTypes.oneOf([
        operators.and,
        operators.or,
      ]),
      conditions: PropTypes.arrayOf(PropTypes.array),
      filterTerm: PropTypes.string,
    }),
    groupActions: PropTypes.shape({
      setFilterTerm: PropTypes.func.isRequired,
    }).isRequired,
    listActions: PropTypes.shape({
      fetch: PropTypes.func.isRequired,
    }).isRequired,
  };

  state = {
    criteriaOpen: true,
  };

  componentDidMount() {
    this.setGroupQuery(this.props.group);
  }

  componentWillReceiveProps({group}) {
    if (group !== this.props.group) {
      this.setGroupQuery(group);
    }
  }

  setGroupQuery({mainCondition, conditions}) {
    const {listActions} = this.props;

    listActions.resetSearch();

    listActions.setExtraFilters([
      queryAdapter(criterions, mainCondition, conditions).toRequest().filter,
    ]);

    listActions.fetch();
  }

  @autobind
  goToEdit() {
    transitionTo('edit-dynamic-customer-group', {groupId: this.props.group.id});
  }

  get header() {
    const {group} = this.props;

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
    const {mainCondition, conditions} = this.props.group;
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
    const {criteriaOpen} = this.state;
    const icon = criteriaOpen ? 'icon-chevron-up' : 'icon-chevron-down';

    return (
      <i className={icon} onClick={() => this.setState({criteriaOpen: !criteriaOpen})} />
    );
  }


  get stats() {
    const customersTotal = _.get(this.props.list, 'savedSearches.0.results.total', 0);
    const avgOrderValue = 83250;

    return (
      <PanelList className={prefixed('stats')}>
        <PanelListItem title="Total Orders">
          132
        </PanelListItem>
        <PanelListItem title="Total Sales">
          <Currency value={avgOrderValue*customersTotal} />
        </PanelListItem>
        <PanelListItem title="Avg. Order Size">
          2
        </PanelListItem>
        <PanelListItem title="Avg. Order Value">
          <Currency value={avgOrderValue} />
        </PanelListItem>
        <PanelListItem title="Return Rate">
          14%
        </PanelListItem>
      </PanelList>
    );
  }

  @autobind
  setFilterTerm({target}) {
    this.props.groupActions.setFilterTerm(target.value);
    this.updateSearch();
  }

  @debounce(200)
  updateSearch() {
    this.props.listActions.fetch();
  }

  get filter() {
    return (
      <PrependIconInput className={prefixed('filter')}>
        <input type="text"
               onChange={this.setFilterTerm}
               value={this.props.group.filterTerm} />
      </PrependIconInput>
    );
  }

  goToCustomer(id) {
    return () => {
      transitionTo('customer', {customerId: id});
    };
  }

  get renderRow() {
    return (row, index, columns, params) => (
      <MultiSelectRow
        cellKeyPrefix={index}
        columns={columns}
        onClick={this.goToCustomer(row.id)}
        row={row}
        setCellContents={(customer, field) => _.get(customer, field)}
        params={params} />
    );
  }

  get table() {
    const {list, listActions} = this.props;

    return (
      <SelectableSearchList
        emptyMessage="No customers found."
        list={list}
        renderRow={this.renderRow}
        tableColumns={tableColumns}
        searchActions={listActions}
        searchOptions={{singleSearch: true}} />
    );
  }

  render() {
    return (
      <div className={classNames(prefixed(), 'fc-list-page')}>
        <div className={classNames(prefixed('details'), 'fc-grid')}>
          <article className="fc-col-md-1-1">
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
