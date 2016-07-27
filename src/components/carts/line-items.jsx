import _ from 'lodash';
import { autobind } from 'core-decorators';
import { connect } from 'react-redux';
import React, { PropTypes } from 'react';
import { trackEvent } from 'lib/analytics';

import * as lineItemActions from 'modules/carts/line-items';

import ConfirmationDialog from 'components/modal/confirmation-dialog';
import EditableContentBox from 'components/content-box/editable-content-box';
import ContentBox from 'components/content-box/content-box';
import CartLineItem from './line-item';
import CartLineItemsFooter from './line-items-footer';
import PanelHeader from 'components/panel-header/panel-header';
import SkuLineItems from 'components/sku-line-items/sku-line-items';
import SkuResult from './sku-result';
import TableView from 'components/table/tableview';
import Typeahead from 'components/typeahead/typeahead';

const columns = [
  {field: 'imagePath', text: 'Image', type: 'image'},
  {field: 'name', text: 'Name'},
  {field: 'sku', text: 'SKU'},
  {field: 'price', text: 'Price', type: 'currency'},
  {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
  {field: 'totalPrice', text: 'Total', type: 'currency'},
  {field: 'delete', text: '', component: 'DeleteLineItem'}
];

const mapStateToProps = state => {
  return {
    lineItems: state.carts.lineItems,
  };
};

const mapDispatchToProps = {
  ...lineItemActions,
};

export class CartLineItems extends React.Component {
  static propTypes = {
    cart: PropTypes.object,
    lineItems: PropTypes.object,
    orderLineItemsStartEdit: PropTypes.func,
    orderLineItemsCancelEdit: PropTypes.func,
    status: PropTypes.string,
    readOnly: PropTypes.bool,
  };

  static defaultProps = {
    status: '',
    readOnly: false,
  };

  get editContent(): Element {
    const { cart, lineItems } = this.props;
    const skus = _.get(cart, 'lineItems.skus', []);

    const renderRow = (item: SkuItem) => {
      const key = `sku-line-item-${item.sku}`;
      return (
        <CartLineItem
          key={key}
          item={item}
          cart={cart}
          onStartDelete={this.props.orderLineItemsStartDelete}
          onUpdateCount={this.props.updateLineItemCount} />
      );
    };

    return (
      <div>
        <SkuLineItems items={skus} columns={columns} renderRow={renderRow} />
        <ConfirmationDialog
          isVisible={lineItems.isDeleting}
          header="Confirm"
          body="Are you sure you want to delete this item?"
          cancel="Cancel"
          confirm="Yes, Delete"
          cancelAction={() => this.props.orderLineItemsCancelDelete(lineItems.skuToDelete)}
          confirmAction={() => this.props.deleteLineItem(cart, lineItems.skuToDelete)} />
      </div>
    );
  }

  get editFooter(): Element {
    const { cart, updateLineItemCount } = this.props;

    return (
      <CartLineItemsFooter
        cart={cart}
        updateCount={updateLineItemCount} />
    );
  }

  get viewContent(): Element {
    const skus = _.get(this.props, 'cart.lineItems.skus', []);
    return <SkuLineItems items={skus} />;
  }

  render() {
    const props = this.props;

    const title = (
      <PanelHeader
        showStatus={true}
        status={props.status}
        text="Items" />
    );

    const isCheckingOut = _.get(props, 'cart.isCheckingOut', false);
    const editAction = isCheckingOut ? null : () => {
      trackEvent('Orders', 'edit_line_items');
      props.orderLineItemsStartEdit();
    };

    const doneAction = () => {
      trackEvent('Orders', 'edit_line_items_done');
      props.orderLineItemsCancelEdit();
    };

    return (
      <EditableContentBox
        className='fc-line-items'
        title={title}
        isEditing={props.lineItems.isEditing}
        editAction={editAction}
        doneAction={doneAction}
        editContent={this.editContent}
        editFooter={this.editFooter}
        indentContent={false}
        viewContent={this.viewContent} />
    );
  }
}


export default connect(mapStateToProps, mapDispatchToProps)(CartLineItems);
