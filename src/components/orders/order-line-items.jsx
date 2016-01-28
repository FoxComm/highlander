import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import React, { PropTypes } from 'react';

import ConfirmationDialog from '../modal/confirmation-dialog';
import EditableContentBox from '../content-box/editable-content-box';
import OrderLineItem from './order-line-item';
import PanelHeader from './panel-header';
import SkuResult from './sku-result';
import TableView from '../table/tableview';
import Typeahead from '../typeahead/typeahead';

const viewModeColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'quantity', text: 'Qty'},
  {field: 'totalPrice', text: 'Total', type: 'currency'}
];

const editModeColumns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
  {field: 'totalPrice', text: 'Total', type: 'currency'},
  {field: 'delete', text: '', component: 'DeleteLineItem'}
];

const OrderLineItems = props => {
  const title = <PanelHeader isCart={props.isCart} status={props.status} text="Items" />;
  const viewContent = (
    <TableView columns={viewModeColumns} data={{rows: props.lineItems.items}} />
  );

  return (
    <EditableContentBox
      className='fc-line-items'
      title={title}
      isEditing={props.lineItems.isEditing}
      editAction={props.orderLineItemsStartEdit}
      doneAction={props.orderLineItemsCancelEdit}
      editContent={<RenderEditContent {...props} />}
      editFooter={<RenderEditFooter {...props} />}
      viewContent={viewContent} />
  );
};

OrderLineItems.propTypes = {
  isCart: PropTypes.bool,
  order: PropTypes.object,
  lineItems: PropTypes.object,
  orderLineItemsStartEdit: PropTypes.func,
  orderLineItemsCancelEdit: PropTypes.func,
  status: PropTypes.string
};

OrderLineItems.defaultProps = {
  isCart: false,
  status: ''
};

class RenderEditContent extends React.Component {

  static propTypes = {
    orderLineItemsCancelDelete: PropTypes.func,
    deleteLineItem: PropTypes.func
  };

  render() {
    const props = this.props;
    const order = props.order.currentOrder;
    const lineItemsStatus = props.lineItems;

    const renderRow = (lineItem) => {
      return <OrderLineItem key={`lineItem-${lineItem.sku}`} item={lineItem} {...props} />;
    };

    return (
      <div>
        <TableView columns={ editModeColumns }
                   data={{rows: lineItemsStatus.items}}
                   renderRow={ renderRow } />
        <ConfirmationDialog
          isVisible={lineItemsStatus.isDeleting}
          header='Confirm'
          body='Are you sure you want to delete this item?'
          cancel='Cancel'
          confirm='Yes, Delete'
          cancelAction={() => props.orderLineItemsCancelDelete(lineItemsStatus.skuToDelete)}
          confirmAction={() => props.deleteLineItem(order, lineItemsStatus.skuToDelete)} />
      </div>
    );
  }
};

class RenderEditFooter extends React.Component {

  static propTypes = {
    updateLineItemCount: PropTypes.func,
    order: PropTypes.object,
    lineItems: PropTypes.object,
    skuSearch: PropTypes.object
  };

  componentDidMount() {
  }

  get suggestedSkus() {
    return _.get(this.props, 'skuSearch.result.rows', []);
  }

  get isFetching() {
    return _.get(this.props, 'skuSearch.isFetching', false);
  }

  get orderSkus() {
    return _.get(this.props, 'lineItems.items', []);
  }

  @autobind
  currentQuantityForSku(sku) { 
    let skus = this.orderSkus;
    let matched = skus.find((o) => { return o.sku === sku;});
    return _.isEmpty(matched) ? 0 : matched.quantity;
  }

  @autobind
  skuSelected(item) {
    const order = this.props.order.currentOrder;
    const newQuantity = this.currentQuantityForSku(item.sku) + 1;
    this.props.updateLineItemCount(order, item.sku, newQuantity);
    this.props.clearSkuSearch();
  }

  get query() { 
    return _.get(this.props, 'skuSearch.phrase', "");
  }

  render() {
    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead onItemSelected={this.skuSelected}
                   component={SkuResult}
                   isFetching={this.isFetching}
                   fetchItems={this.props.suggestSkus}
                   items={this.suggestedSkus}
                   query={this.query}
                   placeholder="Product name or SKU..."/>
      </div>
    );
  }
};

export default OrderLineItems;
