
import React from 'react';

import { Link } from 'react-router';
import ProductImage from '../../image/image';

import styles from '../profile.css';

const renderActions = (isNew) => {
  let actionsContent;
  if (isNew) {
    actionsContent = (
      <div styleName="actions-block">
        <Link styleName="link" to={'/profile'}>ADD</Link>
        &nbsp;|&nbsp;
        <Link styleName="link" to={'/profile'}>IGNORE</Link>
      </div>
    );
  } else {
    actionsContent = (
      <div styleName="actions-block">
        <Link styleName="link" to={'/profile'}>EDIT</Link>
        &nbsp;|&nbsp;
        <Link styleName="link" to={'/profile'}>REMOVE</Link>
      </div>
    );
  }
  return actionsContent;
};

const ReviewRow = (props) => {
  return (
    <div styleName="line-item">
      <div styleName="content">
        <div styleName="product-image">
          <ProductImage src={props.imagePath} width={50} height={50} />
        </div>
        <div styleName="product-data">
          <div styleName="product-info">
            <div styleName="product-name">{props.name}</div>
            <div styleName="product-variant">{/* TODO: variant info must be here */}</div>
          </div>
        </div>
        {renderActions(true)}
      </div>
    </div>
  );
};

export default ReviewRow;
