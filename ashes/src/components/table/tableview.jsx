// libs
import React, { Component } from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import { renderExportModal } from 'modules/bulk-export/helpers';
import { autobind } from 'core-decorators';

// components
import Table from './table';
import DataTable from './data-table';
import ActionsDropdown from '../bulk-actions/actions-dropdown';
import TablePaginator from './paginator';
import TablePageSize from './pagesize';
import ColumnSelector from './column-selector';
import { Button } from 'components/core/button';

import s from './tableview.css';

class TableView extends Component {
  state = {
    showExportModal: false,
  };

  get stateFromProps() {
    const { setState } = this.props;
    if (!setState) return null;

    const { data } = this.props;

    return params => setState({ ...data, ...params });
  }

  @autobind
  getRow(position, items) {
    if (_.isEmpty(items)) return null;

    const filteredItems = _.flatMap(items, (item, index) => {
      return item ? React.cloneElement(item, { key: `${position}-${index}` }) : [];
    });

    return (
      <div className={`fc-table__${position}`}>
        {filteredItems}
      </div>
    );
  }

  @autobind
  handleExport() {
    this.setState({ showExportModal: true });
  }

  get flexSeparator() {
    return <div className="fc-table__flex-separator" />;
  }

  get bulkExportButton() {
    if (!this.props.bulkExport) return null;

    return <Button className={s.bulkExport} icon="export" onClick={this.handleExport} />;
  }

  get columnSelector() {
    if (_.isEmpty(this.props.selectableColumns)) return null;

    return (
      <ColumnSelector
        setColumns={this.props.setColumnSelected}
        columns={this.props.selectableColumns}
        identifier={this.props.tableIdentifier}
        toggleColumnsBtn
      />
    );
  }

  get actionsDropdown() {
    const { bulkActions } = this.props;
    if (_.isEmpty(this.props.bulkActions)) return null;

    const { toggledIds, allChecked, data: { total } } = this.props;

    //disabled if no data or nothing selected
    const totalSelected = allChecked ? total - toggledIds.length : toggledIds.length;
    const disabled = total === 0 || totalSelected === 0;

    return (
      <ActionsDropdown
        actions={bulkActions}
        disabled={disabled}
        allChecked={allChecked}
        toggledIds={toggledIds}
        total={total}
      />
    );
  }

  get topPagination() {
    const { paginator, setState } = this.props;
    if (!paginator || !setState) return null;

    const { data } = this.props;
    const { from, total, size } = data;
    return <TablePaginator total={total} from={from} size={size} setState={this.stateFromProps} />;
  }

  get bottomPagination() {
    const { paginator, setState } = this.props;
    if (!paginator || !setState) return null;

    const { size } = this.props.data;

    return <TablePageSize setState={this.stateFromProps} value={size} />;
  }

  get topItemsLeft() {
    const actionsDropdown = this.actionsDropdown;

    return _.compact([actionsDropdown]);
  }

  get topItemsRight() {
    const { headerControls = [] } = this.props;
    const bulkExport = this.bulkExportButton;
    const columnSelector = this.columnSelector;
    const pagination = this.topPagination;

    return _.compact([bulkExport, columnSelector, pagination, ...headerControls]);
  }

  get topItems() {
    const topItemsLeft = this.topItemsLeft;
    const topItemsRight = this.topItemsRight;

    if (_.isEmpty(topItemsLeft) && _.isEmpty(topItemsRight)) return null;

    return [...topItemsLeft, this.flexSeparator, ...topItemsRight];
  }

  get bottomItems() {
    const pagination = this.bottomPagination;
    const { footerControls = [] } = this.props;

    return [pagination, this.flexSeparator, this.topPagination, ...footerControls];
  }

  @autobind
  closeModal() {
    this.setState({ showExportModal: false });
  }

  @autobind
  onExportConfirm(fields, entity, identifier, description) {
    const { bulkExportAction } = this.props;
    this.closeModal();
    bulkExportAction(fields, entity, identifier, description);
  }

  get bulkExportModal() {
    if (!this.state.showExportModal) return null;

    const { exportFields, exportEntity, exportTitle } = this.props;

    const modal = renderExportModal(exportFields, exportEntity, exportTitle, this.onExportConfirm, null);

    return React.cloneElement(modal, {
      onCancel: this.closeModal,
      isVisible: true,
      entity: exportEntity === 'skus' ? exportTitle : exportTitle.toLowerCase(),
      inBulk: true,
    });
  }

  render() {
    const TableComponent = this.props.dataTable ? DataTable : Table;

    return (
      <div className="fc-tableview">
        {this.bulkExportModal}
        {this.getRow('header', this.topItems)}
        <div className="fc-table__table">
          <TableComponent {...this.props} setState={this.stateFromProps} />
        </div>
        {this.getRow('footer', this.bottomItems)}
      </div>
    );
  }
}

TableView.propTypes = {
  columns: PropTypes.array.isRequired,
  dataTable: PropTypes.bool,
  data: PropTypes.shape({
    rows: PropTypes.array,
    total: PropTypes.number,
    from: PropTypes.number,
    size: PropTypes.number,
  }),
  setState: PropTypes.func,
  renderRow: PropTypes.func,
  processRows: PropTypes.func,
  selectableColumns: PropTypes.array,
  setColumnSelected: PropTypes.func,
  tableIdentifier: PropTypes.string,
  detectNewRows: PropTypes.bool,
  paginator: PropTypes.bool,
  bulkActions: PropTypes.arrayOf(PropTypes.array),
  allChecked: PropTypes.bool,
  toggledIds: PropTypes.array,
  isLoading: PropTypes.bool,
  failed: PropTypes.bool,
  emptyMessage: PropTypes.string,
  errorMessage: PropTypes.string,
  className: PropTypes.string,
  renderHeadIfEmpty: PropTypes.bool,
  headerControls: PropTypes.array,
  footerControls: PropTypes.array,
};

TableView.defaultProps = {
  paginator: true,
  bulkActions: [],
  selectableColumns: [],
  dataTable: false,
  data: {
    rows: [],
    total: 0,
  },
};

export default TableView;
