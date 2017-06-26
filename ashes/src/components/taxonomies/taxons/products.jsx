/* @flow */

// libs
import _ from 'lodash';
import { autobind } from 'core-decorators';
import React, { Component } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
import { filterArchived } from 'elastic/archive';
import * as dsl from 'elastic/dsl';
import { bulkExportBulkAction, renderExportModal } from 'modules/bulk-export/helpers';

// actions
import { actions } from 'modules/taxons/details/products-list';
import { addProduct, deleteProduct as unlinkProduct } from 'modules/taxons/details/taxon';
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

import type { TaxonomyParams } from '../taxonomy';

import styles from './taxons.css';

type Props = ObjectPageChildProps<Taxon> & {
  taxonomy: Taxonomy,
  actions: Object,
  list: Object,
  addState: AsyncState,
  deleteState: AsyncState,
  params: TaxonomyParams & {
    taxonId: number,
  },
  bulkExportAction: (
    fields: Array<string>, entity: string, identifier: string, description: string
  ) => Promise<*>,
  bulkActions: {
    exportByIds: (
      ids: Array<number>, description: string, fields: Array<Object>, entity: string, identifier: string
    ) => void,
  },
};

type State = {
  modalVisible: boolean,
  deletedProductId: ?number,
}

export class TaxonProductsPage extends Component {
  props: Props;

  state: State = {
    modalVisible: false,
    deletedProductId: null,
  };

  componentDidMount() {
    this.props.actions.setExtraFilters([
      dsl.nestedTermFilter('taxonomies.taxonomy', _.get(this.props.taxonomy, 'attributes.name.v')),
      dsl.nestedTermFilter('taxonomies.taxons', _.get(this.props.object, 'attributes.name.v')),
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

  @autobind
  unlinkButton(children: any, row: Product) {
    const inProgress = this.props.deleteState.inProgress && this.state.deletedProductId === row.productId;

    return (
      <Button
        onClick={this.handleUnlinkProduct.bind(this, row)}
        isLoading={inProgress}
      >
        Unlink
      </Button>
    );
  }


  @autobind
  openModal() {
    this.setState({ modalVisible: true });
  }

  @autobind
  closeModal() {
    this.setState({ modalVisible: false });
  }

  @autobind
  addSearchFilters(filters: Array<SearchFilter>, initial: boolean = false) {
    return this.props.actions.addSearchFilters(filterArchived(filters), initial);
  }

  @autobind
  handleAddProduct(product: Product) {
    const { actions, params: { taxonId, context } } = this.props;

    actions.addProduct(product.productId, context, taxonId)
      .then(this.props.actions.fetch);
  }

  @autobind
  handleUnlinkProduct(product: Product, e: MouseEvent) {
    e.preventDefault();
    e.stopPropagation();

    const { actions, params: { taxonId, context } } = this.props;

    this.setState({ deletedProductId: product.productId }, () => {
      actions.unlinkProduct(product.productId, context, taxonId)
        .then(this.props.actions.fetch);

      return;
    });
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

  @autobind
  bulkExport(allChecked: boolean, toggledIds: Array<number>) {
    const { exportByIds } = this.props.bulkActions;
    const modalTitle = 'Products';
    const entity = 'products';

    return renderExportModal(this.tableColumns, entity, modalTitle, exportByIds, toggledIds);
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
    const { list, actions, addState } = this.props;
    const products = _.get(list, 'savedSearches[0].results.rows');

    const searchActions = {
      ...actions,
      addSearchFilters: this.addSearchFilters,
    };

    const TotalCounter = makeTotalCounter(state => _.get(state, 'taxons.details.products'), actions);

    return (
      <div className="fc-products-list">
        <SectionTitle
          title="Products"
          subtitle={<TotalCounter />}
          addTitle="Product"
          onAddClick={this.openModal}
        />
        <BulkMessages
          storePath="taxons.details.bulk"
          module="taxons.details"
          entity="product"
          renderDetail={this.renderBulkDetails}
          className={styles['bulk-message']}
        />
        <BulkActions
          module="taxons.details"
          entity="product"
          actions={this.bulkActions}
        >
          <SelectableSearchList
            exportEntity="products"
            exportTitle="Products"
            bulkExport
            bulkExportAction={this.props.bulkExportAction}
            entity="taxons.details.products"
            emptyMessage="No products found."
            tableClassName={styles.productsTable}
            list={list}
            renderRow={this.renderRow}
            tableColumns={this.tableColumns}
            searchOptions={{ singleSearch: true }}
            searchActions={searchActions}
            predicate={({ id }) => id}
          />
        </BulkActions>
        <ProductsAddModal
          isVisible={this.state.modalVisible}
          onCancel={this.closeModal}
          onConfirm={this.closeModal}
          onAddProduct={this.handleAddProduct}
          addState={addState}
          addedProducts={products}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    list: _.get(state, 'taxons.details.products', {}),
    addState: _.get(state.asyncActions, 'taxonAddProduct', {}),
    deleteState: _.get(state.asyncActions, 'taxonDeleteProduct', {}),
  };
};

const mapDispatchToProps = (dispatch) => {
  return {
    actions: {
      ...bindActionCreators(actions, dispatch),
      addProduct: bindActionCreators(addProduct, dispatch),
      unlinkProduct: bindActionCreators(unlinkProduct, dispatch),
    },
    bulkExportAction: bindActionCreators(bulkExport, dispatch),
    bulkActions: bindActionCreators(bulkActions, dispatch),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(TaxonProductsPage);
