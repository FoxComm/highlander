/* @flow */

import React, { Component, Element } from 'react';

// libs
import _ from 'lodash';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { autobind } from 'core-decorators';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';
import { filterArchived } from 'elastic/archive';

// actions
import { actions } from 'modules/products/list';
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/products/bulk';

// components
import SelectableSearchList from '../list-page/selectable-search-list';
import ProductRow from './product-row';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  actions: Object,
  list: Object,
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

const tableColumns: Columns = [
  { field: 'productId', text: 'Product ID' },
  { field: 'image', text: 'Image', type: 'image' },
  { field: 'title', text: 'Name' },
  { field: 'state', text: 'State' },
];

export class Products extends Component {
  props: Props;

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    const key = `products-${_.get(row, 'id', index)}`;
    return (
      <ProductRow
        key={key}
        product={row}
        columns={columns}
        params={params}
      />
    );
  }

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Products';
    const entity = 'products';

    return renderExportModal(tableColumns, entity, modalTitle, exportByIds, toggledIds);
  }

  get bulkActions(): Array<any> {
    return [
      bulkExportBulkAction(this.bulkExport, 'Products'),
    ];
  }

  @autobind
  renderBulkDetails(context: string, id: number) {
    const { list } = this.props;
    const results = list.currentSearch().results.rows;
    const filteredProduct = _.filter(results, (product) => product.id.toString() === id)[0];
    const productId = filteredProduct.productId;

    return (
      <span key={id}>
        Product <Link to="product-details" params={{ productId, context }}>{productId}</Link>
      </span>
    );
  }

  render() {
    const { list, actions } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <div className="fc-products-list">
        <BulkMessages
          storePath="products.bulk"
          module="products"
          entity="product"
          renderDetail={this.renderBulkDetails}
        />
        <BulkActions
          module="products"
          entity="product"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="products"
            exportTitle="Products"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="products.list"
            emptyMessage="No products found."
            list={list}
            renderRow={this.renderRow}
            tableColumns={tableColumns}
            searchActions={searchActions}
            predicate={({id}) => id}
          />
        </BulkActions>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state.products, 'list', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: bindActionCreators(actions, dispatch),
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Products);
