/* @flow */

// libs
import _ from 'lodash';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';
import * as dsl from 'elastic/dsl';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/catalog/products-list';
/* import { addProduct, deleteProduct as unlinkProduct } from 'modules/taxons/details/taxon';*/
import { bulkExport } from 'modules/bulk-export/bulk-export';
import { actions as bulkActions } from 'modules/taxons/details/bulk';

// components
import { SectionTitle } from 'components/section-title';
import SelectableSearchList from 'components/list-page/selectable-search-list';
import ProductRow from 'components/products/product-row';
import { makeTotalCounter } from 'components/list-page';
import { ProductsAddModal } from 'components/products-add';
import { Button } from 'components/core/button';
import BulkActions from 'components/bulk-actions/bulk-actions';
import BulkMessages from 'components/bulk-actions/bulk-messages';
import { Link } from 'components/link';

type Props = {
  params: {
    catalogId: number,
  },
}

class CatalogProducts extends Component {
  props: Props;

  componentDidMount() {
    const { catalogId } = this.props.params;
    
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('catalogs.id', catalogId),
    ]);
    
    this.props.actions.fetch();
  }
  
  get tableColumns(): Columns {
    return [
      { field: 'productId', text: 'ID' },
      { field: 'image', text: 'Image', type: 'image' },
      { field: 'title', text: 'Name' },
      { field: 'skus', text: 'SKUs' },
      { field: 'state', text: 'State' },
      { field: '', render: this.unlinkButton },
    ];
  }

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    const id = row.productId != null ? row.productId : 0;
    const key = `taxon-product-${id}`;

    return (
      <ProductRow
        key={key}
        product={row}
        columns={columns}
        params={params}
      />
    );
  }

  addSearchFilters = (filters: Array<SearchFilter>, initial: boolean = false) => {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial)
  };

  render() {
    const { list } = this.props;

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    return (
      <SelectableSearchList
        exportEntity="products"
        exportTitle="Products"
        entity="catalogs.products"
        emptyMessage="No products found."
        list={list}
        renderRow={this.renderRow}
        tableColumns={this.tableColumns}
        searchOptions={{ singleSearch: true }}
        searchActions={searchActions}
        predicate={({ id }) => id}
      />
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state, 'catalogs.products', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: {
      ...bindActionCreators(actions, dispatch),
    },
    bulkActionExport: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(CatalogProducts);
