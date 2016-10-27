/* @flow */

// libs
import React from 'react';
import cx from 'classnames';

// styles
import styles from './add-to-cart-btn.css';

type Props = {
  expanded?: bool,
  onClick: Function,
};

const AddToCartBtn = (props: Props) => {
  const { expanded = false, onClick, ...restProps } = props;
  const cls = cx(styles['add-to-cart-btn'], {
    [styles.expanded]: expanded,
  });

  return (
    <button className={cls} onClick={onClick} {...restProps}>
      <span styleName="add-icon">+</span>
      <span styleName="add-btn-title">ADD TO CART</span>
    </button>
  );
};

export default AddToCartBtn;
