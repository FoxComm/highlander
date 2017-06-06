/* @flow */

// libs
import React, { Component, Element } from 'react';

// components
import { ModalContainer } from 'components/modal/base';
import ContentBox from 'components/content-box/content-box';
import ProductsAdd from './products-add';
import Icon from 'components/core/icon';

// styles
import styles from './products-add.css';

type Props = {
  isVisible: boolean,
  onCancel: () => void,
  onConfirm: (users: Array<TUser>) => void,
  onAddProduct: (product: Product) => Promise<*>,
  addState: AsyncState,
  addedProducts: Array<Product>,
  title: string|Element<*>,
}

class ProductsAddModal extends Component {
  props: Props;

  static defaultProps = {
    title: 'Add Product',
  };

  get actionBlock() {
    return (
      <a className="fc-modal-close" onClick={this.props.onCancel}>
        <Icon name="close" />
      </a>
    );
  }

  render() {
    const { isVisible, title, addedProducts, addState, onAddProduct, onCancel } = this.props;

    return (
      <ModalContainer isVisible={isVisible} onCancel={onCancel}>
        <ContentBox
          className={styles.modal}
          title={title}
          actionBlock={this.actionBlock}
        >
          <div className="fc-modal-body">
            <ProductsAdd
              addedProducts={addedProducts}
              addState={addState}
              onAddProduct={onAddProduct}
            />
          </div>
        </ContentBox>
      </ModalContainer>
    );
  }
}

export default ProductsAddModal;
