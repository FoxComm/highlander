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
import { linkProducts, unlinkProduct } from 'modules/catalog/details';
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
import Content from 'components/core/content/content';

// styles
import styles from './products.css';

type Props = {
  params: {
    catalogId: number,
  },
  actions: {
    addSearchFilters: Function,
    fetch: Function,
    linkProducts: Function,
    unlinkProduct: Function,
    setExtraFilters: Function,
  },
  list: ?Object,
  linkState: Object,
  unlinkState: Object,
}

type State = {
  modalVisible: boolean,
  deletedProductId: ?number,
};

class CatalogProducts extends Component {
  props: Props;

  state: State = {
    modalVisible: false,
    deletedProductId: null,
  };

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

  unlinkButton = (children: any, row: Product) => {
    const inProgress = this.props.unlinkState.inProgress
      && this.state.deletedProductId === row.productId;

    return (
      <Button
        onClick={this.handleUnlinkProduct.bind(this, row)}
        isLoading={inProgress}
      >
        Unlink
      </Button>
    );
  };

  openModal = () => this.setState({ modalVisible: true });
  closeModal = () => this.setState({ modalVisible: false });

  handleAddProduct = (product: Product) => {
    const { actions, params: { catalogId } } = this.props;
    actions.linkProducts(catalogId, { productIds: [product.productId] }).
      then(this.props.actions.fetch);
  };

  handleUnlinkProduct = (product: Product, e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();

    const { actions, params: { catalogId } } = this.props;
    const { productId } = product;

    this.setState({ deletedProductId: productId }, () => {
      actions.unlinkProduct(catalogId, productId).then(this.props.actions.fetch);
    });
  };

  renderRow(row: Product, index: number, columns: Columns, params: Object) {
    const id = row.productId != null ? row.productId : 0;
    const key = `catalog-product-${id}`;

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
    const { list, actions, linkState } = this.props;
    const products = _.get(list, ['savedSearches', 0, 'results', 'rows']);

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    const TotalCounter = makeTotalCounter(state => _.get(state, 'catalogs.products'), actions);

    return (
      <div styleName="list-container" className="fc-products-list">
        <SectionTitle
          title="Products"
          subtitle={<TotalCounter />}
          addTitle="Product"
          onAddClick={this.openModal}
        />
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
        <ProductsAddModal
          isVisible={this.state.modalVisible}
          onCancel={this.closeModal}
          onConfirm={this.closeModal}
          onAddProduct={this.handleAddProduct}
          addState={linkState}
          addedProducts={products}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state, 'catalogs.products', {}),
    linkState: _.get(state.asyncActions, 'catalogLinkProducts', {}),
    unlinkState: _.get(state.asyncActions, 'catalogUnlinkProduct', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: {
      ...bindActionCreators(actions, dispatch),
      linkProducts: bindActionCreators(linkProducts, dispatch),
      unlinkProduct: bindActionCreators(unlinkProduct, dispatch),
    },
    bulkActionExport: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(CatalogProducts);
