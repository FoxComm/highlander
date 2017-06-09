/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { stateTitles } from '../../paragons/content-type';
import { getIdsByProps, bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import ContentTypeRow from './content-type-row';
import { ChangeStateModal, CancelModal } from 'components/bulk-actions/modal';
import { SelectableSearchList } from 'components/list-page';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/content-types/list';
import { actions as bulkActions } from 'modules/content-types/bulk';
import { bulkExport } from 'modules/bulk-export/bulk-export';

type Props = {
  list: Object,
  actions: Object,
  bulkActions: {
    cancelContentTypes: (codes: Array<string>, reasonId: number) => void,
    changeContentTypesState: (codes: Array<string>, state: string) => void,
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<string>, entity: string, identifier: string
    ) => void,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
};

const tableColumns = [
  {field: 'code', text: 'Content Type Number', model: 'contenttype'},
  {field: 'originType', text: 'Type', model: 'contentType'},
  {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
  {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
  {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
  {field: 'state', text: 'State', type: 'state', model: 'contentType'},
  {field: 'createdAt', text: 'Date/Time Issued', type: 'date'}
];

class ContentTypes extends Component {
  props: Props;

  @autobind
  cancelContentTypes(allChecked: boolean, toggledIds: Array<string>) {
    const {cancelContentTypes} = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => cancelContentTypes(toggledIds, reasonId)} />
    );
  }

  getChangeContentTypesState(state: string) {
    const stateTitle = stateTitles[state];

    return (allChecked, toggledIds) => {
      const {changeContentTypesState} = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeContentTypesState(toggledIds, state)} />
      );
    };
  }

  getChangeContentTypesStateAction(state: string) {
    const stateTitle = stateTitles[state];

    return [
      `Change Content Types state to ${stateTitle}`,
      this.getChangeContentTypesState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<string>) {
    const { list } = this.props;
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Content Types';
    const entity = 'contentTypes';
    const results = list.currentSearch().results.rows;
    const ids = getIdsByProps('code', toggledIds, results);

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, ids);
  }

  get cancelGCAction(): Array<any> {
    return [
      'Cancel Content Types',
      this.cancelContentTypes,
      'successfully canceled',
      'could not be canceled',
    ];
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Content Types'),
      this.cancelGCAction,
      this.getChangeContentTypesStateAction('active'),
      this.getChangeContentTypesStateAction('onHold'),
    ];
  }

  renderDetail(messages: string, code: string) {
    return (
      <span key={code}>
        Gift card <Link to="contenttype" params={{contentType: code}}>{code}</Link>
      </span>
    );
  }

  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `content-type-${row.code}`;

    return (
      <ContentTypeRow
        key={key}
        contentType={row}
        columns={columns}
        params={params}
      />
    );
  }

  render() {
    const {list, actions} = this.props;

    return (
      <div>
        <BulkMessages
          storePath="contentTypes.bulk"
          module="contentTypes"
          entity="content type"
          renderDetail={this.renderDetail} />
        <BulkActions
          module="contentTypes"
          entity="content type"
          actions={this.bulkActions}>
          <SelectableSearchList
            exportEntity="contentTypes"
            exportTitle="Content Types"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="contentTypes.list"
            emptyMessage="No content types found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={actions}
            predicate={({code}) => code} />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.contentTypes, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(ContentTypes);
