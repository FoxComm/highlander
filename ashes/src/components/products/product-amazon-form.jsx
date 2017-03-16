/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import s from './product-amazon.css';
import Form from '../forms/form';
import WaitAnimation from '../common/wait-animation';
import ObjectFormInner from '../object-form/object-form-inner';

type State = {
  attrs: ?Object,
};

export default class ProductAmazonForm extends Component {
  state: State = {
    attrs: this.props.product.attributes,
  };

  render() {
    const { schema, categoryId } = this.props;
    const { attrs } = this.state;

    console.log('attrs', attrs);

    if (!schema) {
      return null;
    }

    const p = schema && schema.properties.attributes.properties;
    // @todo should we use the schema as layout?
    const pKeys = _.keys(p); // All Amazon-specific field names, e.g. ['title', 'description' ...]
    const amazonVoidAttrs = _.mapValues(p, attr => ({
      t: 'string',
      v: '',
    }));
    amazonVoidAttrs.category_id = {
      t: 'string',
      v: categoryId,
    };
    const amazonExistedAttrs = _.pick(attrs, pKeys);
    const amazonAllAttrs = { ...amazonVoidAttrs, ...amazonExistedAttrs };

    return (
      <Form className={s.form} onSubmit={() => this._handleSubmit()}>
        <ObjectFormInner
          onChange={(e) => this._handleChange(e)}
          attributes={amazonAllAttrs}
          schema={schema.properties.attributes}
        />
        <button type="submit">Submit</button>
      </Form>
    );
  }

  _handleChange(nextAttributes) {
    this.setState({
      attrs: {
        ...this.state.attrs,
        ...nextAttributes,
      },
    });
  }

  _handleSubmit() {
    const { product, onSubmit } = this.props;
    const newProduct = {
      ...product,
      attributes: {
        ...product.attributes,
        ...this.state.attrs,
      },
    };

    onSubmit(newProduct);
  }
}
