/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import { ModalContainer } from 'components/modal/base';
import Modal from 'components/core/modal';
import ContentBox from 'components/content-box/content-box';
import ProductsAdd from './products-add';

// styles
import styles from './products-add.css';

type Props = {
  isVisible: boolean,
  onCancel: () => void,
  onConfirm: (users: Array<TUser>) => void,
  onAddProduct: (product: Product) => Promise<*>,
  addState: AsyncState,
  addedProducts: Array<Product>,
  title: string | Element<*>,
}

class ProductsAddModal extends Component {
  props: Props;

  static defaultProps = {
    title: 'Add Product',
  };

  render() {
    const { isVisible, title, addedProducts, addState, onAddProduct, onCancel } = this.props;

    return (
      <Modal className={styles.modal} title={title} isVisible={isVisible} onCancel={onCancel}>
        <ProductsAdd
          addedProducts={addedProducts}
          addState={addState}
          onAddProduct={onAddProduct}
        />
      </Modal>
    );
  }
}

export default ProductsAddModal;
