/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import Modal from 'components/core/modal';
import ProductsAdd from './products-add';

type Props = {
  isVisible: boolean,
  onCancel: () => void,
  onConfirm: (users: Array<TUser>) => void,
  onAddProduct: (product: Product) => Promise<*>,
  addState: AsyncState,
  addedProducts: Array<Product>,
  title: string | Element<*>,
};

class ProductsAddModal extends Component {
  props: Props;

  static defaultProps = {
    title: 'Add Product',
  };

  render() {
    const { isVisible, title, addedProducts, addState, onAddProduct, onCancel } = this.props;

    return (
      <Modal title={title} isVisible={isVisible} onClose={onCancel}>
        <ProductsAdd addedProducts={addedProducts} addState={addState} onAddProduct={onAddProduct} />
      </Modal>
    );
  }
}

export default ProductsAddModal;
