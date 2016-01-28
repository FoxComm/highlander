import React, { PropTypes } from 'react';
import _ from 'lodash';
import ConfirmationDialog from '../modal/confirmation-dialog';
import OrderLineItem from './order-line-item';
import TableView from '../table/tableview';
import EditableContentBox from '../content-box/editable-content-box';
import Typeahead from '../typeahead/typeahead';
import SkuResult from './sku-result';
import PanelHeader from './panel-header';

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
    fetchSkus: PropTypes.func,
    skusActions: PropTypes.shape({
      skus: PropTypes.array
    })
  };

  componentDidMount() {
     this.props.fetchSkus();
  }

  get skus() {
    return _.get(this.props, 'skusActions.skus', []);
  }

  render() {
    return (
      <div className="fc-line-items-add">
        <div className="fc-line-items-add-label">
          <strong>Add Item</strong>
        </div>
        <Typeahead onItemSelected={null}
                   component={SkuResult}
                   fetchItems={null}
                   items={this.skus}
                   placeholder="Product name or SKU..."/>
      </div>
    );
  }
};

export default OrderLineItems;
