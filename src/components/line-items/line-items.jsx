import React, { PropTypes } from 'react';
import TableView from '../table/tableview';
import LineItemCounter from './line-item-counter';
import { autobind } from 'core-decorators';
import LineItemActions from '../../actions/line-items';
import DeleteLineItem from './line-item-delete';
import SkuStore from '../../stores/skus';
import SkuResult from '../orders/sku-result';
import Typeahead from '../typeahead/typeahead';
import ContentBox from '../content-box/content-box';
import {EditButton} from '../common/buttons';

export default class LineItems extends React.Component {
  static propTypes = {
    entity: PropTypes.object,
    tableColumns: PropTypes.array,
    suggestedSkus: PropTypes.array,
    model: PropTypes.string
  };

  static defaultProps = {
    suggestedSkus: []
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      isEditing: false
    };
  }

  get orderDefaultColumns () {
    return [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'quantity', text: 'Qty'},
      {field: 'total', text: 'Total', type: 'currency'}
    ];
  }

  get orderEditColumns () {
    return [
      {field: 'imagePath', text: 'Image', type: 'image'},
      {field: 'name', text: 'Name'},
      {field: 'sku', text: 'SKU'},
      {field: 'price', text: 'Price', type: 'currency'},
      {field: 'lineItem', text: 'Qty', component: 'LineItemCounter'},
      {field: 'total', text: 'Total', type: 'currency'},
      {field: 'delete', text: '', component: 'DeleteLineItem'}
    ];
  }

  toggleEdit() {
    this.setState({
      isEditing: !this.state.isEditing
    });
  }

  @autobind
  itemSelected(sku) {
    LineItemActions.editLineItems(
      this.props.model,
      this.props.entity.referenceNumber,
      [{'sku': sku.sku, 'quantity': 1}]
    );
  }

  render() {
    let actions = null;
    let controls = null;
    let columns = this.props.tableColumns;
    let rows = this.props.entity.lineItems.skus;
    let headerActions = null;
    let body = <TableView columns={columns} data={{rows: rows}} />;

    if (this.props.model === 'order') {
      if (this.state.isEditing) {
        columns = this.orderEditColumns;
        body = (
          <TableView columns={columns} rows={rows} model="lineItem">
            <LineItemCounter entityName={this.props.model} entity={this.props.entity} />
            <DeleteLineItem entityName={this.props.model} entity={this.props.entity} />
          </TableView>
        );
        actions = (
          <footer className="fc-line-items-footer">
            <div>
              <div className="fc-line-items-add-label">
                <strong>Add Item</strong>
              </div>
              <Typeahead onItemSelected={this.itemSelected}
                         component={SkuResult}
                         items={this.props.suggestedSkus}
                         placeholder="Product name or SKU..." />
            </div>
            <div className="fc-line-items-footer-editing-done">
              <button className="fc-btn fc-btn-primary"
                      onClick={ this.toggleEdit.bind(this) } >Done</button>
            </div>
          </footer>
        );
      } else {
        columns = this.orderDefaultColumns;
        headerActions = (
          <EditButton onClick={this.toggleEdit.bind(this)} />
        );
        body = (
            <TableView
              columns={columns}
              rows={rows}
              model={this.props.model}
              />
        );
        controls = (
          <div>
            <button className="fc-right fc-btn icon-edit" onClick={this.toggleEdit.bind(this)}>
            </button>
          </div>
        );
      }
    }

    return (
      <ContentBox
        title="Items"
        className="fc-line-items"
        actionBlock={headerActions}
        indentContent={false}>
        {body}
        {actions}
      </ContentBox>
    );
  }
}
