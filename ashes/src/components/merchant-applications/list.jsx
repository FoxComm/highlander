/* @flow */

// libs
import React, { Component, Element } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';

// components
import { ListPageContainer, makeTotalCounter } from 'components/list-page';
import MerchantApplicationRow from './row';
import MultiSelectTable from 'components/table/multi-select-table';

// actions
import * as applicationActions from 'modules/merchant-applications/list';

// types
import type { MerchantApplication } from 'paragons/merchant-applications';

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
    }
  }

  render(): Element {
    console.log('Applications:');
    console.log(this.props.list.applications);

    return (
      <ListPageContainer title="Merchant Applications">
        <MultiSelectTable
          columns={tableColumns}
          data={{ rows: this.props.list.applications }}
          renderRow={this.renderRow} />
      </ListPageContainer>
    );
  }
}

export default connect(mapStateToProps, applicationActions)(MerchantApplicationsList);