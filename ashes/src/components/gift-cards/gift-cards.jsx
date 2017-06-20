/* @flow */

import React, { Component } from 'react';

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { stateTitles } from 'paragons/gift-card';
import { getIdsByProps, bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// components
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import GiftCardRow from './gift-card-row';
import { ChangeStateModal, CancelModal } from 'components/bulk-actions/modal';
import { SelectableSearchList } from 'components/list-page';
import { Link } from 'components/link';

// actions
import { actions } from 'modules/gift-cards/list';
import { actions as bulkActions } from 'modules/gift-cards/bulk';
import { bulkExport } from 'modules/bulk-export/bulk-export';

type Props = {
  list: Object,
  actions: Object,
  bulkActions: {
    cancelGiftCards: (codes: Array<string>, reasonId: number) => void,
    changeGiftCardsState: (codes: Array<string>, state: string) => void,
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<string>, entity: string, identifier: string
    ) => void,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
};

const tableColumns = [
  {field: 'code', text: 'Gift Card Number', model: 'giftcard'},
  {field: 'originType', text: 'Type', model: 'giftCard'},
  {field: 'originalBalance', text: 'Original Balance', type: 'currency'},
  {field: 'currentBalance', text: 'Current Balance', type: 'currency'},
  {field: 'availableBalance', text: 'Available Balance', type: 'currency'},
  {field: 'state', text: 'State', type: 'state', model: 'giftCard'},
  {field: 'createdAt', text: 'Date/Time Issued', type: 'datetime'}
];

class GiftCards extends Component {
  props: Props;

  @autobind
  cancelGiftCards(allChecked: boolean, toggledIds: Array<string>) {
    const {cancelGiftCards} = this.props.bulkActions;

    return (
      <CancelModal
        count={toggledIds.length}
        onConfirm={(reasonId) => cancelGiftCards(toggledIds, reasonId)} />
    );
  }

  getChangeGiftCardsState(state: string) {
    const stateTitle = stateTitles[state];

    return (allChecked, toggledIds) => {
      const {changeGiftCardsState} = this.props.bulkActions;

      return (
        <ChangeStateModal
          count={toggledIds.length}
          stateTitle={stateTitle}
          onConfirm={() => changeGiftCardsState(toggledIds, state)} />
      );
    };
  }

  getChangeGiftCardsStateAction(state: string) {
    const stateTitle = stateTitles[state];

    return [
      `Change Gift Cards state to ${stateTitle}`,
      this.getChangeGiftCardsState(state),
      `successfully changed state to ${stateTitle}`,
      `could not change state to ${stateTitle}`,
    ];
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<string>) {
    const { list } = this.props;
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Gift Cards';
    const entity = 'giftCards';
    const results = list.currentSearch().results.rows;
    const ids = getIdsByProps('code', toggledIds, results);

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, ids);
  }

  get cancelGCAction(): Array<any> {
    return [
      'Cancel Gift Cards',
      this.cancelGiftCards,
      'successfully canceled',
      'could not be canceled',
    ];
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Gift Cards'),
      this.cancelGCAction,
      this.getChangeGiftCardsStateAction('active'),
      this.getChangeGiftCardsStateAction('onHold'),
    ];
  }

  renderDetail(messages: string, code: string) {
    return (
      <span key={code}>
        Gift card <Link to="giftcard" params={{giftCard: code}}>{code}</Link>
      </span>
    );
  }

  renderRow(row: Object, index: number, columns: Columns, params: Object) {
    const key = `gift-card-${row.code}`;

    return (
      <GiftCardRow
        key={key}
        giftCard={row}
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
          storePath="giftCards.bulk"
          module="giftCards"
          entity="gift card"
          renderDetail={this.renderDetail} />
        <BulkActions
          module="giftCards"
          entity="gift card"
          actions={this.bulkActions}>
          <SelectableSearchList
            exportEntity="giftCards"
            exportTitle="Gift Cards"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="giftCards.list"
            emptyMessage="No gift cards found."
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
    list: _.get(state.giftCards, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(GiftCards);
