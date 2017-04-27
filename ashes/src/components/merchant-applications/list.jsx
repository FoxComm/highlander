/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { ListPageContainer, makeTotalCounter } from 'components/list-page';
import MerchantApplicationRow from './row';
import MultiSelectTable from 'components/table/multi-select-table';
import SubNav from './sub-nav';

// actions
import * as applicationActions from 'modules/merchant-applications/list';

// styles
import styles from './list.css';

// types
import type { MerchantApplication } from 'paragons/merchant-application';

type Props = {
  list: {
    applications: Array<MerchantApplication>,
  },
  isFetching: boolean,
  fetchError: ?Object,
  fetchApplications: Function,
};

const mapStateToProps = (state) => {
  return {
    list: state.applications.list,
    isFetching: _.get(state.asyncActions, 'getApplications.inProgress', null),
    fetchError: _.get(state.asyncActions, 'getApplications.err', null),
  };
};

const tableColumns = [
  { field: 'id', text: 'ID' },
  { field: 'business_name', text: 'Business Name' },
  { field: 'name', text: 'Name' },
  { field: 'email_address', text: 'Email' },
  { field: 'reference_number', text: 'Reference Number' },
  { field: 'state', text: 'State' },
];

class MerchantApplicationsList extends Component {
  props: Props;

  componentDidMount() {
    this.props.fetchApplications();
  }

  get renderRow() {
    return (row, index, columns, params) => {
      const key = `application-${row.id}`;

      return (
        <MerchantApplicationRow
          key={key}
          application={row}
          columns={columns}
          params={params} />
      );
    };
  }

  render() {
    const { list, isFetching } = this.props;
    const count = list.applications.length;

    const props = isFetching || count == 0
      ? { title: 'Merchant Applications' }
      : { title: 'Merchant Applications', subtitle: count };

    return (
      <ListPageContainer {...props}>
        <SubNav />
        <div styleName="body">
          <div styleName="content">
            <MultiSelectTable
              columns={tableColumns}
              data={{ rows: list.applications }}
              isLoading={isFetching}
              emptyMessage="No applications found."
              renderRow={this.renderRow} />
          </div>
        </div>
      </ListPageContainer>
    );
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationsList);
