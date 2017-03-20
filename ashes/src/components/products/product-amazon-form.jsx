/**
 * @flow weak
 */

// libs
import React, { Component, Element, PropTypes } from 'react';
import classNames from 'classnames';
import _ from 'lodash';
import { assoc } from 'sprout-data';
import s from './product-amazon.css';
import Form from '../forms/form';
import WaitAnimation from '../common/wait-animation';
import ObjectFormInner from '../object-form/object-form-inner';
import ContentBox from '../content-box/content-box';
import MultiSelectTable from 'components/table/multi-select-table';
import EditableSkuRow from './skus/editable-sku-row';
import { setSkuAttribute } from 'paragons/product';
import { PrimaryButton } from '../common/buttons';

type State = {
  product: Object,
};

export default class ProductAmazonForm extends Component {
  state: State = {
    product: this.props.product,
  };

  renderRow(row, index, columns, params) {
    const key = row.feCode || row.code || row.id;

    return (
      <EditableSkuRow
        columns={columns}
        sku={row}
        index={index}
        params={params}
        key={key}
        updateFields={(id, next) => this._handleSetSkuProperties(id, next)}
      />
    );
  }

  renderVariants() {
    const { product } = this.state;
    const { skus } = product;

    if (!skus.length) {
      return null;
    }

    const arr = [
      { field: 'sku', text: 'SKU' },
      { field: 'image', text: 'Image' },
      { field: 'inventory', text: 'Inventory' },
      { field: 'upc', text: 'UPC' },
      { field: 'asin', text: 'ASIN' },
    ];

    const onCheck = function(skuIds) {
      const { product } = this.state;
      let nextProduct = product;

      _.each(product.skus, (sku, i) => {
        const checked = _.includes(skuIds, sku.id);

        nextProduct = assoc(nextProduct, ['skus', i, 'attributes', 'amazon'], { t: 'bool', v: checked });
      });

      this.setState({ product: nextProduct });
    };

    return (
      <MultiSelectTable
        className={s.table}
        tbodyId="fct-sku-list"
        columns={arr}
        dataTable={false}
        data={{ rows: skus }}
        renderRow={this.renderRow.bind(this)}
        emptyMessage="emptyMessage"
        hasActionsColumn={false}
        onCheck={onCheck.bind(this)}
      />
    );
  }

  render() {
    const { schema, categoryId, categoryPath } = this.props;
    const { product: { attributes } } = this.state;

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
    amazonVoidAttrs.nodeId.v = categoryId;
    amazonVoidAttrs.nodePath.v = categoryPath;
    const amazonExistedAttrs = _.pick(attributes, pKeys);
    const amazonAllAttrs = { ...amazonVoidAttrs, ...amazonExistedAttrs };

    return (
      <Form className={s.form} onSubmit={() => this._handleSubmit()}>
        <ContentBox title="Product fields" className={s.box}>
          <ObjectFormInner
            onChange={(e) => this._handleChange(e)}
            attributes={amazonAllAttrs}
            schema={schema.properties.attributes}
          />
        </ContentBox>

        <ContentBox title="SKUs fields" className={s.box}>
          {this.renderVariants()}
        </ContentBox>

        <PrimaryButton type="submit">Save</PrimaryButton>
      </Form>
    );
  }

  _handleSetSkuProperties(code: string, updateArray: Array<Array<any>>) {
    const { product } = this.state;

    if (product) {
      const newProduct = _.reduce(updateArray, (p, [field, value]) => {
        return setSkuAttribute(p, code, field, value, 'string');
      }, product);
      this.setState({product: newProduct});
    }
  }

  _handleChange(nextAttributes) {
    const { product } = this.state;

    this.setState({
      product: {
        ...this.state.product,
        attributes: {
          ...this.state.product.attributes,
          ...nextAttributes,
        },
      },
    });
  }

  _handleSubmit() {
    const { product } = this.state;
    const { onSubmit } = this.props;

    onSubmit(product);
  }
}
