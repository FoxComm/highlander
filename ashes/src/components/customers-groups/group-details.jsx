/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { connect } from 'react-redux';
import { bindActionCreators } from 'redux';
import { autobind, debounce } from 'core-decorators';
import moment from 'moment';
import classNames from 'classnames';
import operators from 'paragons/customer-groups/operators';
import { transitionTo } from 'browserHistory';
import { prefix } from 'lib/text-utils';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import {
  fetchGroupStats,
  GROUP_TYPE_MANUAL,
  GROUP_TYPE_DYNAMIC,
  addCustomersToGroup
} from 'modules/customer-groups/details/group';
import { actions as customersListActions } from 'modules/customer-groups/details/customers-list';
import { suggestCustomers } from 'modules/customers/suggest';
import { actions as bulkActions } from 'modules/customer-groups/details/bulk';
import { bulkExport } from 'modules/bulk-export/bulk-export';

// components
import { Link } from 'components/link';
import { DeleteModal } from 'components/bulk-actions/modal';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { SelectableSearchList, makeTotalCounter } from 'components/list-page';
import { PrimaryButton, Button } from 'components/core/button';
import MultiSelectRow from 'components/table/multi-select-row';
import ContentBox from 'components/core/content-box';
import Criterion from './editor/criterion-view';
import CustomerGroupStats from './stats';
import SearchCustomersModal from './customers/search-modal';

type State = {
  criteriaOpen: boolean,
  addCustomersModalShown: boolean,
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
  bulkActions: {
    deleteCustomersFromGroup: (groupId: number, customersIds: Array<number>) => Promise<*>,
    exportByIds: (ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string)
      => void,
  },
  bulkExportAction: (fields: Array<string>, entity: string, identifier: string, description: string) => Promise<*>,
  suggested: Array<TUser>,
  suggestState: AsyncState,
  suggestCustomers: (token: string) => Promise<*>,
  addCustomersToGroup: (groupId: number, ids: Array<number>) => Promise<*>,
};

const prefixed = prefix('fc-customer-group');

const tableColumns = [
  { field: 'name', text: 'Name' },
  { field: 'email', text: 'deleteCustomersFromGroupEmail' },
  { field: 'joinedAt', text: 'Date/Time Joined', type: 'datetime' }
];

const TotalCounter = makeTotalCounter(state => state.customerGroups.details.customers, customersListActions);

class GroupDetails extends Component {

  props: Props;

  static defaultProps = {
    customersList: [],
  };

  state: State = {
    criteriaOpen: true,
    addCustomersModalShown: false,
  };

  componentDidMount() {
    this.refreshGroupData();
  }

  componentWillReceiveProps({ group }: Props) {
    if (group.id !== this.props.group.id) {
      this.refreshGroupData();
    }
  }

  @autobind
  refreshGroupData() {
    const { customersListActions, groupActions, group } = this.props;

    customersListActions.resetSearch();

    customersListActions.setExtraFilters([group.elasticRequest]);

    customersListActions.fetch();

    groupActions.fetchGroupStats();
  }

  @autobind
  goToEdit() {
    transitionTo('edit-customer-group', { groupId: this.props.group.id });
  }

  @autobind
  showAddCustomersModal() {
    this.setState({ addCustomersModalShown: true });
  }

  @autobind
  onAddCustomersCancel() {
    this.setState({ addCustomersModalShown: false });
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Customers';
    const entity = 'customers';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  @autobind
  handleDeleteCustomers(allChecked: boolean, customersIds: Array<number> = []) {
    const { deleteCustomersFromGroup } = this.props.bulkActions;

    return (
      <DeleteModal
        count={customersIds.length}
        stateTitle={'Delete'}
        onConfirm={() => deleteCustomersFromGroup(this.props.group.id, customersIds)}
      />
    );
  }

  get deleteAction() {
    return [
      'Delete From Group',
      this.handleDeleteCustomers,
      'successfully deleted from group',
      'could not be deleted from group'
    ];
  }

  get bulkActions() {
    if (this.props.group.groupType != GROUP_TYPE_MANUAL) return [];

    return [
      bulkExportBulkAction(this.bulkExport, 'Customers'),
      this.deleteAction,
    ];
  }

  @autobind
  handleCustomersSave(ids: Array<number>) {
    const { group, addCustomersToGroup } = this.props;

    this.setState({ addCustomersModalShown: false }, () => {
      addCustomersToGroup(group.id, ids).then(this.refreshGroupData);
    });
  }

  get addCustomersModal() {
    if (this.props.group.groupType != GROUP_TYPE_MANUAL) {
      return null;
    }

    return (
      <SearchCustomersModal
        isVisible={this.state.addCustomersModalShown}
        onCancel={this.onAddCustomersCancel}
        handleSave={this.handleCustomersSave}
        suggestCustomers={this.props.suggestCustomers}
        suggested={this.props.suggested}
        suggestState={this.props.suggestState}
      />
    );
  }

  get criteria() {
    const { mainCondition, conditions, groupType } = this.props.group;

    if (groupType != GROUP_TYPE_DYNAMIC) return null;

    const main = mainCondition === operators.and ? 'all' : 'any';
    const conditionBlock = _.map(conditions, this.renderCriterion);

    return (
      <ContentBox title="Criteria"
                  className={prefixed('criteria')}
                  bodyClassName={classNames({ '_closed': !this.state.criteriaOpen })}
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

  get criteriaToggle() {
    const { criteriaOpen } = this.state;
    const icon = criteriaOpen ? 'icon-chevron-up' : 'icon-chevron-down';

    return (
      <i className={icon} onClick={() => this.setState({ criteriaOpen: !criteriaOpen })} />
    );
  }

  @debounce(200)
  updateSearch() {
    this.props.customersListActions.fetch();
  }

  @autobind
  renderCriterion([field, operator, value]: Array<Object>, index?: number) {
    return (
      <Criterion
        key={index}
        field={field}
        operator={operator}
        value={value}
      />
    );
  }

  get renderRow(): Function {
    return (row, index, columns, params) => (
      <MultiSelectRow
        key={index}
        columns={columns}
        linkTo="customer"
        linkParams={{ customerId: row.id }}
        row={row}
        setCellContents={(customer, field) => _.get(customer, field)}
        params={params}
      />
    );
  }

  renderBulkDetails(customerName, customerId) {
    return (
      <span key={customerId}>
        Customer <Link to="customer-details" params={{ customerId }}>{customerName}</Link>
      </span>
    );
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
          {group.groupType == 'manual' && <Button onClick={this.showAddCustomersModal}>Add Customers</Button>}
          {group.groupType != 'template' && <PrimaryButton onClick={this.goToEdit}>Edit Group</PrimaryButton>}
        </div>
        <div className={prefixed('about')}>
          <div>
            <span className={prefixed('about__key')}>Type:&nbsp;</span>
            <span className={prefixed('about__value')}>{_.capitalize(group.groupType)}</span>
          </div>
          <div>
            <span className={prefixed('about__key')}>Created:&nbsp;</span>
            <span className={prefixed('about__value')}>{moment(group.createdAt).format('DD/MM/YYYY HH:mm')}</span>
          </div>
        </div>
      </header>
    );
  }

  get table() {
    const { customersList, customersListActions } = this.props;

    return (
      <div>
        <BulkMessages
          storePath="customerGroups.details.bulk"
          module="customerGroups.details"
          entity="customer"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="customerGroups.details"
          entity="customer"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="customers"
            exportTitle="Customers"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="customerGroups.details.customers"
            emptyMessage="No customers found."
            list={customersList}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={customersListActions}
            searchOptions={{ singleSearch: true }}
          />
        </BulkActions>
      </div>
    );
  }

  render() {
    const { group, statsLoading } = this.props;

    return (
      <div className={classNames(prefixed(), 'fc-list-page')}>
        <div className={classNames(prefixed('details'))}>
          <article>
            {this.header}
            {this.criteria}
            <CustomerGroupStats stats={group.stats} isLoading={statsLoading} />
          </article>
        </div>
        {this.table}
        {this.addCustomersModal}
      </div>
    );
  }
}

const mapState = (state) => {
  return {
    customersList: _.get(state.customerGroups, 'details.customers', {}),
    statsLoading: _.get(state.asyncActions, 'fetchStatsCustomerGroup.inProgress', false),
    suggested: _.get(state.customers, 'suggest.customers', []),
    suggestState: _.get(state.asyncActions, 'suggestCustomers', {}),
  };
};

const mapDispatch = (dispatch, props) => {
  const customerEntries = _.get(props, 'customerGroups.details.customers', []);
  const customers = _.map(customerEntries, customer => customer.id);

  return {
    groupActions: bindActionCreators({ fetchGroupStats }, dispatch),
    customersListActions: bindActionCreators(customersListActions, dispatch),
    ...(bindActionCreators({
      suggestCustomers: suggestCustomers(customers),
      addCustomersToGroup,
    }, dispatch)),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapState, mapDispatch)(GroupDetails);
